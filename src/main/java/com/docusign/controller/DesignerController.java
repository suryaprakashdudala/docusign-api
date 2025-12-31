package com.docusign.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docusign.entity.Designer;
import com.docusign.exception.ResourceNotFoundException;
import com.docusign.repository.DesignerRepo;
import com.docusign.service.DesignerService;
import com.docusign.service.DocumentCompletionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/designers")
@RequiredArgsConstructor
@Slf4j
public class DesignerController {


    private final DesignerRepo designerRepo;
    private final DesignerService  designerService;
    private final DocumentCompletionService documentCompletionService;

    // 1) create designer (returns designerId)
    @PostMapping
    public ResponseEntity<Designer> createDesigner(@RequestBody Map<String,String> body) {
    	Designer designer =  designerService.saveDesignerDocument(body);
        return ResponseEntity.ok(designer);
    }

    // 2) generate upload presigned URL for a file
    @PostMapping("/{id}/upload-url")
    public ResponseEntity<Map<String,String>> getUploadUrl(@PathVariable String id, @RequestBody Map<String,String> body) {
        Map<String, String> map = designerService.getUploadUrl(id, body);
        return ResponseEntity.ok(map);
    }

    // 3) update metadata (after upload/parse)
    @PutMapping("/{id}")
    public ResponseEntity<Designer> updateDesigner(@PathVariable String id, @RequestBody Designer designer) {
        Designer stored = designerService.updateDesignerDocument(id, designer);
        return ResponseEntity.ok(stored);
    }

    // 5) get designer with document view URL
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDesigner(@PathVariable String id) {
    	
        Map<String, Object> response = designerService.getDesigner(id);        
        return ResponseEntity.ok(response);
    }

    // 4) generate presigned URL for viewing/downloading the document
    @GetMapping("/{id}/view-url")
    public ResponseEntity<Map<String,String>> getViewUrl(@PathVariable String id) {
        try {
        	Map<String, String> map = designerService.getViewUrl(id); 
        	return ResponseEntity.ok(map);
        }catch(Exception e) {
            log.error("Failed to get view URL for designer {}: ", id, e);
        	return ResponseEntity.badRequest().body(Map.of("error", "No document uploaded for this designer"));
        }
    }

    // 6) Send emails to selected users
   // @PostMapping("/{id}/send-emails")
    public ResponseEntity<Map<String, Object>> sendEmails(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Designer designer = designerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Designer not found with id: " + id));
        
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        
        if (users == null || users.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No users provided"));
        }
        // Generate completion tokens and send email
        documentCompletionService.sendCompletionEmails(designer, users);
        designerRepo.save(designer);
        
        return ResponseEntity.ok(Map.of("message", "Emails sent successfully", "sentTo", users.size()));
    }


    @GetMapping("/{id}/all-values")
    public ResponseEntity<Map<String, Object>> getAllValues(@PathVariable String id) {
        Map<String, Object> allValues = documentCompletionService.getConsolidatedValues(id);
        return ResponseEntity.ok(allValues);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<Designer>> getAllDesigners() {
        return ResponseEntity.ok(designerService.findAll());
    }
    
    @GetMapping("/all/completed")
    public ResponseEntity<List<Designer>> getAllCompletedDocuments() {
        return ResponseEntity.ok(designerService.findAllCompletedDocuments());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesigner(@PathVariable String id) {
        designerRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<Designer> publishDesigner(@PathVariable String id) {
        Designer designer = designerService.publishDesigner(id);
        return ResponseEntity.ok(designer);
    }

    @PostMapping("/{id}/bulk-publish")
    public ResponseEntity<Map<String, Object>> bulkPublish(@PathVariable String id, @RequestBody Map<String, Object> body) {
        List<Map<String, Object>> targetUsers = (List<Map<String, Object>>) body.get("users");
        if (targetUsers == null || targetUsers.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No target users provided"));
        }
        Map<String, Object> result = designerService.bulkPublish(id, targetUsers);
        return ResponseEntity.ok(result);
    }
}
