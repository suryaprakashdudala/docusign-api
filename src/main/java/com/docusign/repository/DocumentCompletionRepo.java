package com.docusign.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.docusign.entity.DocumentCompletion;

public interface DocumentCompletionRepo extends MongoRepository<DocumentCompletion, String> {
    Optional<DocumentCompletion> findByToken(String token);
    Optional<DocumentCompletion> findByTokenAndUserId(String token, String userId);
    Optional<DocumentCompletion> findByDesignerIdAndUserId(String designerId, String userId);
    Optional<DocumentCompletion> findByUserId(String userId);
    long countByDesignerIdAndStatus(String designerId, String status);
    java.util.List<DocumentCompletion> findByDesignerId(String designerId);
}
