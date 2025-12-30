package com.docusign.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docusign.constants.AppConstants;
import com.docusign.dto.UserEmailContext;
import com.docusign.entity.Designer;
import com.docusign.entity.DocumentCompletion;
import com.docusign.exception.ResourceNotFoundException;
import com.docusign.repository.DesignerRepo;
import com.docusign.repository.DocumentCompletionRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentCompletionServiceImpl implements DocumentCompletionService {

    private final DocumentCompletionRepo completionRepo;
    private final DesignerRepo designerRepo;
    private final EmailService emailService;
    private final S3Service s3Service;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // -------------------------------------------------------------------------
    // Send completion email
    // -------------------------------------------------------------------------
    @Override
    public void sendCompletionEmails(Designer designer, List<Map<String, Object>> users) {

        for (Map<String, Object> user : users) {
            UserEmailContext context = buildUserContext(user);

            validateEmail(context);

            DocumentCompletion completion =
                    createCompletionRecord(designer, context);

            completionRepo.save(completion);
            log.info("Completion record saved in db: {}", completion);

            String completionLink = buildCompletionLink(completion);

            sendCompletionEmailSafely(designer, context, completionLink);
        }
    }
    
    private UserEmailContext buildUserContext(Map<String, Object> user) {

        String userId = (String) user.get("userId");
        String email = (String) user.get("email");
        String firstName = (String) user.get("firstName");
        String lastName = (String) user.get("lastName");
        boolean isExternal = (boolean) user.get("isExternal");

        String userName =
                (firstName != null ? firstName : "") + " " +
                (lastName != null ? lastName : "");

        return new UserEmailContext(userId, email, userName.trim(), isExternal);
    }
    
    private void validateEmail(UserEmailContext context) {

        if (context.email() == null || context.email().isBlank()) {
            throw new ResourceNotFoundException(
                "User " +
                (context.userName().isEmpty() ? context.userId() : context.userName()) +
                " does not have an email address"
            );
        }
    }

    
    private DocumentCompletion createCompletionRecord(
            Designer designer,
            UserEmailContext context) {

        DocumentCompletion completion = new DocumentCompletion();

        completion.setDesignerId(designer.getId());
        completion.setUserId(context.isExternal() ? context.email() : context.userId());
        completion.setExternal(context.isExternal());
        completion.setToken(UUID.randomUUID().toString());
        completion.setStatus(AppConstants.STATUS_PENDING);
        completion.setCreatedAt(Instant.now());

        return completion;
    }
    
    private String buildCompletionLink(DocumentCompletion completion) {
        return frontendUrl + "/documents/complete/" +
               completion.getToken() +
               "?isExternal=" + completion.isExternal();
    }

    private void sendCompletionEmailSafely(
            Designer designer,
            UserEmailContext context,
            String completionLink) {

        try {
            emailService.sendDocumentCompletionEmail(
                context.email(),
                context.isExternal() ? "User" : context.userName(),
                designer.getTitle(),
                completionLink
            );
        } catch (Exception e) {
            log.error("Failed to send completion email to {}", context.email(), e);
            throw new ResourceNotFoundException(
                "Failed to send email to " + context.email() + " "+ e
            );
        }
    }





    // -------------------------------------------------------------------------
    // Save completed document (controller passes designerId only)
    // -------------------------------------------------------------------------
    @Override
    public String saveCompletedDocument(
            String designerId,
            String userId,
            Map<String, Object> fieldValues,
            String token
    ) {
        Designer designer = designerRepo.findById(designerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Designer not found")
                );

        return saveCompletedDocument(designer, userId, fieldValues, token);
    }

    // -------------------------------------------------------------------------
    // Core save logic
    // -------------------------------------------------------------------------
    private String saveCompletedDocument(
            Designer designer,
            String userId,
            Map<String, Object> fieldValues,
            String token
    ) {
        DocumentCompletion completion =
                completionRepo.findByTokenAndUserId(token, userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Invalid completion token")
                        );

        if (!completion.getDesignerId().equals(designer.getId())) {
            throw new ResourceNotFoundException("Token does not match designer");
        }

        String originalKey = designer.getS3Key();
        if (originalKey == null || originalKey.isEmpty()) {
            throw new ResourceNotFoundException("Designer document not found");
        }

        String filename =
                originalKey.substring(originalKey.lastIndexOf("/") + 1);

        String captureKey =
                String.format(
                        "capture-docs/%s/%s",
                        designer.getId(),
                        filename
                );

        try {
            s3Service.copyObject(originalKey, captureKey);
        } catch (Exception e) {
            log.error("Failed to copy document from {} to {}: ", originalKey, captureKey, e);
            throw new InternalError(
                    "Failed to copy document to capture-docs",
                    e
            );
        }

        completion.setFieldValues(fieldValues);
        completion.setCaptureKey(captureKey);
        completion.setStatus(AppConstants.STATUS_COMPLETED);
        completion.setCompletedAt(Instant.now());

        completionRepo.save(completion);

        checkAndNotifyCompletion(designer);

        return captureKey;
    }

    // -------------------------------------------------------------------------
    // Check if all recipients completed & notify
    // -------------------------------------------------------------------------
    private void checkAndNotifyCompletion(Designer designer) {

        List<Map<String, Object>> recipients = designer.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        long completedCount =
                completionRepo.countByDesignerIdAndStatus(
                        designer.getId(),
                        AppConstants.STATUS_COMPLETED
                );

        if (completedCount < recipients.size()) {
            return;
        }

        designer.setStatus(AppConstants.STATUS_COMPLETED);
        designerRepo.save(designer);

        String finalLink =
                frontendUrl + "/documents/final/" + designer.getId();

        for (Map<String, Object> recipient : recipients) {

            String email = (String) recipient.get("email");
            String name = (String) recipient.get("name");

            if (email != null) {
                try {
                    emailService.sendFinalDocumentEmail(
                            email,
                            name,
                            designer.getTitle(),
                            finalLink
                    );
                } catch (Exception e) {
                    log.error("Failed to send final email to {}: ", email, e);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fetch completion by token
    // -------------------------------------------------------------------------
    @Override
    public DocumentCompletion getCompletionByToken(String token) {

        if (token == null || token.trim().isEmpty()) {
            throw new ResourceNotFoundException("Token not found");
        }

        return completionRepo.findByToken(token)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invalid or expired completion token"
                        )
                );
    }

    // -------------------------------------------------------------------------
    // Get document + view URL for completion page
    // -------------------------------------------------------------------------
    @Override
    public Map<String, Object> getDocumentForCompletion(String token) {

        DocumentCompletion completion = getCompletionByToken(token);

        Designer designer = designerRepo.findById(completion.getDesignerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Designer not found for token")
                );

        if (designer.getS3Key() == null || designer.getS3Key().isEmpty()) {
            throw new ResourceNotFoundException(
                    "Document not found. The document may not have been uploaded."
            );
        }

        String viewUrl;
        try {
            viewUrl = s3Service.generatePresignedGetUrl(designer.getS3Key());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key {}: ", designer.getS3Key(), e);
            throw new InternalError(
                    "Failed to generate document URL",
                    e
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("designerId", designer.getId());
        response.put("title", designer.getTitle() != null
                ? designer.getTitle()
                : "Document");
        response.put("fields", designer.getFields() != null
                ? designer.getFields()
                : List.of());
        response.put("currentUserId", completion.getUserId());
        response.put("token", token);
        response.put("viewUrl", viewUrl);
        response.put("status", completion.getStatus());
        response.put("consolidatedData", getConsolidatedValues(designer.getId()));

        return response;
    }

    // -------------------------------------------------------------------------
    // Consolidated field values
    // -------------------------------------------------------------------------
    @Override
    public Map<String, Object> getConsolidatedValues(String designerId) {

        List<DocumentCompletion> completions =
                completionRepo.findByDesignerId(designerId);

        Map<String, Object> merged = new HashMap<>();

        for (DocumentCompletion dc : completions) {
            if (dc.getFieldValues() != null) {
                dc.getFieldValues().forEach((key, value) -> {
                    if (value != null &&
                        !(value instanceof String str && str.trim().isEmpty())) {
                        merged.put(key, value);
                    }
                });
            }
        }
        return merged;
    }

}
