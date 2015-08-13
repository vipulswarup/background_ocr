package com.eisenvault.ocrIntegration;

/**
 This file is part of the Tesseract Alfresco Integration written by
 Simon White.

 Customised by Vipul Swarup for EisenVault - 17 June 2015

 The Integration is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 The Integration is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple transformer, very heavily based upon the Alfresco-provided ImageMagick
 * transformer, to allow for image->text transformation via Tesseract.
 * 
 * Note that simply having an image->text/plain transformation available will
 * let images get indexed by Alfresco, which is nice.
 * 
 * Very basic code so far, intended as a demonstrator/starting point rather than
 * for production use at this stage
 * 
 * TODO: il8n TODO: expand available source mimetypes and make configurable
 * 
 * A singleton bean, instantiated from the service-context.xml in the Tesseract
 * module
 *
 * @author simon_DOT_white_AT_gmail_DOT_com
 */
public class BengaliOCRActionExecuter extends OCRMethods {

	


	private static final String NAME = "Bengali-OCR";
	// private static final String PARAM_DESTINATION_FOLDER =
	// "destination-folder";

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER,
		// DataTypeDefinition.NODE_REF, true,
		// getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
	}

	public void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {

		this.executeOCR(ruleAction, actionedUponNodeRef, NAME, "ben");

	}



}