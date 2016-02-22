package com.eisenvault.repo.model;

import org.alfresco.service.namespace.QName;

import com.eisenvault.service.namespace.EisenvaultNamespaceService;

public class EisenvaultDocModel {
	
	// Namespaces
    public static final QName QNAME_EISENVAULT_DOC_CONTENT_MODEL_URI  = QName.createQName(EisenvaultNamespaceService.EISENVAULT_CONTENT_MODEL_URI);

    // Types
    public static final QName TYPE_EISENVAULT_DOC = QName.createQName(EisenvaultNamespaceService.EISENVAULT_CONTENT_MODEL_URI, "eisenvaultDoc");
    
    // Properties
    public static final QName PROP_YEAR = QName.createQName(EisenvaultNamespaceService.EISENVAULT_CONTENT_MODEL_URI,"yearOfCreation");
    public static final QName PROP_MONTH= QName.createQName(EisenvaultNamespaceService.EISENVAULT_CONTENT_MODEL_URI,"monthOfCreation");
    public static final QName PROP_DATE= QName.createQName(EisenvaultNamespaceService.EISENVAULT_CONTENT_MODEL_URI,"dateOfCreation");
    public static final QName PROP_SERIAL_NUMBER= QName.createQName(EisenvaultNamespaceService.EISENVAULT_CONTENT_MODEL_URI,"serialNumber"); //Sys DBId
}
