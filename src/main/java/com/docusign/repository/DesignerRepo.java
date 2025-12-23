package com.docusign.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.docusign.entity.Designer;

public interface DesignerRepo extends MongoRepository<Designer, String> {}
