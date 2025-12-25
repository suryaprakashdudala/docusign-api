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

import com.docusign.entity.DocumentCompletion;
import com.docusign.entity.User;
import com.docusign.repository.UserRepo;
import com.docusign.security.JwtService;
import com.docusign.service.DocumentCompletionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
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
            log.warn("Submission failed for designer {}: Token or userId missing", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token and userId are required"));
        }

        try {
            String captureKey =
                    completionService.saveCompletedDocument(
                            id,
                            userId,
                            fieldValues,
                            token
                    );

            log.info("Document {} submitted successfully by user {}", id, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Document submitted successfully",
                    "captureKey", captureKey
            ));
        } catch (Exception e) {
            log.error("Error submitting document {}: ", id, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Get document for completion
    @GetMapping("/complete/{doctoken}")
    public ResponseEntity<Map<String, Object>> getDocumentByToken(@PathVariable String doctoken) {
        try {
            Map<String, Object> response =
                    completionService.getDocumentForCompletion(doctoken);

            String currentUserId = (String) response.get("currentUserId");
            
            // Refetch completion to check isExternal status saved in DB
            DocumentCompletion completion = completionService.getCompletionByToken(doctoken);
            
            String token = null;
            if (completion.isExternal()) {
                log.info("Processing External User token generation");
                token = jwtService.generateTokenForExternalUser(currentUserId);
            } else {
                log.info("Processing Internal User token generation");
                User louser = userRepo.findById(currentUserId)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserId));
                token = jwtService.generateToken(louser);
            }
            
            log.info("Token generated for user {} accessing document via token {}", currentUserId, doctoken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of("token", token, "response", response));

        } catch (Exception e) {
            log.error("Failed to fetch document for token {}: ", doctoken, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
