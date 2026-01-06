package com.docusign.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.docusign.constants.AppConstants;
import com.docusign.entity.Designer;
import com.docusign.exception.ResourceNotFoundException;
import com.docusign.repository.DesignerRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignerServiceImpl implements DesignerService {
	
	private final DesignerRepo designerRepo;
    private final S3Service s3Service;
    private final DocumentCompletionService documentCompletionService;
	
	private static final String MESSAGE = "Designer not found with id: ";
	private static final String DOCUMENT = "Document";
	
	@Override
	public List<Designer> findAll() {
	    return designerRepo.findByStatusNot(AppConstants.STATUS_COMPLETED);
	}


	@Override
	public List<Designer> findAllCompletedDocuments() {
	    return designerRepo.findByStatus(AppConstants.STATUS_COMPLETED);
	}


	@Override
	public Designer saveDesignerDocument(Map<String, String> body) {
        String originalTitle = body.getOrDefault("title", AppConstants.DEFAULT_TITLE);
        String uniqueTitle = generateUniqueTitle(originalTitle);

        Designer d = new Designer();
        d.setId(UUID.randomUUID().toString());
        d.setTitle(uniqueTitle);
        d.setStatus(AppConstants.STATUS_DRAFT);
        d.setType(DOCUMENT);
        return designerRepo.save(d);
	}

    private String generateUniqueTitle(String title) {
        if (!designerRepo.existsByTitle(title)) {
            return title;
        }

        String baseTitle = title;
        int counter = 1;

        if (title.matches(".*\\s\\(\\d+\\)$")) {
            baseTitle = title.substring(0, title.lastIndexOf(" ("));
        }

        String newTitle;
        do {
            newTitle = String.format("%s (%d)", baseTitle, counter++);
        } while (designerRepo.existsByTitle(newTitle));

        return newTitle;
    }


	@Override
	public Map<String, String> getUploadUrl(String id, Map<String, String> body) {
		String filename = body.get("fileName");
	    String key = String.format("designer-docs/%s/%s", id, filename);
	    String url = s3Service.generatePresignedPutUrl(key);
		return Map.of("url", url, "key", key);
	}


	@Override
	public Designer updateDesignerDocument(String id, Designer designer) {
		 Designer document = designerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(MESSAGE + id));
	        if (designer.getS3Key() != null) {
	        	document.setS3Key(designer.getS3Key());
	        }
	        if (designer.getPages() > 0) {
	        	document.setPages(designer.getPages());
	        }
	        if (!designer.getRecipients().isEmpty()) {
	        	document.setRecipients(designer.getRecipients());
	        }
	        if (!designer.getFields().isEmpty()) {
	        	document.setFields(designer.getFields());
	        }
	        if (designer.getStatus() != null) {
	        	document.setStatus(designer.getStatus());
	        }
	        if (designer.getType() != null) {
	        	document.setType(designer.getType());
	        }
	        
	        designerRepo.save(document);
	        return document;
	}


	@Override
	public Map<String, Object> getDesigner(String id) {
		Designer designer = designerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(MESSAGE + id));
        Map<String, Object> response = new HashMap<>();
        response.put("id", designer.getId());
        response.put("title", designer.getTitle() != null ? designer.getTitle() : "");
        response.put("s3Key", designer.getS3Key() != null ? designer.getS3Key() : "");
        response.put("pages", designer.getPages());
        response.put("recipients", designer.getRecipients() != null ? designer.getRecipients() : List.of());
        response.put("fields", designer.getFields() != null ? designer.getFields() : List.of());
        response.put("status", designer.getStatus() != null ? designer.getStatus() : AppConstants.STATUS_DRAFT);
        response.put("type", designer.getType() != null ? designer.getType() : DOCUMENT);
        
        if (designer.getS3Key() != null && !designer.getS3Key().isEmpty()) {
            String viewUrl = s3Service.generatePresignedGetUrl(designer.getS3Key());
            response.put("viewUrl", viewUrl);
        }
        
        return response;
	}


	@Override
	public Map<String, String> getViewUrl(String id) {
		Designer designer = designerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(MESSAGE + id));
        if (designer.getS3Key() == null || designer.getS3Key().isEmpty()) {
        	throw new ResourceNotFoundException("S3 Key not found for designer: " + id);
        }
        String viewUrl = s3Service.generatePresignedGetUrl(designer.getS3Key());
        return Map.of("url", viewUrl);
	}


	@Override
	public Designer publishDesigner(String id) {
		Designer designer = designerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(MESSAGE + id));
        designer.setUpdatedAt(java.time.Instant.now());
        designer.setStatus(AppConstants.STATUS_PUBLISHED);
        List<Map<String, Object>> users = designer.getRecipients();
        documentCompletionService.sendCompletionEmails(designer, users);
        designerRepo.save(designer);
		return designer;
	}


	@Override
	public Map<String, Object> bulkPublish(String id, List<Map<String, Object>> targetUsers) {
		Designer template = designerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(MESSAGE + id));
		
		int count = 0;
		for (Map<String, Object> userData : targetUsers) {
			try {
				Designer clone = new Designer();
				clone.setId(UUID.randomUUID().toString());
				
				String userDisplayName = (String) userData.get("firstName");
				if (userDisplayName == null || userDisplayName.isEmpty()) {
					userDisplayName = (String) userData.get("userName");
				}
				
				clone.setTitle(String.format("%s - %s", template.getTitle(), userDisplayName));
				clone.setS3Key(template.getS3Key());
				clone.setPages(template.getPages());
				clone.setOwnerUserId(template.getOwnerUserId());
				clone.setStatus(AppConstants.STATUS_PUBLISHED);
				clone.setType(DOCUMENT);
				clone.setCreatedAt(java.time.Instant.now());
				clone.setUpdatedAt(java.time.Instant.now());
				
				// Set this specific user as the recipient
				clone.setRecipients(List.of(userData));
				
				// Clone fields and reassign to this user
				String targetUserId = (String) userData.get("id");
				if (targetUserId == null) {
					targetUserId = (String) userData.get("userId");
				}
				
				final String finalUserId = targetUserId;
				List<Map<String, Object>> clonedFields = new java.util.ArrayList<>();
				for (Map<String, Object> field : template.getFields()) {
					Map<String, Object> newField = new java.util.HashMap<>(field);
					newField.put("userId", finalUserId);
					newField.put("id", "field_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5));
					clonedFields.add(newField);
				}
				clone.setFields(clonedFields);
				
				designerRepo.save(clone);
				
				documentCompletionService.sendCompletionEmails(clone, List.of(userData));
				count++;
			} catch (Exception e) {
				log.error("Failed to clone document for user {}: ", userData.get("userName"), e);
			}
		}
		
		return Map.of("message", "Bulk publish completed", "clonedCount", count);
	}
	
}