package com.eisenvault.webscript;

public class PicklistItem {
	protected String label;
	protected String value;
	
	public PicklistItem(String label,String value){
		this.label = label;
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
