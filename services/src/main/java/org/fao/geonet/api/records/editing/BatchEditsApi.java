/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */
package org.fao.geonet.api.records.editing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.Util;
import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiParams;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.api.processing.report.ErrorReport;
import org.fao.geonet.api.processing.report.IProcessingReport;
import org.fao.geonet.api.processing.report.InfoReport;
import org.fao.geonet.api.processing.report.SimpleMetadataProcessingReport;
import org.fao.geonet.api.records.model.BatchEditParameter;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.Setting;
import org.fao.geonet.domain.SettingDataType;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.AddElemValue;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.EditLib;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.SelectionManager;
import org.fao.geonet.kernel.aws.S3Operation;
import org.fao.geonet.kernel.batchedit.BatchEditParam;
import org.fao.geonet.kernel.batchedit.BatchEditReport;
import org.fao.geonet.kernel.batchedit.BatchEditXpath;
import org.fao.geonet.kernel.batchedit.CSVBatchEdit;
import org.fao.geonet.kernel.schema.MetadataSchema;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.kernel.setting.Settings;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.repository.SettingRepository;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetObjectAclRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jeeves.server.context.ServiceContext;
import jeeves.services.ReadWriteController;

@RequestMapping(value = { "/api/records", "/api/" + API.VERSION_0_1 + "/records" })
@Api(value = "records", tags = "records", description = "Metadata record editing operations")
@Controller("records/edit")
@ReadWriteController
public class BatchEditsApi implements ApplicationContextAware {
	@Autowired
	SchemaManager _schemaManager;
	private ApplicationContext context;
	// List<BatchEditReport> reports;
	AmazonS3URI s3uri = new AmazonS3URI(Geonet.BATCHEDIT_BACKUP_BUCKET);
	List<Metadata> tempBackupData = new ArrayList<>();
	Gson g = new Gson();
	
	double pct;
	SimpleMetadataProcessingReport report = new SimpleMetadataProcessingReport();
	
	public synchronized void setApplicationContext(ApplicationContext context) {
		this.context = context;
	}

	/**
	 * The service edits to the current selection or a set of uuids.
	 */
	@ApiOperation(value = "Edit a set of records by XPath expressions", nickname = "batchEdit")
	@RequestMapping(value = "/batchediting", method = RequestMethod.PUT, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Return a report of what has been done."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT) })
	@PreAuthorize("hasRole('Editor')")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public IProcessingReport batchEdit(
			@ApiParam(value = ApiParams.API_PARAM_RECORD_UUIDS_OR_SELECTION, required = false, example = "iso19139") @RequestParam(required = false) String[] uuids,
			@ApiParam(value = ApiParams.API_PARAM_BUCKET_NAME, required = false) @RequestParam(required = false) String bucket,
			@RequestBody BatchEditParameter[] edits, HttpServletRequest request) throws Exception {

		List<BatchEditParameter> listOfUpdates = Arrays.asList(edits);
		if (listOfUpdates.size() == 0) {
			throw new IllegalArgumentException("At least one edit must be defined.");
		}

		ServiceContext serviceContext = ApiUtils.createServiceContext(request);
		final Set<String> setOfUuidsToEdit;
		if (uuids == null) {
			SelectionManager selectionManager = SelectionManager.getManager(serviceContext.getUserSession());

			synchronized (selectionManager.getSelection(bucket)) {
				final Set<String> selection = selectionManager.getSelection(bucket);
				setOfUuidsToEdit = Sets.newHashSet(selection);
			}
		} else {
			setOfUuidsToEdit = Sets.newHashSet(Arrays.asList(uuids));
		}

		if (setOfUuidsToEdit.size() == 0) {
			throw new IllegalArgumentException("At least one record should be defined or selected for updates.");
		}

		ConfigurableApplicationContext appContext = ApplicationContextHolder.get();
		DataManager dataMan = appContext.getBean(DataManager.class);
		SchemaManager _schemaManager = context.getBean(SchemaManager.class);
		AccessManager accessMan = context.getBean(AccessManager.class);
		final String settingId = Settings.SYSTEM_CSW_TRANSACTION_XPATH_UPDATE_CREATE_NEW_ELEMENTS;
		boolean createXpathNodeIfNotExists = context.getBean(SettingManager.class).getValueAsBool(settingId);

		SimpleMetadataProcessingReport report = new SimpleMetadataProcessingReport();
		report.setTotalRecords(setOfUuidsToEdit.size());

		String changeDate = null;
		final MetadataRepository metadataRepository = context.getBean(MetadataRepository.class);
		for (String recordUuid : setOfUuidsToEdit) {
			Metadata record = metadataRepository.findOneByUuid(recordUuid);
			if (record == null) {
				report.incrementNullRecords();
			} else if (!accessMan.isOwner(serviceContext, String.valueOf(record.getId()))) {
				report.addNotEditableMetadataId(record.getId());
			} else {
				// Processing
				try {
					EditLib editLib = new EditLib(_schemaManager);
					MetadataSchema metadataSchema = _schemaManager.getSchema(record.getDataInfo().getSchemaId());
					Element metadata = record.getXmlData(false);
					boolean metadataChanged = false;

					Iterator<BatchEditParameter> listOfUpdatesIterator = listOfUpdates.iterator();
					while (listOfUpdatesIterator.hasNext()) {
						BatchEditParameter batchEditParameter = listOfUpdatesIterator.next();

						AddElemValue propertyValue = new AddElemValue(batchEditParameter.getValue());

						metadataChanged = editLib.addElementOrFragmentFromXpath(metadata, metadataSchema,
								batchEditParameter.getXpath(), propertyValue, createXpathNodeIfNotExists);
					}
					if (metadataChanged) {
						boolean validate = false;
						boolean ufo = false;
						boolean index = true;
						dataMan.updateMetadata(serviceContext, record.getId() + "", metadata, validate, ufo, index,
								"eng", // Not used when validate is false
								changeDate, false);
						report.addMetadataInfos(record.getId(), "Metadata updated.");
					}
				} catch (Exception e) {
					report.addMetadataError(record.getId(), e);
				}
				report.incrementProcessedRecords();
			}
		}
		report.close();
		return report;
	}

	/**
	 * The service updates records by uploading the csv file
	 */
	@ApiOperation(value = "Updates records by uploading the csv file")
	@RequestMapping(value = "/batchediting/csv", method = RequestMethod.POST, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Return a report of what has been done."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT) })
	@PreAuthorize("hasRole('Administrator')")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public void batchUpdateUsingCSV(@RequestParam(value = "file") MultipartFile file,
			@RequestParam(value = "mode") String mode, @RequestParam(value = "desc") String desc, HttpServletRequest request) {

		ServiceContext serviceContext = ApiUtils.createServiceContext(request);
		if(tempBackupData != null && tempBackupData.size() > 0){
			tempBackupData.clear();
		}
		Log.debug(Geonet.SEARCH_ENGINE, "ECAT, BatchEditsApi mode: " + mode);

		// File csvFile = new File(file.getOriginalFilename());
		try {
			clearReport(report);
			// csvFile.createNewFile();
			File csvFile = File.createTempFile(file.getOriginalFilename(), "csv");
			FileUtils.copyInputStreamToFile(file.getInputStream(), csvFile);
			
			Runnable task = () -> {
				Log.debug(Geonet.SEARCH_ENGINE, "BatchEditAPI calling... startBackupOperation........");
				processCsv(csvFile, context, serviceContext, mode, desc);
			};

			// start the thread
			new Thread(task).start();

		} catch (Exception e) {
			Log.error(Geonet.SEARCH_ENGINE, "ECAT, BatchEditsApi (C) Stacktrace is\n" + Util.getStackTrace(e));
			report.addError(e);
		}

		//return report;

	}
	
	/**
	 * The service updates records by uploading the csv file
	 */
	@ApiOperation(value = "Get batch edit report history.")
	@RequestMapping(value = "/batchediting/report", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Return a report of what has been done."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT) })
	@PreAuthorize("hasRole('Administrator')")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public int batchUpdateReport(HttpServletRequest request) {
		return report.getNumberOfRecordsProcessed();
	}
	
	/**
	 * The service updates records by uploading the csv file
	 */
	@ApiOperation(value = "Get batch edit report history.")
	@RequestMapping(value = "/batchediting/history", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Return a report of what has been done."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT) })
	@PreAuthorize("hasRole('Administrator')")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public String batchUpdateHistory(HttpServletRequest request) {
		
		try{
			
			SettingRepository settingRepo = context.getBean(SettingRepository.class);
			Setting sett = settingRepo.findOne(Settings.METADATA_BATCHEDIT_HISTORY);
	
			if(sett != null){
				return sett.getValue();
			}
		}catch(Exception e){}
		
		return null;

	}
	
	/**
	 * The service updates records by uploading the csv file
	 */
	@ApiOperation(value = "Get batch edit report history.")
	@RequestMapping(value = "/batchediting/history/{key}", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Return a report of what has been done."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT) })
	@PreAuthorize("hasRole('Administrator')")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public String batchUpdateHistoryReport(@PathVariable String key, HttpServletRequest request) {
		
		StringBuilder sb = new StringBuilder();
		
		try {
			S3Object object = getS3Client().getObject(new GetObjectRequest(s3uri.getBucket(), key + ".json"));
			InputStream objectData = object.getObjectContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(objectData));
	        String line = null;
	        
	        while ((line = reader.readLine()) != null) {
	            sb.append(line);
	        }
	        
			objectData.close();
		} catch (Exception e) {
			return "";
		}

		return sb.toString();

	}
	
	
	
	/**
	 * The service updates records by uploading the csv file
	 */
	@ApiOperation(value = "Delete batch edit report history.")
	@RequestMapping(value = "/batchediting/purge", method = RequestMethod.DELETE, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Return a report of what has been done."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT) })
	@PreAuthorize("hasRole('Administrator')")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public void purgeBatchUpdateHistory(HttpServletRequest request) {
		
		try{
	
			SettingRepository settingRepo = context.getBean(SettingRepository.class);
			Setting sett = settingRepo.findOne(Settings.METADATA_BATCHEDIT_HISTORY);
	
			if(sett != null){
				settingRepo.delete(sett);
			}
		}catch(Exception e){}
		
	}

	/**
	 * 
	 * @param csvFile
	 * @param context
	 * @param serviceContext
	 * @param mode
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void processCsv(File csvFile, ApplicationContext context,
			ServiceContext serviceContext, String mode, String desc) {
		
		
		// Create folder in s3 bucket with current date
		Date datetime = new Date(System.currentTimeMillis());
		final String dateTimeStr = Geonet.DATE_FORMAT.format(datetime);
		
		
		
		SAXBuilder sb = new SAXBuilder();
		// final CSVBatchEdit cbe = context.getBean(CSVBatchEdit.class);
		CSVBatchEdit cbe = new CSVBatchEdit(context);
		final MetadataRepository metadataRepository = context.getBean(MetadataRepository.class);
		final SchemaManager schemaManager = context.getBean(SchemaManager.class);
		final DataManager dataMan = context.getBean(DataManager.class);
		EditLib editLib = new EditLib(schemaManager);
		final SettingRepository settingRepo = context.getBean(SettingRepository.class);

		
		
		final BatchEditXpath bxpath = context.getBean(BatchEditXpath.class);
		Map<String, XPath> xpathExpr = bxpath.getXPathExpr();
		
		final String s3key = dateTimeStr;
		Log.debug(Geonet.SEARCH_ENGINE, "CSVRecord, BatchEditsApi --> s3key : " + s3key);
		CSVParser parser = null;
		try {
			// Parse the csv file
			parser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.EXCEL.withHeader());
			// report.setTotalRecords(parser.getRecords().size());
		} catch (IOException e1) {
			Log.error(Geonet.SEARCH_ENGINE, e1.getMessage());
		}
		// Currently only supports iso19115-3 standard
		Path p = schemaManager.getSchemaDir("iso19115-3");
		for (CSVRecord csvr : parser) {

			final int id;

			//Log.debug(Geonet.SEARCH_ENGINE, "CSVRecord, BatchEditsApi --> csvRecord.toString() : " + csvr.toString());
			try {
				Metadata record = null;
				if (!csvr.isMapped("uuid")) {
					if (csvr.isMapped("eCatId")) {
						id = Integer.parseInt(csvr.get("eCatId"));

						Log.debug(Geonet.SEARCH_ENGINE,
								"CSVRecord, BatchEditsApi --> csvRecord.get(eCatId) : " + csvr.get("eCatId"));

						// Search record based on eCatId from lucene index
						Element request = Xml
								.loadString("<request><isAdmin>true</isAdmin><_isTemplate>n</_isTemplate><eCatId>"
										+ csvr.get("eCatId") + "</eCatId><fast>index</fast></request>", false);
						try {
							record = cbe.getMetadataByLuceneSearch(context, serviceContext, request);
						} catch (BatchEditException e) {
							report.addMetadataError(id, new Exception(e.getMessage()));
							// report.incrementNullRecords();
							continue;
						}
					} else {// If there is no valid uuid and ecatId, doesn't
							// process this record and continue to execute next
							// record
						report.addError(new Exception("Unable to process record number " + csvr.getRecordNumber()));
						// report.incrementNullRecords();
						continue;
					}

				} else {// find record by uuid, if its defined in csv file
					record = metadataRepository.findOneByUuid(csvr.get("uuid"));
					if (csvr.isMapped("eCatId")) {
						id = Integer.parseInt(csvr.get("eCatId"));
					} else {
						id = record.getId();
					}

				}

				if (record == null) {
					report.addError(new Exception(
							"No metadata found, Unable to process record number " + csvr.getRecordNumber()));
					// report.incrementNullRecords();
					continue;
				}

				
				if (!saveToS3Bucket(record)) {
					report.addError(new Exception("Unable to backup record uuid/ecat: " + id));
					continue;
				}

				MetadataSchema metadataSchema = schemaManager.getSchema(record.getDataInfo().getSchemaId());
				Element metadata = record.getXmlData(false);

				Document document = sb.build(new StringReader(record.getData()));

				Iterator iter = parser.getHeaderMap().entrySet().iterator();
				List<BatchEditParam> listOfUpdates = new ArrayList<>();

				// Iterate through all csv records, and create list of batchedit
				// parameter with xpath and values
				while (iter.hasNext()) {
					Map.Entry<String, Integer> header = (Map.Entry<String, Integer>) iter.next();
					Log.debug(Geonet.SEARCH_ENGINE, header.getKey() + " - " + header.getValue());

					XPath _xpath = xpathExpr.get(header.getKey());

					if (_xpath != null) {
						BatchEditReport batchreport = cbe.removeOrAddElements(context, serviceContext, header, csvr,
								_xpath, document, listOfUpdates, mode);
						batchreport.getErrorInfo().stream().forEach(err -> {
							report.addMetadataError(id, new Exception(err));
						});
						batchreport.getProcessInfo().stream().forEach(info -> {
							report.addMetadataInfos(id, info);
						});
					}
				}

				boolean metadataChanged = false;

				Iterator<BatchEditParam> listOfUpdatesIterator = listOfUpdates.iterator();
				Log.debug(Geonet.SEARCH_ENGINE, "BatchEditsApi --> listOfUpdates : " + listOfUpdates.size());

				metadata = document.getRootElement();

				// Iterate through batchedit parameter list and add elements
				while (listOfUpdatesIterator.hasNext()) {
					BatchEditParam batchEditParam = listOfUpdatesIterator.next();

					AddElemValue propertyValue = new AddElemValue(batchEditParam.getValue());

					metadataChanged = editLib.addElementOrFragmentFromXpath(metadata, metadataSchema,
							batchEditParam.getXpath(), propertyValue, true);
				}

				if (metadataChanged) {
					Log.debug(Geonet.SEARCH_ENGINE, "BatchEditsApi --> updating Metadata: "  + record.getId());
					dataMan.updateMetadata(serviceContext, record.getId() + "", metadata, false, false, true, "eng",
							null, false);
					report.addMetadataInfos(id, "Metadata updated, uuid: " + record.getUuid());
					report.incrementProcessedRecords();
				}

			} catch (Exception e) {
				Log.error(Geonet.SEARCH_ENGINE, "Exception :" + e.getMessage());
			}

		}

		// create entry for this batch edit in s3 bucket
		boolean isEntered = addEntry(report, s3key, settingRepo, desc);
		if(!isEntered){
			report.addError(new Exception("Unable to create an entry for this batch edit operation. So manually has to recall from aws s3 location."));
		}
		
		Log.debug(Geonet.SEARCH_ENGINE, "BatchEditAPI calling... startBackupOperation........");
		startBackupOperation(s3key);
		
	}

	public AmazonS3 getS3Client(){
		AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(s3uri.getRegion()).build();
		return s3client;
	}
	/**
	 * Backup metadata into s3 bucket in order to revert back incase it went wrong or or need to change to old state 
	 * @param s3client
	 * @param dateTimeStr - dateTime value as folder name
	 * @param md
	 * @return
	 */
	private boolean saveToS3Bucket(Metadata md) {
		try {
			tempBackupData.add(md);
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public void startBackupOperation(String s3key){
		
		AmazonS3 s3client = getS3Client();
		TransferManager xfer_mgr = TransferManagerBuilder
				.standard()
				.withS3Client(s3client)
				.withExecutorFactory(() -> Executors.newFixedThreadPool(10))
				.withMultipartUploadThreshold(5 * 1024 * 1024L)
				.withMinimumUploadPartSize(5 * 1024 * 1024L)
				.build();
		
        String tmpDir = System.getProperty("java.io.tmpdir");
		Path tempPath = Paths.get(tmpDir);
	
		if(!tempPath.toFile().isDirectory()){
			tempPath.toFile().mkdir();
		}
        
		Log.debug(Geonet.SEARCH_ENGINE, "BatchEditAPI, tmpDir for backup xml files --->" + tmpDir);
		
		List<File> files = tempBackupData.stream().map(md -> {
				
				try {
					Path path = Files.createTempFile(tempPath, md.getUuid(), ".xml"); 
					File f = path.toFile();
					f.deleteOnExit();
					FileUtils.writeByteArrayToFile(f, md.getData().getBytes());
					return f; 
				} catch (IOException e) {
					return null;
				}
				
			}).collect(Collectors.toList());
		
		
		//MultipleFileUpload xfer = xfer_mgr.uploadFileList(s3uri.getBucket(), s3key, new File("."), files);
		MultipleFileUpload xfer = xfer_mgr.uploadFileList(s3uri.getBucket(), s3key, tempPath.toFile(), files);
		
		do {
		    try {
		        Thread.sleep(100);
		    } catch (InterruptedException e) {
		        return;
		    }
		    TransferProgress progress = xfer.getProgress();
		    //long so_far = progress.getBytesTransferred();
		    //long total = progress.getTotalBytesToTransfer();
		    pct = progress.getPercentTransferred();
		    progressBackup();
		} while (xfer.isDone() == false);
		// print the final state of the transfer.
		TransferState xfer_state = xfer.getState();
		
		Log.debug(Geonet.SEARCH_ENGINE, ": " + xfer_state);
		
        
        S3Operation op = new S3Operation();
        try {
			List<String> filenames = op.getBucketObjectNames(Geonet.BATCHEDIT_BACKUP_BUCKET + s3key);
			filenames.stream().forEach(fn -> {
				SetObjectAclRequest req = new SetObjectAclRequest(s3uri.getBucket(), fn, CannedAccessControlList.PublicRead);
				s3client.setObjectAcl(req);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        try{
        	files.stream().forEach(f -> {
        		f.delete();	
        	});
        }catch(Exception e){
        	Log.error(Geonet.SEARCH_ENGINE, "Unable to remove tmp xml files created during batch edit");
        }
	}
	
	/**
	 * The service updates records by uploading the csv file
	 */
	@ApiOperation(value = "Get batch edit backup progress.")
	@RequestMapping(value = "/batchediting/progress", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Return a report of what has been done."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT) })
	@PreAuthorize("hasRole('Administrator')")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public Double batchEditBackup(HttpServletRequest request) {
		return progressBackup();
	}
	
    public Double progressBackup()
    {
        Log.debug(Geonet.SEARCH_ENGINE, "Percentage transfer: " + pct);
        return pct;
    }
	
	/**
	 * Add batch update entry into database. Converts CustomReport into JSON and stores as StrigClob 
	 * @param report
	 * @param dateTime
	 * @param settingRepo
	 * @return
	 */
	private boolean addEntry(SimpleMetadataProcessingReport report, String s3key, SettingRepository settingRepo, String desc){
		
		try{
			Type listType = new TypeToken<List<CustomReport>>() {}.getType();
			
			List<CustomReport> target = new LinkedList<CustomReport>();
			CustomReport customReport = new CustomReport();
			customReport.setErrorReport(report.getMetadataErrors());
			customReport.setInfoReport(report.getMetadataInfos());
			customReport.setProcessedRecords(report.getNumberOfRecordsProcessed());
			customReport.setDateTime(s3key);
			customReport.setDesc(desc);
			
			target.add(customReport);
			Setting sett = settingRepo.findOne(Settings.METADATA_BATCHEDIT_HISTORY);
			
			//Add Info and Error report to s3 bucket
			String _report = g.toJson(target, listType);
			byte[] bytes = _report.getBytes();
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(bytes.length);
			InputStream targetStream = new ByteArrayInputStream(bytes);

			PutObjectRequest putObj = new PutObjectRequest(s3uri.getBucket(), s3key + ".json", targetStream, metadata)
					.withCannedAcl(CannedAccessControlList.PublicRead);

			getS3Client().putObject(putObj);
			
			
			//Add description and and filename into DB, removes info and error report in order to minimize the content
			Gson gson = new GsonBuilder()
	                .registerTypeAdapter(CustomReport.class, new CustomReportSerializer())
	                .create();
			
			if(sett == null){//Creates if there is no entity exist 
				sett = new Setting();
				sett.setName(Settings.METADATA_BATCHEDIT_HISTORY);
				sett.setDataType(SettingDataType.JSON);
				sett.setPosition(200199);
				String _rep = gson.toJson(target, listType);
				sett.setValue(_rep);
			}else{//Adds report into existing value.
				List<CustomReport> target2 = gson.fromJson(sett.getValue(), listType);
				target2.add(customReport);
				String _rep = gson.toJson(target2, listType);
				sett.setValue(_rep);
			}
			settingRepo.save(sett);
		}catch (Exception e) {
			Log.error(Geonet.SEARCH_ENGINE, "BatchEditsApi --> Unable to create an entry for this batch edit operation:" + e.getMessage());
			return false;
		}
		
		return true;
	}
	
	public void clearReport(SimpleMetadataProcessingReport report){
		try{
			report.getInfos().clear();
			report.getErrors().clear();
			report.getMetadata().clear();
			report.getMetadataErrors().clear();
			report.getMetadataInfos().clear();
			report.setTotalRecords(0);
			report.processStart();
			report.setNumberOfRecordsProcessed(0);
		}catch(Exception e){
			
		}
		
	}

}

class CustomReport {
	protected String desc;
	protected int processedRecords = 0;
	protected String dateTime;
	protected List<EditErrorReport> errorReport;
	protected List<EditInfoReport> infoReport;
	
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public int getProcessedRecords() {
		return processedRecords;
	}
	public void setProcessedRecords(int processedRecords) {
		this.processedRecords = processedRecords;
	}
	public String getDateTime() {
		return dateTime;
	}
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	
	public List<EditErrorReport> getErrorReport() {
		return errorReport;
	}
	public void setErrorReport(Map<Integer, ArrayList<ErrorReport>> metadataErrors) {
		this.errorReport = new ArrayList<>();
		metadataErrors.entrySet().stream().forEach(e -> {
			EditErrorReport eer = new EditErrorReport();
			eer.setId(e.getKey());
			eer.setMetadataErrors(e.getValue().stream().map(ErrorReport::getMessage).collect(Collectors.toList()));
			this.errorReport.add(eer);
		});
	}
	
	public List<EditInfoReport> getInfoReport() {
		return infoReport;
	}
	public void setInfoReport(Map<Integer, ArrayList<InfoReport>> metadataInfos) {
		this.infoReport = new ArrayList<>();
		metadataInfos.entrySet().stream().forEach(e -> {
			EditInfoReport eir = new EditInfoReport();
			eir.setId(e.getKey());
			eir.setMetadataInfos(e.getValue().stream().map(InfoReport::getMessage).collect(Collectors.toList()));
			this.infoReport.add(eir);
		});
	}
	
	class EditErrorReport {
		private int id;
		private List<String> metadataErrors;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public List<String> getMetadataErrors() {
			return metadataErrors;
		}

		public void setMetadataErrors(List<String> metadataErrors) {
			this.metadataErrors = metadataErrors;
		}

	}
	
	class EditInfoReport {
		private int id;
		private List<String> metadataInfos;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public List<String> getMetadataInfos() {
			return metadataInfos;
		}

		public void setMetadataInfos(List<String> metadataInfos) {
			this.metadataInfos = metadataInfos;
		}

	}
    
}

class CustomReportSerializer implements JsonSerializer<CustomReport> {

    @Override
    public JsonElement serialize(CustomReport report, Type type, JsonSerializationContext jsc) {
        JsonObject jObj = (JsonObject)new GsonBuilder().create().toJsonTree(report);
        jObj.remove("errorReport");
        jObj.remove("infoReport");
        return jObj;
    }
}
