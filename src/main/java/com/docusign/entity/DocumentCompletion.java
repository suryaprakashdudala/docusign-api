package com.docusign.entity;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_completions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentCompletion extends BaseAuditEntity {
    @Id
    private String id;
    private String designerId;
    private String userId;
    private String token;
    private String status; // pending, completed
    private Map<String, Object> fieldValues;
    private String captureKey;
    // private Instant createdAt; // Handled by BaseAuditEntity
    private Instant completedAt;
}
