package com.docusign.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditEntity {

    @Id
    private ObjectId objectId;

    @Indexed(unique = true)
    private String userName;
    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private String id;
    private String email;
    
    @JsonProperty("isFirstTimeLogin")
    private boolean isFirstTimeLogin;
    
    @JsonProperty("isExternal")
    private boolean isExternal;

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
