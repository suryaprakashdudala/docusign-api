package com.docusign.repository;

import com.docusign.entity.EmailQueue;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface EmailRepo extends MongoRepository<EmailQueue, String> {
	
    Optional<EmailQueue> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
    
}
