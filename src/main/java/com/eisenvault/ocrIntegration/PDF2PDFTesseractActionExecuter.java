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
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.util.concurrent.Service;

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
public class PDF2PDFTesseractActionExecuter extends ActionExecuterAbstractBase {

	private static final Log logger = LogFactory
			.getLog(PDF2PDFTesseractActionExecuter.class);

	private static final String VAR_SOURCE = "source";
	private static final String VAR_TARGET = "target";
	private static final String NAME = "ocr-pdf";
//	private static final String PARAM_DESTINATION_FOLDER = "destination-folder";

	private FileFolderService fileFolderService;
	//private NodeService nodeService;
	private CheckOutCheckInService checkOutCheckInService;

	/**
	 * The executor command performs the actual transformation
	 */
	private RuntimeExec executer;

	/**
	 * The check command is used to confirm that tesseract is actually installed
	 * and functioning correctly. This is essentially based upon exit code. See
	 * the <i>test</i> method for further details
	 */
	private RuntimeExec checkCommand;

	// Injected
	private MimetypeService mimetypeService;

	private boolean available = true;
	private Date lastChecked = new Date(0l);
	private int checkFrequencyInSeconds = 120;

	public void setCheckFrequencyInSeconds(int frequency) {
		checkFrequencyInSeconds = frequency;
	}

	/**
	 * Is the transformer available for use? If at least
	 * [checkFrequencyInSeconds] seconds have elapsed since the last time the
	 * verify command was run, use the verify command to check that Tesseract is
	 * still working, otherwise just use the last return value. The frequency of
	 * testing can be adjusted via the 'checkFrequencyInSeconds' property in
	 * Spring
	 */
	public boolean isAvailable() {
		Date refreshAvailabilityDate = new Date(lastChecked.getTime() + 1000l
				* checkFrequencyInSeconds);
		if (new Date().after(refreshAvailabilityDate)) {
			test();
		}
		return available;
	}

	public void setMimetypeService(MimetypeService ms) {
		mimetypeService = ms;
	}

	/**
	 * Set the execution timeout. A given transform will only be allocated this
	 * amount of time to run, and will be marked as failed if it takes longer,
	 * even if the transform would ultimatley succeed. Note that Tesseract
	 * transforms can take a very long time with long documents on slow
	 * infrastructure - this timeout should be set accordingly.
	 * 
	 * @param timeout
	 */
	public void setTimeout(long timeout) {
		tesseractTimeout = timeout;
	}

	public long tesseractTimeout = 10000000l;

	public void setExecuter(RuntimeExec executer) {
		this.executer = executer;
	}

	public void setCheckCommand(RuntimeExec checkCommand) {
		this.checkCommand = checkCommand;
	}

	protected void test() {
		try {
			logger.debug("Testing availability");
			RuntimeExec.ExecutionResult result = checkCommand.execute();
			available = result.getSuccess();
			logger.info("Is tesseract available? " + available);
		} catch (Exception e) {
			available = false;
			logger.warn("Check command ["
					+ checkCommand.getCommand()
					+ "] failed.  Registering transform as unavailable for the next "
					+ checkFrequencyInSeconds + " seconds");
		}
	}

	 @Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
	//	paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER,
	//			DataTypeDefinition.NODE_REF, true,
	//			getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
	}

	public void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {
		
		NodeRef workingNode=checkOutCheckInService.checkout(actionedUponNodeRef);
		ContentReader reader = fileFolderService.getReader(workingNode);
		 
		try {
			logger.debug("Beginning transform for "
					+ reader.getContentData().getContentUrl());

			String sourceMimetype = reader.getMimetype();
			if (sourceMimetype.equalsIgnoreCase("application/pdf")) {
				
				
				String targetMimetype = "application/pdf";
				String sourceExtension = mimetypeService
						.getExtension(sourceMimetype);
				String targetExtension = mimetypeService
						.getExtension(targetMimetype);

				File sourceFile = TempFileProvider.createTempFile(getClass()
						.getSimpleName() + "_source_", "." + sourceExtension);
				File targetFile = TempFileProvider.createTempFile(getClass()
						.getSimpleName() + "_target_", "." + targetExtension);

				logger.debug("Temp files created");
				reader.getContent(sourceFile);

				logger.debug("Source file written");
				Map<String, String> properties = new HashMap<String, String>(5);

				properties.put(VAR_SOURCE, sourceFile.getAbsolutePath());
				properties.put(VAR_TARGET, targetFile.getAbsolutePath());

				RuntimeExec.ExecutionResult result = executer.execute(
						properties, tesseractTimeout);

				// VS - script auto appends .pdf -- this causes the file name
				// to become XXXX.pdf.pdf
				// code below adds a .pdf at the end to account for the extra
				// .tiff
				File actualLocation = new File(targetFile.getAbsolutePath()
						+ ".pdf");
				actualLocation.renameTo(targetFile);

				if (result.getExitValue() != 0 && result.getStdErr() != null
						&& result.getStdErr().length() > 0) {
					throw new ContentIOException(
							"Failed to perform OCR transformation: \n" + result);
				}
				logger.debug("Transform executed");
				ContentWriter writer = fileFolderService.getWriter(workingNode);
				writer.putContent(targetFile);
				checkOutCheckInService.checkin(workingNode, null);

				logger.info("Transform complete");
			}
		} catch (Exception e) {
			logger.error("Exception during transform", e);
			
		}

	}

	public void setFileFolderService(FileFolderService fileFolderService){
		this.fileFolderService=fileFolderService;
	}
	public void setCheckOutCheckInService(CheckOutCheckInService checkOutCheckinService){
		this.checkOutCheckInService=checkOutCheckinService;
	}

}