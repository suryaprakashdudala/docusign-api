package com.docusign.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.docusign.entity.Designer;
import com.docusign.repository.DesignerRepo;
import com.docusign.repository.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignerServiceImpl implements DesignerService {
	
	private final DesignerRepo designerRepo;
    private final S3Service s3Service;
    private final DocumentCompletionService documentCompletionService;
	
	
	@Override
	public List<Designer> findAll() {
	    return designerRepo.findAll()
	            .stream()
	            .filter(designer -> 
	                !designer.getStatus().equalsIgnoreCase("completed")
	            )
	            .toList();
	}


	@Override
	public List<Designer> findAllCompletedDocuments() {
	    return designerRepo.findAll()
	            .stream()
	            .filter(designer -> 
	                designer.getStatus().equalsIgnoreCase("completed")
	            )
	            .toList();
	}


	@Override
	public Designer saveDesignerDocument(Map<String, String> body) {
        Designer d = new Designer();
        d.setId(UUID.randomUUID().toString());
        d.setTitle(body.getOrDefault("title","Untitled"));
        return designerRepo.save(d);
	}


	@Override
	public Map<String, String> getUploadUrl(String id, Map<String, String> body) {
		String filename = body.get("fileName");
	    String contentType = body.getOrDefault("contentType", "application/pdf");
	    String key = String.format("designer-docs/%s/%s", id, filename);
	    String url = s3Service.generatePresignedPutUrl(key, contentType);
		return Map.of("url", url, "key", key);
	}


	@Override
	public Designer updateDesignerDocument(String id, Designer designer) {
		 Designer document = designerRepo.findById(id).orElseThrow();
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
	        
	        designerRepo.save(document);
	        return document;
	}


	@Override
	public Map<String, Object> getDesigner(String id) {
		Designer designer = designerRepo.findById(id).orElseThrow();
        Map<String, Object> response = new HashMap<>();
        response.put("id", designer.getId());
        response.put("title", designer.getTitle() != null ? designer.getTitle() : "");
        response.put("s3Key", designer.getS3Key() != null ? designer.getS3Key() : "");
        response.put("pages", designer.getPages());
        response.put("recipients", designer.getRecipients() != null ? designer.getRecipients() : List.of());
        response.put("fields", designer.getFields() != null ? designer.getFields() : List.of());
        response.put("status", designer.getStatus() != null ? designer.getStatus() : "draft");
        
        // Add view URL if document exists
        if (designer.getS3Key() != null && !designer.getS3Key().isEmpty()) {
            String viewUrl = s3Service.generatePresignedGetUrl(designer.getS3Key());
            response.put("viewUrl", viewUrl);
        }
        
        return response;
	}


	@Override
	public Map<String, String> getViewUrl(String id) throws NotFoundException {
		Designer designer = designerRepo.findById(id).orElseThrow();
        if (designer.getS3Key() == null || designer.getS3Key().isEmpty()) {
        	throw new NotFoundException();
        }
        String viewUrl = s3Service.generatePresignedGetUrl(designer.getS3Key());
        return Map.of("url", viewUrl);
	}


	@Override
	public Designer publishDesigner(String id) {
		Designer designer = designerRepo.findById(id).orElseThrow();
        designer.setUpdatedAt(java.time.Instant.now());
        designer.setStatus("published");
        List<Map<String, Object>> users = designer.getRecipients();
        documentCompletionService.sendCompletionEmails(designer, users);
        designerRepo.save(designer);
		return designer;
	}

	
}