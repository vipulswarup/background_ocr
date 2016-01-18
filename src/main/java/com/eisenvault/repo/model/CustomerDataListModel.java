package com.eisenvault.repo.model;

import org.alfresco.service.namespace.QName;

public class CustomerDataListModel {
	public static final String EV_CUSTOMER_DATALIST_MODEL_URI = "http://www.eisenvault.net/model/dl/1.0";
    public static final String EV_CUSTOMER_DATALIST_MODEL_PREFIX = "cdl";
    
    public static final QName TYPE_CUSTOMER_DATALIST_ITEM = QName.createQName(EV_CUSTOMER_DATALIST_MODEL_URI, "customerListItem");
    public static final QName PROP_VALUE = QName.createQName(EV_CUSTOMER_DATALIST_MODEL_URI, "value");
    public static final QName PROP_SORT_ORDER = QName.createQName(EV_CUSTOMER_DATALIST_MODEL_URI, "sortOrder");
}
