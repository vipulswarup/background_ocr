package com.eisenvault.ocrIntegration;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class OCRMethods extends ActionExecuterAbstractBase {

	protected static final Log logger = LogFactory
			.getLog(OCRMethods.class);
	
	protected FileFolderService fileFolderService;
	protected NodeService nodeService;
	protected CheckOutCheckInService checkOutCheckInService;

	/**
	 * The executor command performs the actual transformation
	 */
	protected RuntimeExec executer;

	/**
	 * The check command is used to confirm that tesseract is actually installed
	 * and functioning correctly. This is essentially based upon exit code. See
	 * the <i>test</i> method for further details
	 */
	protected RuntimeExec checkCommand;

	// Injected
	protected MimetypeService mimetypeService;

	protected boolean available = true;
	protected Date lastChecked = new Date(0l);
	protected int checkFrequencyInSeconds = 120;
	
	protected static final String VAR_SOURCE = "source";
	protected static final String VAR_TARGET = "target";
	protected static final String VAR_LANG = "language";
	protected static final String VAR_SOURCE_EXT = "source_extension";

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
	
	/*
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
	
	
	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		// TODO Auto-generated method stub

	}
	
	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public void setCheckOutCheckInService(
			CheckOutCheckInService checkOutCheckinService) {
		this.checkOutCheckInService = checkOutCheckinService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
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
	
	protected void executeOCR(Action ruleAction, NodeRef actionedUponNodeRef, String NAME, String ocrLanguageParam)
	{
		logger.debug("Language for OCR is:"+ocrLanguageParam);
		try {

			ContentData contentData = (ContentData) nodeService.getProperty(
					actionedUponNodeRef, ContentModel.PROP_CONTENT);
			String sourceMimetype = contentData.getMimetype();
			String targetMimetype = "application/pdf";

			Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
			
			versionProperties.put(VersionModel.PROP_VERSION_TYPE,
					VersionType.MAJOR);

			if (sourceMimetype.equalsIgnoreCase("application/pdf")
					|| sourceMimetype.equalsIgnoreCase("image/tiff")
					|| sourceMimetype.equalsIgnoreCase("image/png")
					|| sourceMimetype.equalsIgnoreCase("image/jpeg")) {

				// Change mimetype to PDF if source mimetype is not pdf
				if (!sourceMimetype.equalsIgnoreCase(targetMimetype)) {
					ContentData cd = (ContentData) nodeService.getProperty(
							actionedUponNodeRef, ContentModel.PROP_CONTENT);
					ContentData newCD = ContentData.setMimetype(cd,
							targetMimetype);
					nodeService.setProperty(actionedUponNodeRef,
							ContentModel.PROP_CONTENT, newCD);

					// change file extension to .pdf in the repository
					String fileName = (String) nodeService.getProperty(
							actionedUponNodeRef, ContentModel.PROP_NAME);
					fileName = FilenameUtils.removeExtension(fileName);
					fileName = fileName + ".pdf";
					nodeService.setProperty(actionedUponNodeRef,
							ContentModel.PROP_NAME, fileName);
					
					versionProperties.put(Version.PROP_DESCRIPTION,
							"OCR Run on this Document & Converted to PDF.");
				}else{
					versionProperties.put(Version.PROP_DESCRIPTION,
							"OCR Run on this Document.");
				}

				NodeRef workingNode = checkOutCheckInService
						.checkout(actionedUponNodeRef);
				ContentReader reader = fileFolderService.getReader(workingNode);

				logger.debug("Beginning transform for "
						+ reader.getContentData().getContentUrl());

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
				properties.put(VAR_SOURCE_EXT, sourceExtension);
				properties.put(VAR_LANG, ocrLanguageParam);

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

				checkOutCheckInService.checkin(workingNode, versionProperties);

				logger.info("Transform complete");
			}
		} catch (Exception e) {
			logger.error("Exception during transform:" + e.getMessage());

		}
	}

}
