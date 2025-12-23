package com.docusign.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;


@Document(collection = "designers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Designer extends BaseAuditEntity {
    @Id
    private String id; // designerId (uuid)
    private String title;
    private String ownerUserId;
    private String s3Key; // set after upload
    @Default
    private int pages = 0;
    @Default
    private List<Map<String,Object>> recipients = new ArrayList<>();
    @Default
    private List<Map<String,Object>> fields = new ArrayList<>();
    
    @Default
    private String status = "draft";

}
