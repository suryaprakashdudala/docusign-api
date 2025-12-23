package com.docusign.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.docusign.entity.User;
import java.util.Optional;

public interface UserRepo extends MongoRepository<User, String> {
    Optional<User> findByUserName(String userName);
    
    Optional<User> findByEmail(String email);
    
}
