package com.eisenvault.ocrIntegration;

import java.util.List;


import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;


import com.eisenvault.ocrIntegration.OCRMethods;


/**
 * 
 * Run OCR links on folders---Bengali OCR Folder
 * @author Ankita Pandey
 * 
 */


public class BengaliFolderActionExecuter extends OCRMethods
{

	public static final String NAME = "BengaliOcr-Folder";
	
	/**
	 * @param ruleAction 
	 * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(Action, NodeRef)
	 */
		

	
	public void executeImpl(Action ruleAction, NodeRef actionUponNodeRef)
	{
	    ChildAssociationRef childAssociationRef = nodeService.getPrimaryParent(actionUponNodeRef);

	    iterateThroughChildren(ruleAction,childAssociationRef);
	}
	
	public void iterateThroughChildren( Action ruleAction,ChildAssociationRef childAssocRef)
	{
	    
	    NodeRef childNodeRef = childAssocRef.getChildRef();
	    List<ChildAssociationRef> children = nodeService.getChildAssocs(childNodeRef);

	    int count = 0;
	    for (ChildAssociationRef childAssoc : children) 
	    {
	       childAssoc.getChildRef();
	      // Use childNodeRef here.
	       
	       executeOCR(ruleAction, childAssoc.getChildRef(), NAME, "ben");
	       System.out.println("******Child nodes inside are******"+ childAssoc.getChildRef());
	       count++;
		   System.out.println("******Total nodes count is******"+ count);

	      // This call recurses the method with the new child.
	      iterateThroughChildren(ruleAction,childAssoc);
	      // If there are no children then the list will be empty and so this will be skipped.


	    }
	}

	

	/**
	 * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
	 */

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// Add definitions for action parameters
		/*paramList.add(
				new ParameterDefinitionImpl(                       // Create a new parameter defintion to add to the list
						PARAM_ASPECT_NAME,                              // The name used to identify the parameter
						DataTypeDefinition.QNAME,                       // The parameter value type
						false,                                           // Indicates whether the parameter is mandatory
						getParamDisplayLabel(PARAM_ASPECT_NAME)));   */   // The parameters display label

	}
}

