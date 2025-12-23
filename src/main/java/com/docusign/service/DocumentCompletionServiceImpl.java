package com.docusign.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docusign.entity.Designer;
import com.docusign.entity.DocumentCompletion;
import com.docusign.repository.DesignerRepo;
import com.docusign.repository.DocumentCompletionRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentCompletionServiceImpl implements DocumentCompletionService {

    private final DocumentCompletionRepo completionRepo;
    private final DesignerRepo designerRepo;
    private final EmailService emailService;
    private final S3Service s3Service;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // -------------------------------------------------------------------------
    // Send completion emails
    // -------------------------------------------------------------------------
    @Override
    public void sendCompletionEmails(Designer designer, List<Map<String, Object>> users) {

        for (Map<String, Object> user : users) {

            String userId = (String) user.get("userId");
            String email = (String) user.get("email");
            String firstName = (String) user.get("firstName");
            String lastName = (String) user.get("lastName");

            String userName =
                    (firstName != null ? firstName : "") + " " +
                    (lastName != null ? lastName : "");

            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException(
                        "User " +
                        (userName.trim().isEmpty() ? userId : userName.trim()) +
                        " does not have an email address"
                );
            }

            String token = UUID.randomUUID().toString();

            DocumentCompletion completion = new DocumentCompletion();
            completion.setDesignerId(designer.getId());
            completion.setUserId((boolean) user.get("isExternal") ? email : userId);
            completion.setToken(token);
            completion.setStatus("pending");
            completion.setCreatedAt(Instant.now());

            completionRepo.save(completion);

            String completionLink =
                    frontendUrl + "/documents/complete/" + token+"?isExtrernal="+(boolean) user.get("isExternal");

            try {
                emailService.sendDocumentCompletionEmail(
                        email,
                        (boolean) user.get("isExternal") ? "User" : userName.trim(),
                        designer.getTitle(),
                        completionLink
                );
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to send email to " + email,
                        e
                );
            }
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
                        new RuntimeException("Designer not found")
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
                                new RuntimeException("Invalid completion token")
                        );

        if (!completion.getDesignerId().equals(designer.getId())) {
            throw new RuntimeException("Token does not match designer");
        }

        String originalKey = designer.getS3Key();
        if (originalKey == null || originalKey.isEmpty()) {
            throw new RuntimeException("Designer document not found");
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
            throw new RuntimeException(
                    "Failed to copy document to capture-docs",
                    e
            );
        }

        completion.setFieldValues(fieldValues);
        completion.setCaptureKey(captureKey);
        completion.setStatus("completed");
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
                        "completed"
                );

        if (completedCount < recipients.size()) {
            return;
        }

        designer.setStatus("completed");
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
                    System.err.println(
                            "Failed to send final email to " + email
                    );
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
            throw new RuntimeException("Token is required");
        }

        return completionRepo.findByToken(token)
                .orElseThrow(() ->
                        new RuntimeException(
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
                        new RuntimeException("Designer not found for token")
                );

        if (designer.getS3Key() == null || designer.getS3Key().isEmpty()) {
            throw new RuntimeException(
                    "Document not found. The document may not have been uploaded."
            );
        }

        String viewUrl;
        try {
            viewUrl = s3Service.generatePresignedGetUrl(designer.getS3Key());
        } catch (Exception e) {
            throw new RuntimeException(
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
                merged.putAll(dc.getFieldValues());
            }
        }
        return merged;
    }
}
