package com.docusign.entity;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private ObjectId _id;

    private String userName;
    private String password;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private String id;
    private String email;
    private boolean isFirstTimeLogin;
    private boolean isExternal;

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
