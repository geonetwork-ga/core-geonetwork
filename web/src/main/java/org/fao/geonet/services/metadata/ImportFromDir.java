//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.services.metadata;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jeeves.exceptions.XSDValidationErrorEx;
import jeeves.guiservices.session.JeevesUser;
import jeeves.interfaces.Service;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;
import jeeves.utils.Util;
import jeeves.utils.Xml;

import org.apache.commons.io.FileUtils;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Geonet.Profile;
import org.fao.geonet.constants.Params;
import org.fao.geonet.exceptions.SchematronValidationErrorEx;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.search.spatial.Pair;
import org.fao.geonet.kernel.MetadataIndexerProcessor;
import org.fao.geonet.kernel.mef.MEFLib;
import org.fao.geonet.services.NotInReadOnlyModeService;
import static org.fao.geonet.services.metadata.Insert.applyImportStylesheet;
import org.fao.geonet.util.ThreadUtils;
import org.jdom.Element;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

// FIXME: this class could be moved to DataManager


/**
 * Import all metadata found inside a given directory.
 */
public class ImportFromDir extends NotInReadOnlyModeService{
	private FilenameFilter filter = new FilenameFilter()
	{
		public boolean accept(File dir, String name)
		{
			if (name.equals(CONFIG_FILE))
				return false;

			return !name.startsWith(".");
		}
	};

	//--------------------------------------------------------------------------

	/**
	 * Filter xml or mef files.
	 */
	private FilenameFilter mdFilter = new FilenameFilter()
	{
		public boolean accept(File dir, String name)
		{
			if (name.equals(CONFIG_FILE))
				return false;

			if (name.startsWith("."))
				return false;

			if (name.toLowerCase().endsWith(".xml") || name.toLowerCase().endsWith(".mef"))
				return true;
			else
				return false;
		}
	};

	//--------------------------------------------------------------------------

	private String stylePath;
    private List<Pair<Exception,String>> exceptions = new ArrayList<Pair<Exception,String>>();
    private boolean failOnError;
	private static final String CONFIG_FILE = "import-config.xml";
	
	//Joseph Added
	private String bucketName = "";
	private ListObjectsV2Result v2Result;
	private ListObjectsV2Request v2Request;
	private boolean isDirectory = true;
	private int fileLength = 0;
	//--------------------------------------------------------------------------
	//---
	//--- Init
	//---
	//--------------------------------------------------------------------------

	public void init(String appPath, ServiceConfig params) throws Exception
	{
		this.stylePath = appPath + Geonet.Path.IMPORT_STYLESHEETS;
	}

	//--------------------------------------------------------------------------
	//---
	//--- API
	//---
	//--------------------------------------------------------------------------

	/**
	 * util.import service allow to import metadata in batch mode using 2 
	 * different mode:
	 * <ul>
	 * <li>Search for all xml file in given directory and load them</li>
	 * <li>Search for a file named import-config.xml in given directory
	 * and the import as described in that file.</li>
	 * </ul>
	 * 
	 * Fail on error parameter is available for directory batch mode. If true, 
	 * import stop on first error.
	 * 
	 * Return the number of record inserted and the list of exceptions.
	 */
	public Element serviceSpecificExec(Element params, ServiceContext context) throws Exception
	{
		String dir  = Util.getParam(params, Params.DIR);
		failOnError = Util.getParam(params, Params.FAIL_ON_ERROR, "off").equals("on");
		File   file = new File(dir +"/"+ CONFIG_FILE);

		//-----------------------------------------------------------------------

		long start = System.currentTimeMillis();

		int result;

		if (file.exists())	result = configImport(params, context, file);
			else 					result = standardImport(params, context);

		long end = System.currentTimeMillis();
		long duration = (end - start) / 1000;

		context.info("Import time is :" + duration + " secs");

		Element response = new Element("response")
			.addContent(new Element("records").setText(""+result))
			.addContent(new Element("time").setText(""+duration));
		
		if (exceptions.size() > 0) {
			Element ex = new Element("exceptions").setAttribute("count", ""+exceptions.size());
			for (Pair<Exception,String> ep : exceptions) {
								Element singleEx = new Element("exception");
								singleEx.setAttribute("filename", ep.two());
								Exception e = ep.one();
                if (e instanceof SchematronValidationErrorEx) {
                    ex.addContent(singleEx.addContent((Element) ((SchematronValidationErrorEx) e).getObject()));

                } else if (e instanceof XSDValidationErrorEx) {
                        ex.addContent(singleEx.addContent((Element) ((XSDValidationErrorEx) e).getObject()));

                } else {
                    ex.addContent(singleEx.setText(e.getMessage()));

                }
			}
			response.addContent(ex);
		}
		
		return response;
	}

	//--------------------------------------------------------------------------
	//---
	//--- ImportMetadataReindexer class (used in standardImport)
	//---
	//--------------------------------------------------------------------------

	public class ImportCallable implements Callable<Pair<List<String>,List<Pair<Exception,String>>>> {
		private final File files[];
		private final AmazonS3 s3Client;
		private final int beginIndex, count;
		private final Element params;
		private final String stylePath;
		private final boolean failOnError;
		private final ServiceContext context;
		private final String userId;
		private final String userName;
		private final String userProfile;
		private final Dbms dbms;

		ImportCallable(File files[], AmazonS3 s3Client, int beginIndex, int count, Element params, ServiceContext context, String stylePath, boolean failOnError, Dbms dbms) {
			this.files = files;
			this.s3Client = s3Client;
			this.beginIndex = beginIndex;
			this.count = count;
			this.params = params;
			this.context = context;
			this.stylePath = stylePath;
			this.failOnError = failOnError;
			this.userId = this.context.getUserSession().getUserId();
			this.userName = this.context.getUserSession().getUsername();
			this.userProfile = this.context.getUserSession().getProfile();
			this.dbms = dbms;
		}
		
		private void login() {
		    JeevesUser user = new JeevesUser(this.context.getProfileManager());
		    user.setId(this.userId);
		    user.setUsername(this.userName);
		    user.setProfile(this.userProfile);
		    UserSession session = new UserSession();
		    session.loginAs(user);
		    this.context.setUserSession(session);
		}
		
		public Pair<List<String>,List<Pair<Exception,String>>> call() throws Exception {
			List<String> ids = new ArrayList<String>();
			List<Pair<Exception,String>> exceptions = new ArrayList<Pair<Exception,String>>();
			
			login();
			String filePath = "";
			for(int i=beginIndex; i<beginIndex+count; i++) {
				try {
					File f;
					if(!isDirectory){//Import AWS files 
						
						S3ObjectSummary objectSummary = v2Result.getObjectSummaries().get(i);
						filePath = s3Client.getUrl(bucketName, objectSummary.getKey()).toString();
						S3ObjectInputStream content = s3Client.getObject(bucketName, objectSummary.getKey())
								.getObjectContent();
						
						f = new File("temp/" + objectSummary.getKey());
						FileUtils.copyInputStreamToFile(content, f);
					}else{//Import files from a given directory
						f = files[i];
						filePath = files[i].getPath();
					}
					
					List<String> nIds = MEFLib.doImport(params, context, f, stylePath, dbms);
					ids.addAll(nIds);
					
				} catch (Exception e) {
					if (failOnError) {
						throw e;
					}
					exceptions.add(Pair.read(e, filePath));
				}
			}
			return Pair.read(ids,exceptions);
		}
	}

	
	public class ImportMetadata {
		Element params;
		File files[];
		AmazonS3 s3Client;
		String stylePath;
		ServiceContext context;
		List<String> ids = new ArrayList<String>();
		List<Pair<Exception,String>> exceptions = new ArrayList<Pair<Exception,String>>();


		public ImportMetadata(Element params, ServiceContext context, File files[], String stylePath, boolean failOnError) {
			this.params = params;
			this.context = context;
			this.files = files;
			this.stylePath = stylePath;
		}

		//Joseph - added
		public ImportMetadata(Element params, ServiceContext context, AmazonS3 s3Client, String stylePath, boolean failOnError) {
			this.params = params;
			this.context = context;
			this.s3Client = s3Client;
			this.stylePath = stylePath;
		}
		
		public void process() throws Exception {
			int threadCount = ThreadUtils.getNumberOfThreads();

			ExecutorService executor = Executors.newFixedThreadPool(threadCount);

			int perThread;
			if (files.length < threadCount) perThread = files.length;
			else perThread = files.length / threadCount;
			int index = 0;

			// batch import is transactional - open up one dbms channel for each thread
			// - we abort the dbms channels if we get an exception otherwise commit
			List<Dbms> dbmsList = new ArrayList<Dbms>();
			try {
				List<Future<Pair<List<String>,List<Pair<Exception,String>>>>> sList = new ArrayList<Future<Pair<List<String>,List<Pair<Exception,String>>>>>();
				while(index < files.length) {
					int start = index;
					int count = Math.min(perThread,files.length-start);
					// create threads to process this chunk of files
					Dbms dbms = (Dbms) context.getResourceManager().openDirect(Geonet.Res.MAIN_DB);
					dbmsList.add(dbms);
					
					//Joseph Added
					Callable<Pair<List<String>,List<Pair<Exception,String>>>> worker = new ImportCallable(files, s3Client, start, count, params, context, stylePath, failOnError, dbms);
					
					//Joseph Commented
					//Callable<Pair<List<String>,List<Pair<Exception,String>>>> worker = new ImportCallable(files, start, count, params, context, stylePath, failOnError, dbms);
					
					Future<Pair<List<String>,List<Pair<Exception,String>>>> submit = executor.submit(worker);
					sList.add(submit);
					index += count;
				}
	
				for (Future<Pair<List<String>,List<Pair<Exception,String>>>> future : sList) {
					try {
						ids.addAll(future.get().one());
						exceptions.addAll(future.get().two());
					} catch (InterruptedException e) {
						exceptions.add(Pair.read((Exception)e,"unknown"));  // don't know the file name here
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
						exceptions.add(Pair.read((Exception)e,"unknown"));  // don't know the file name here
					}
				}
		
				for (Dbms dbms : dbmsList) {
					if (exceptions.size() > 0 && failOnError) dbms.abort();
					else dbms.commit();
				}
			} finally {
				for (Dbms dbms : dbmsList) {
					context.getResourceManager().close(Geonet.Res.MAIN_DB, dbms);
				}
			}

			executor.shutdown();
		}
		
		//-----------------------------------------------------------
		//
		// Joseph Added this function to Process Files from S3 Bucket
		//
		//-----------------------------------------------------------
		public void processS3Files() throws Exception {
			int threadCount = ThreadUtils.getNumberOfThreads();
			
			ExecutorService executor = Executors.newFixedThreadPool(threadCount);

			// batch import is transactional - open up one dbms channel for each thread
			// - we abort the dbms channels if we get an exception otherwise commit
			List<Dbms> dbmsList = new ArrayList<Dbms>();
			try {
				List<Future<Pair<List<String>,List<Pair<Exception,String>>>>> sList = new ArrayList<Future<Pair<List<String>,List<Pair<Exception,String>>>>>();
					
				do {//get object keys in a bucket if more than 1000 (default max-key)
					v2Result = s3Client.listObjectsV2(v2Request);
					
					int resultSize = v2Result.getObjectSummaries().size();
					int perThread;
					if (resultSize < threadCount) 
						perThread = resultSize;
					else 
						perThread = resultSize / threadCount;
					
					int index = 0;
					
					fileLength = fileLength + resultSize;
					while (index < resultSize) {
						int start = index;
						int count = Math.min(perThread, resultSize - start);
						// create threads to process this chunk of files
						Dbms dbms = (Dbms) context.getResourceManager().openDirect(Geonet.Res.MAIN_DB);
						dbmsList.add(dbms);
						
						Callable<Pair<List<String>, List<Pair<Exception, String>>>> worker = new ImportCallable(
								files, s3Client, start, count, params, context, stylePath, failOnError, dbms);

						Future<Pair<List<String>, List<Pair<Exception, String>>>> submit = executor.submit(worker);
						sList.add(submit);
						index += count;
					}

					v2Request.setContinuationToken(v2Result.getNextContinuationToken());
				} while (v2Result.isTruncated() == true);
				
	
				for (Future<Pair<List<String>,List<Pair<Exception,String>>>> future : sList) {
					try {
						ids.addAll(future.get().one());
						exceptions.addAll(future.get().two());
					} catch (InterruptedException e) {
						exceptions.add(Pair.read((Exception)e,"unknown"));  // don't know the file name here
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
						exceptions.add(Pair.read((Exception)e,"unknown"));  // don't know the file name here
					}
				}
		
				for (Dbms dbms : dbmsList) {
					if (exceptions.size() > 0 && failOnError) dbms.abort();
					else dbms.commit();
				}
			} finally {
				for (Dbms dbms : dbmsList) {
					context.getResourceManager().close(Geonet.Res.MAIN_DB, dbms);
				}
			}

			executor.shutdown();
		}

		public List<String> getIds() {
			return ids;
		}

		public List<Pair<Exception,String>> getExceptions() {
			return exceptions;
		}
	}

	//--------------------------------------------------------------------------
	//---
	//--- Standard import
	//---
	//--------------------------------------------------------------------------

	private int standardImport(Element params, ServiceContext context) throws Exception
	{
		
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		DataManager   dm = gc.getDataManager();

		String dir      = Util.getParam(params, Params.DIR);
		
		List<String> ids = null;
		ImportMetadata r = null;
		if(dir.startsWith("https")){//Joseph Added - To import files from AWS
			isDirectory = false;
			try{
				AmazonS3 s3 = getAmazonS3(dir);
				r = new ImportMetadata(params, context, s3, stylePath, failOnError);
				r.processS3Files();
			}catch (AmazonServiceException ase) {
				throw new Exception("Request made to Amazon S3, but was rejected " + ase.getRawResponseContent());
			} catch (AmazonClientException ace) {
				throw new Exception("Encountered an internal error while trying to communicate with S3 Bucket");
			} catch (Exception e) {
				throw new Exception("Exception while accessing AWS S3 Bucket");
			}
		}else{
			isDirectory = true;
			File files[] = new File(dir).listFiles(mdFilter);
			
			if (files == null)
				throw new Exception("Directory not found: " + dir);
			
			fileLength = files.length;
			r = new ImportMetadata(params, context, files, stylePath, failOnError);
			r.process();
		}

		
		ids = r.getIds();
		exceptions = r.getExceptions();
	
		context.info("Now reindexing "+ids.size());

		dm.batchRebuild(context, ids);
		return fileLength;
	}

	/**
	 * This method creates AmazonS3 client  
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private AmazonS3 getAmazonS3(String url) throws IOException, IllegalArgumentException{
		
		AmazonS3URI s3uri = new AmazonS3URI(url);
		bucketName = s3uri.getBucket();
		AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(s3uri.getRegion()).build();
		v2Request = new ListObjectsV2Request().withBucketName(bucketName);
		
		return s3client;
	}
	
	//--------------------------------------------------------------------------
	//---
	//--- Config import
	//---
	//--------------------------------------------------------------------------

	private int configImport(Element params, ServiceContext context, File configFile) throws Exception
	{
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		DataManager   dm = gc.getDataManager();

		ImportConfig config = new ImportConfig(configFile, context);

		String dir   = Util.getParam(params, Params.DIR);
		String group = Util.getParam(params, Params.GROUP);
		String style = Util.getParam(params, Params.STYLESHEET);

		boolean validate = Util.getParam(params, Params.VALIDATE, "off").equals("on");

		File sites[] = new File(dir).listFiles(filter);

		if (sites == null)
			throw new Exception("Directory not found : " + dir);

		int counter = 0;

		ArrayList<ImportInfo> alImport = new ArrayList<ImportInfo>();

		for(int i=0; i<sites.length; i++)
		{
            if(context.isDebug())
                context.debug("Scanning site : "+sites[i]);

			File categs[] = sites[i].listFiles(filter);

			if (categs == null)
				throw new Exception("Cannot scan categories in : " + sites[i].getPath());

			for(int j=0; j<categs.length; j++)
			{
                if(context.isDebug())
                    context.debug("   Scanning category : "+categs[j]);

				String catDir  = categs[j].getName();
				File   files[] = categs[j].listFiles(mdFilter);

				if (files == null)
					throw new Exception("Cannot scan files in : " + categs[j].getPath());

				for(int k=0; k<files.length; k++)
				{
					Element xml = Xml.loadFile(files[k]);

					xml = applyImportStylesheet(xml, style, gc, stylePath);

					String category = config.mapCategory(catDir);
					String schema   = config.mapSchema(catDir);

					if (validate)
						dm.validate(schema, xml);

					alImport.add(new ImportInfo(schema, category, xml));
					counter++;
				}
			}
		}

		//--- step 2 : insert metadata

		List<String> ids = new ArrayList<String>();
		for (ImportInfo ii : alImport) {
			String mId = insert(ii.schema, ii.xml, group, context, ii.category);
			ids.add(mId);
		}

		//--- step 3 : index inserted metadata

		dm.batchRebuild(context, ids);

		return counter;
	}

	//--------------------------------------------------------------------------
	//---
	//--- Private methods
	//---
	//--------------------------------------------------------------------------

	private String insert(String schema, Element xml, String group, ServiceContext context,
							  String category) throws Exception
	{
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		DataManager   dm = gc.getDataManager();

		//-----------------------------------------------------------------------
		//--- if the uuid does not exist we generate it

		String uuid = dm.extractUUID(schema, xml);

		if (uuid.length() == 0)
			uuid = UUID.randomUUID().toString();

		if (category.equals("_none_")) category = null;
		//-----------------------------------------------------------------------
		//--- insert metadata into the system

		Dbms dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);

        String docType = null, title = null, createDate = null, changeDate = null;
        boolean ufo = true, indexImmediate = true;
        String isTemplate = "n";
				int mId = context.getSerialFactory().getSerial(dbms, "Metadata");
        dm.insertMetadata(context, dbms, schema, xml, mId, uuid, context.getUserSession().getUserIdAsInt(), group, gc.getSiteId(),
                         isTemplate, docType, title, category, createDate, changeDate, ufo, indexImmediate);

		return mId+"";

	}

}

//=============================================================================

class ImportInfo
{
	public String  schema;
	public String  category;
	public Element xml;

	//--------------------------------------------------------------------------

	public ImportInfo(String schema, String category, Element xml)
	{
		this.schema   = schema;
		this.category = category;
		this.xml      = xml;
	}
}

//=============================================================================

class ImportConfig
{
	private static final String CATEGORY_MAPPING = "categoryMapping";
	private static final String SCHEMA_MAPPING   = "schemaMapping";

	private static final String MAPPING = "mapping";
	private static final String DEFAULT = "default";

	private static final String ATTR_DIR = "dir";
	private static final String ATTR_TO  = "to";

	//--------------------------------------------------------------------------

	private Hashtable htCategId       = new Hashtable();
	private Hashtable htCategMapping  = new Hashtable();
	private Hashtable htSchemaMapping = new Hashtable();

	private String defaultCateg;
	private String defaultSchema;

	//--------------------------------------------------------------------------
	//---
	//--- Constructor
	//---
	//--------------------------------------------------------------------------

	public ImportConfig(File configFile, ServiceContext context) throws Exception
	{
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		DataManager   dm = gc.getDataManager();

		Dbms dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);

		Element config = Xml.loadFile(configFile);

		fillCategIds(dbms);

		mapCategor(config.getChild(CATEGORY_MAPPING));
		mapSchemas(config.getChild(SCHEMA_MAPPING), dm);
	}

	//--------------------------------------------------------------------------
	//---
	//--- API methods
	//---
	//--------------------------------------------------------------------------

	public String mapCategory(String catDir)
	{
		String mapping = (String) htCategMapping.get(catDir);

		if (mapping == null)
			mapping = defaultCateg;

		return mapping;
	}

	//--------------------------------------------------------------------------

	public String mapSchema(String catDir)
	{
		String mapping = (String) htSchemaMapping.get(catDir);

		if (mapping == null)
			mapping = defaultSchema;

		return mapping;
	}

	//--------------------------------------------------------------------------
	//---
	//--- Private methods
	//---
	//--------------------------------------------------------------------------

	private void fillCategIds(Dbms dbms) throws Exception
	{
		String query = "SELECT * FROM Categories";

		List idsList = dbms.select(query).getChildren();

		for (int i=0; i<idsList.size(); i++)
		{
			Element record = (Element) idsList.get(i);

			String id   = record.getChildText("id");
			String name = record.getChildText("name");

			htCategId.put(name, id);
		}
	}

	//--------------------------------------------------------------------------

	private void mapCategor(Element categMapping)
	{
		List list = categMapping.getChildren(MAPPING);

		for(int i=0; i<list.size(); i++)
		{
			Element el = (Element) list.get(i);

			String dir = el.getAttributeValue(ATTR_DIR);
			String to  = el.getAttributeValue(ATTR_TO);

			String categId = (String) htCategId.get(to);

			if (categId == null)
				throw new IllegalArgumentException("Category not found : "+ to);

			htCategMapping.put(dir, categId);
		}

		String defaultTo = categMapping.getChild(DEFAULT).getAttributeValue(ATTR_TO);
		String defaultId = (String) htCategId.get(defaultTo);

		if (defaultId == null)
			throw new IllegalArgumentException("Default category not found : "+ defaultTo);

		defaultCateg = defaultTo;
	}

	//--------------------------------------------------------------------------

	private void mapSchemas(Element schemaMapping, DataManager dm)
	{
		List list = schemaMapping.getChildren(MAPPING);

		for(int i=0; i<list.size(); i++)
		{
			Element el = (Element) list.get(i);

			String dir = el.getAttributeValue(ATTR_DIR);
			String to  = el.getAttributeValue(ATTR_TO);

			if (!dm.existsSchema(to))
				throw new IllegalArgumentException("Schema not found : "+ to);

			htSchemaMapping.put(dir, to);
		}

		String defaultTo = schemaMapping.getChild(DEFAULT).getAttributeValue(ATTR_TO);

		if (!dm.existsSchema(defaultTo))
			throw new IllegalArgumentException("Default schema not found : "+ defaultTo);

		defaultSchema = defaultTo;
	}	
}
