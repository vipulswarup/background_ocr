package com.eisenvault.webscript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class NodeAuditTrailWebScript extends DeclarativeWebScript {
	private static Log logger = LogFactory
			.getLog(NodeAuditTrailWebScript.class);
	private NodeService nodeService;
	private AuditService auditService;
	private Repository repository;
	private NamespaceService namespaceService;

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		try {
			String nodeRefString = req.getParameter("nodeRef");
			if (StringUtils.isBlank(nodeRefString))
				throw new Exception(
						"'nodeRef' missing while processing request...");

			nodeRefString = nodeRefString.replace(":/", "");
			NodeRef nodeRef = repository.findNodeRef("node",
					nodeRefString.split("/"));

			List<TemplateAuditInfo> auditTrailList = getAuditTrail(nodeRef);
			List<Map<String, Object>> auditList = new ArrayList<Map<String, Object>>(
					auditTrailList.size());

			for (TemplateAuditInfo auditInfo : auditTrailList) {
				if (logger.isDebugEnabled()) {
					logger.debug(auditInfo.toString());
				}
				Map<String, Object> auditInfoMap = new HashMap<String, Object>();
				auditInfoMap.put("userName", auditInfo.getUserIdentifier());
				auditInfoMap.put("applicationName",
						auditInfo.getAuditApplication());
				auditInfoMap.put("applicationMethod",
						auditInfo.getAuditMethod());
				auditInfoMap.put("date", auditInfo.getDate());
				auditInfoMap.put("propertiesUpdate",
						auditInfo.getUpdatedPropertiesList());
				// auditInfoMap.put("auditValues", auditInfo.getValues());
				if (auditInfo.getValues() != null) {
					// Convert values to Strings
					Map<String, String> valueStrings = new HashMap<String, String>(
							auditInfo.getValues().size() * 2);
					for (Map.Entry<String, Serializable> mapEntry : auditInfo
							.getValues().entrySet()) {
						String key = mapEntry.getKey();
						Serializable value = mapEntry.getValue();
						try {
							String valueString = DefaultTypeConverter.INSTANCE
									.convert(String.class, value);
							valueStrings.put(key, valueString);
						} catch (TypeConversionException e) {
							valueStrings.put(key, value.toString());
						}
					}
					auditInfoMap.put("auditValues", valueStrings);
				}
				if(auditInfo.getAuditMethod().equals("PROPERTIES UPDATED") && auditInfo.getUpdatedPropertiesList().get(0).equals(" ")){
					continue;
				}
				auditList.add(auditInfoMap);
			}
			Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
			String fileName = (String) props.get(ContentModel.PROP_NAME);

			model.put("data", auditList);
			model.put("nodeRef", nodeRef.getId());
			model.put("fileName", fileName);
			model.put("count", auditList.size());
			model.put("returnStatus", Boolean.TRUE);
			model.put(
					"statusMessage",
					"Successfully retrieved audit trail for nodeRef["
							+ nodeRef.getId() + "]");

		} catch (Exception e) {
			logger.warn(e.getMessage());
			model.put("returnStatus", Boolean.FALSE);
			model.put("statusMessage", e.getMessage());
		}
		return model;
	}

	private List<TemplateAuditInfo> getAuditTrail(NodeRef nodeRef) {
		final List<TemplateAuditInfo> result = new ArrayList<TemplateAuditInfo>();

		final AuditQueryCallback callback = new AuditQueryCallback() {

			public boolean valuesRequired() {
				return true;
			}

			public boolean handleAuditEntryError(Long entryId, String errorMsg,
					Throwable error) {
				throw new AlfrescoRuntimeException(
						"Failed to retrieve audit data.", error);
			}

			public boolean handleAuditEntry(Long entryId,
					String applicationName, String userName, long time,
					Map<String, Serializable> values) {
				TemplateAuditInfo auditInfo = new TemplateAuditInfo(
						applicationName, userName, time, values);
				result.add(auditInfo);
				return true;
			}
		};

		// resolve the path of the node
		final String nodePath = ISO9075.decode(nodeService.getPath(nodeRef)
				.toPrefixString(namespaceService));

		AuthenticationUtil.runAs(new RunAsWork<Object>() {
			public Object doWork() throws Exception {
				String applicationName = "alfresco-access";
				AuditQueryParameters pathParams = new AuditQueryParameters();
				pathParams.setApplicationName(applicationName);
				pathParams.addSearchKey("/alfresco-access/transaction/path",
						nodePath);
				auditService.auditQuery(callback, pathParams, -1);

				AuditQueryParameters copyFromPathParams = new AuditQueryParameters();
				copyFromPathParams.setApplicationName(applicationName);
				copyFromPathParams
						.addSearchKey(
								"/alfresco-access/transaction/copy/from/path",
								nodePath);
				auditService.auditQuery(callback, copyFromPathParams, -1);

				AuditQueryParameters moveFromPathParams = new AuditQueryParameters();
				moveFromPathParams.setApplicationName(applicationName);
				moveFromPathParams
						.addSearchKey(
								"/alfresco-access/transaction/move/from/path",
								nodePath);
				auditService.auditQuery(callback, moveFromPathParams, -1);

				return null;
			}
		}, AuthenticationUtil.getAdminUserName());

		// sort audit entries by time of generation
		Collections.sort(result, new Comparator<TemplateAuditInfo>() {
			public int compare(TemplateAuditInfo o1, TemplateAuditInfo o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
		return result;
	}

	public class TemplateAuditInfo {
		private String applicationName;
		private String userName;
		private long time;
		private Map<String, Serializable> values;

		public TemplateAuditInfo(String applicationName, String userName,
				long time, Map<String, Serializable> values) {
			this.applicationName = applicationName;
			this.userName = userName;
			this.time = time;
			this.values = values;
		}

		public String getAuditApplication() {
			return this.applicationName;
		}

		public String getUserIdentifier() {
			return this.userName;
		}

		public Date getDate() {
			return new Date(time);
		}

		public String getAuditMethod() {
			if (this.values.get("/alfresco-access/transaction/action").equals(
					"updateNodeProperties")) {
				return "PROPERTIES UPDATED";
			} else if (this.values.get("/alfresco-access/transaction/action")
					.equals("readContent")) {
				return "READ CONTENT";
			} else
				return this.values.get("/alfresco-access/transaction/action")
						.toString();
		}

		public Map<String, Serializable> getValues() {
			return this.values;
		}

		public List<String> getUpdatedPropertiesList() {
			if (this.values.get("/alfresco-access/transaction/action").equals(
					"updateNodeProperties") && (this.values
							.get("/alfresco-access/transaction/properties/from") != null)) {

				// Get the map of properities that are updated
				HashMap previousValuesMap = (HashMap) this.values
						.get("/alfresco-access/transaction/properties/from");
				HashMap updatedValuesMap = (HashMap) this.values
						.get("/alfresco-access/transaction/properties/to");

				// This list will contain the string declaring properties
				// updated from-->to
				List<String> propertiesUpdatedFromTo = new ArrayList<String>();

				String[] property = null;
				String previousPropertyValue = null;
				String updatedPropertyValue = null;

				// loop for properties: name, updated from, updated to
				for (Object key : previousValuesMap.keySet()) {

					// Sample data that we are trying to parse
					// /alfresco-access/transaction/properties/from =
					// {{http://www.alfresco.org/model/content/1.0}title={en=yello},
					// {http://www.alfresco.org/model/content/1.0}modified=Fri
					// Mar 11 13:00:27 IST 2016,
					// {http://www.alfresco.org/model/content/1.0}author=me}
					property = key.toString().split("}");

					//taggable coming as null if not provided a value
					if(previousValuesMap.get(key) == null){
						previousPropertyValue = "\"\"";
					}
					//Some values are in format "{a=b}" and some are "a=b"
					else if (previousValuesMap.get(key).toString().startsWith("{")) {
						String[] mapValues = previousValuesMap.get(key)
								.toString().split("=");
						// done to remove the trailing "}"
						previousPropertyValue = mapValues[1].substring(0,
								mapValues[1].length() - 1);
					} else {
						previousPropertyValue = previousValuesMap.get(key)
								.toString();
						// when property is taggable the value is node path
						// "workspace://SpacesStore/d18eb8b0-fc01-466a-b5c8-cadbe3c3192d"
						if (property[1].equals("taggable")) {
							// Get the names of tags from the node path
							previousPropertyValue = getTaggablePropValue(previousPropertyValue
									.substring(1,
											previousPropertyValue.length() - 1));
						}
					}
					if (previousPropertyValue.equals("")) {
						previousPropertyValue = "\"\"";
					}

					//Parsing based on similar logic as explained for above(previousValuesMap)
					if(updatedValuesMap.get(key) == null){
						updatedPropertyValue = "\"\"";
					}
					else if (updatedValuesMap.get(key).toString().startsWith("{")) {
						String[] mapValues = updatedValuesMap.get(key)
								.toString().split("=");
						updatedPropertyValue = mapValues[1].substring(0,
								mapValues[1].length() - 1);
					} else {
						updatedPropertyValue = updatedValuesMap.get(key)
								.toString();
						if (property[1].equals("taggable")) {
							updatedPropertyValue = getTaggablePropValue(updatedPropertyValue
									.substring(1,
											updatedPropertyValue.length() - 1));
						}
					}
					if (updatedPropertyValue.equals("")) {
						updatedPropertyValue = "\"\"";
					}
					// add the properties to list
					propertiesUpdatedFromTo.add("Property: " + property[1]
							+ ", updated From: " + previousPropertyValue
							+ ", To: " + updatedPropertyValue);
				}

				// Some properties when given values first time don't come as
				// updated values, they come as values that are added
				if (this.values
						.get("/alfresco-access/transaction/properties/add") != null) {
					// get the map of added properties
					HashMap addedPropertiesMap = (HashMap) this.values
							.get("/alfresco-access/transaction/properties/add");
					String addedPropertyValue = null;

					for (Object key : addedPropertiesMap.keySet()) {
						property = key.toString().split("}");
						// taggable coming as null if not provided a value
						if (addedPropertiesMap.get(key) == null) {

						} else if (addedPropertiesMap.get(key).toString()
								.startsWith("{")) {
							String[] mapValues = addedPropertiesMap.get(key)
									.toString().split("=");
							// Some properties even though not added were coming
							// in the map with empty value
							if (StringUtils.isBlank(mapValues[1].substring(0,
									mapValues[1].length() - 1))) {

							} else {
								addedPropertyValue = mapValues[1].substring(0,
										mapValues[1].length() - 1);
								propertiesUpdatedFromTo.add("Property: "
										+ property[1]
										+ " updated From: \"\", To: "
										+ addedPropertyValue);
							}
						} else {
							//Some properties even though not added were coming in the map with empty value
							if(StringUtils.isBlank(addedPropertiesMap.get(key)
									.toString())){
								
							}else{
								addedPropertyValue = addedPropertiesMap.get(key)
										.toString();
								if(property[1].equals("taggable")){
									addedPropertyValue = getTaggablePropValue(addedPropertyValue.substring(1, addedPropertyValue.length()-1));
								}
								propertiesUpdatedFromTo.add("Property: "
										+ property[1]+ " updated From: \"\", To: "
										+ addedPropertyValue);
							}
						}
					}
				}
				return propertiesUpdatedFromTo;
			}
			return new ArrayList<String>(Arrays.asList(" "));
		}

		// Returns taggable properties values from their respective node path
		public String getTaggablePropValue(String taggablePropertyValue) {
			StringBuffer tagNames = new StringBuffer();
			String[] tagsNodeValue = taggablePropertyValue.split(",");
			for (int i = 0; i < tagsNodeValue.length; i++) {
				// Get the nodeRef
				NodeRef nodeRef = new NodeRef(tagsNodeValue[i].trim());
				tagNames.append(nodeService.getProperty(nodeRef,
						ContentModel.PROP_NAME).toString()
						+ ", ");
			}
			return tagNames.toString();
		}

		public String toString() {
			return "TemplateAuditInfo [applicationName=" + applicationName
					+ ",userName= " + userName + ",time= " + time + ",values= "
					+ values + "]";
		}
	}
}
