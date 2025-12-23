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
    private ObjectId _id; 
    private String id;
    private String email;
    private String otp;
    private LocalDateTime expiresAt;
    private boolean used;
    private String subject;
    
    public void setId(ObjectId _id) {
        this._id = _id;
        this.id = (_id != null) ? _id.toHexString() : null;
    }
    public String getId() {
        if (this.id == null && this._id != null) {
            this.id = this._id.toHexString();
        }
        return this.id;
    }
}
