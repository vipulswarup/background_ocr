package com.eisenvault.helpers.behaviors;

import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import com.eisenvault.repo.model.EisenvaultDocModel;

public class SysDBIdGenerator implements
		NodeServicePolicies.OnCreateNodePolicy,
		NodeServicePolicies.OnUpdatePropertiesPolicy {

	// Dependencies
	private NodeService nodeService;
	private PolicyComponent policyComponent;

	// Behaviours
	private Behaviour onCreateNode;
	private Behaviour onUpdateProperties;

	private Logger logger = Logger.getLogger(SysDBIdGenerator.class);

	public void init() {
		if (logger.isDebugEnabled())
			logger.debug("******************* Initializing Sys DBId generator behavior *******************");

		// Create behaviours
		this.onCreateNode = new JavaBehaviour(this, "onCreateNode",
				NotificationFrequency.EVERY_EVENT);

		this.onUpdateProperties = new JavaBehaviour(this, "onUpdateProperties",
				NotificationFrequency.EVERY_EVENT);

		// Bind behaviours to node policies
		this.policyComponent.bindClassBehaviour(QName.createQName(
				NamespaceService.ALFRESCO_URI, "onCreateNode"),
				EisenvaultDocModel.TYPE_EISENVAULT_DOC,
				this.onCreateNode);

		this.policyComponent.bindClassBehaviour(QName.createQName(
				NamespaceService.ALFRESCO_URI, "onUpdateProperties"),
				EisenvaultDocModel.TYPE_EISENVAULT_DOC,
				this.onUpdateProperties);

	}

	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		if (logger.isDebugEnabled())
			logger.debug("Inside onCreateNode");
		fetchSysDBId(childAssocRef.getChildRef());
	}

	@Override
	public void onUpdateProperties(NodeRef nodeRef,
			Map<QName, Serializable> before, Map<QName, Serializable> after) {
		if (logger.isDebugEnabled())
			logger.debug("Inside onUpdateProperties");
		fetchSysDBId(nodeRef);

	}

	/**
	 * 
	 * Method used for generating the evgb:goyalBrotherDocument type fields
	 * 
	 * @param nodeRef
	 *            nodeRef of the node
	 * 
	 */
	private void fetchSysDBId(NodeRef nodeRef) {
		if (logger.isDebugEnabled())
			logger.debug("Inside fetchProperties");

		String serialNumber = "EV"
				+ nodeService.getProperty(nodeRef, ContentModel.PROP_NODE_DBID);

		nodeService.setProperty(nodeRef,
				EisenvaultDocModel.PROP_SERIAL_NUMBER, serialNumber);

	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
}
