package com.docusign.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.docusign.entity.Designer;

public interface DesignerRepo extends MongoRepository<Designer, String> {
    List<Designer> findByStatusNot(String status);
    List<Designer> findByStatus(String status);
}
