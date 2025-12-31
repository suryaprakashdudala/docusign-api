package com.docusign.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.docusign.entity.Designer;

@Service
public interface DesignerService {
	
	public List<Designer> findAll();
	
	public List<Designer> findAllCompletedDocuments();
	
	public Designer saveDesignerDocument(Map<String,String> body);
	
	public Map<String,String> getUploadUrl(String id, Map<String, String> body);
	
	public Designer updateDesignerDocument(String id, Designer designer);
	
	public Map<String, Object> getDesigner(String id);
	
	public Map<String, String> getViewUrl(String id) throws NotFoundException;
	
	public Designer publishDesigner(String id);

	public Map<String, Object> bulkPublish(String id, List<Map<String, Object>> targetUsers);
	
	
}