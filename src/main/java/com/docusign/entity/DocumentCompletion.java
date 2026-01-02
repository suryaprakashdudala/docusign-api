package com.docusign.entity;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("isExternal")
    private boolean isExternal;
    
    private String token;
    private String status;
    private Map<String, Object> fieldValues;
    private String captureKey;
    private Instant completedAt;
}
