package com.docusign.entity;

import lombok.Data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "emails")
public class EmailQueue extends BaseAuditEntity {
	@Id
    private ObjectId objectId; 
    private String id;
    private String email;
    private String otp;
    private LocalDateTime expiresAt;
    private boolean used;
    private String subject;
    
    public void setId(ObjectId objectId) {
        this.objectId = objectId;
        this.id = (objectId != null) ? objectId.toHexString() : null;
    }
    public String getId() {
        if (this.id == null && this.objectId != null) {
            this.id = this.objectId.toHexString();
        }
        return this.id;
    }
}
