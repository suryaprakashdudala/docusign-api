package com.docusign.repository;

import java.time.LocalDateTime;
import com.docusign.entity.EmailQueue;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface EmailRepo extends MongoRepository<EmailQueue, String> {
	
    Optional<EmailQueue> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
    
    void deleteByExpiresAtBefore(LocalDateTime time);
    void deleteByUsedTrue();
}
