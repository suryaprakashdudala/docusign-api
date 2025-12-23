package com.docusign.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.docusign.entity.User;
import com.docusign.repository.UserRepo;
import com.docusign.security.JwtService;
import com.docusign.service.DocumentCompletionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentCompletionService completionService;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    // Submit completed document
    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitDocument(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldValues =
                (Map<String, Object>) body.get("fieldValues");

        String token = (String) body.get("token");
        String userId = (String) body.get("userId");

        if (token == null || userId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token and userId are required"));
        }

        String captureKey =
                completionService.saveCompletedDocument(
                        id,
                        userId,
                        fieldValues,
                        token
                );

        return ResponseEntity.ok(Map.of(
                "message", "Document submitted successfully",
                "captureKey", captureKey
        ));
    }

    // Get document for completion
    @GetMapping("/complete/{doctoken}")
    public ResponseEntity<Map<String, Object>> getDocumentByToken(@PathVariable String doctoken, @RequestParam(name="isExternal", required = false) boolean isExternal) {
        try {
        	
            Map<String, Object> response =
                    completionService.getDocumentForCompletion(doctoken);

       	 String token = null;
       	 if(isExternal) {
   			 token = jwtService.generateTokenForExternalUser((String) response.get("currentUserId")); 
       	 }else {
       		 User louser = userRepo.findById((String) response.get("currentUserId")).orElseThrow(() -> new UsernameNotFoundException("User not found "));
   			 token = jwtService.generateToken(louser);
       		 
       	 }
            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer "+token).body(Map.of("token", token, "response", response));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
