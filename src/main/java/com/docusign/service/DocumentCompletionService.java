package com.docusign.service;


import java.util.List;
import java.util.Map;

import com.docusign.entity.Designer;
import com.docusign.entity.DocumentCompletion;

public interface DocumentCompletionService {

    String saveCompletedDocument(
            String designerId,
            String userId,
            Map<String, Object> fieldValues,
            String token
    );

    Map<String, Object> getDocumentForCompletion(String token);

    // existing methods
    void sendCompletionEmails(Designer designer, List<Map<String, Object>> users);
    DocumentCompletion getCompletionByToken(String token);
    Map<String, Object> getConsolidatedValues(String designerId);
}
