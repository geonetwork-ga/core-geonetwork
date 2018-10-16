package org.fao.geonet.api.records;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.utils.Xml;
import org.fao.geonet.kernel.search.LuceneSearcher;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.search.SearcherType;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.services.util.SearchDefaults;
import org.fao.geonet.utils.Log;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jdom.Document;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import springfox.documentation.annotations.ApiIgnore;

@RequestMapping(value = { "/api/records", "/api/" + API.VERSION_0_1 + "/records" })
@Api(value = API_CLASS_RECORD_TAG, tags = API_CLASS_RECORD_TAG)
@Controller("recordSearch")
public class MetadataSearchApi {

	SAXBuilder sb = new SAXBuilder();
	private List<String> eCatIds = new ArrayList<>();
	private boolean isSearching = false;

	@ApiOperation(value = "Get records by xpath")
	@RequestMapping(value = "/search/xpath", consumes = {
			MediaType.APPLICATION_JSON_VALUE }, method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public void getMetadataRecordsByXpath(@ApiIgnore HttpServletRequest request,
			@RequestBody Map<String, String> allRequestParams) throws Exception {

		isSearching = true;
		ServiceContext context = ApiUtils.createServiceContext(request);
		
		final MetadataRepository metadataRepository = context.getBean(MetadataRepository.class);
		

		if (allRequestParams.containsKey("xpath")) {

			XPath _xpath;
			try {
				_xpath = XPath.newInstance(allRequestParams.get("xpath"));

				allRequestParams.remove("xpath");
				if (allRequestParams.containsKey("_isTemplate"))
					allRequestParams.replace("_isTemplate", "n");

				XPath _eCatIdPath = XPath.newInstance(
						"/mdb:MD_Metadata/mdb:alternativeMetadataReference/*/cit:identifier/*/mcc:code/gco:CharacterString");

				if (allRequestParams.get("resultType") == null) {
					allRequestParams.put("resultType", "details");
				}

				List<String> uuids = query(allRequestParams, request);

				Runnable task = () -> {
					try {
						uuids.stream().forEach(uuid -> {
							try {
								
								Metadata metadata = metadataRepository.findOneByUuid(uuid);
								
								//Document doc = sb.build(new StringReader());
								Element ele = (Element) _xpath.selectSingleNode(new Document(metadata.getXmlData(false)));

								if (ele != null) {
									
									Content eCatId = (Content) _eCatIdPath.selectSingleNode(ele);
									eCatIds.add(eCatId.getValue());
								}
							} catch (JDOMException e) {
								Log.error(Geonet.SEARCH_ENGINE, "JDOMException: " + e.getMessage());
							} catch (IOException e) {
								Log.error(Geonet.SEARCH_ENGINE, "IOException: " + e.getMessage());
							}
						});

					} catch (Exception e) {
					} finally {
						isSearching = false;
					}
				};
				// start the thread
				new Thread(task).start();

			} catch (JDOMException e1) {
				Log.error(Geonet.SEARCH_ENGINE, "Unable to search by XPath, exception: " + e1.getMessage());
			}
		}

		// return eCatIds;

	}

	@ApiOperation(value = "Get records by xpath")
	@RequestMapping(value = "/search/status", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public boolean getMetadataSearchStatus(@ApiIgnore HttpServletRequest request) throws Exception {
		return isSearching;
	}

	@ApiOperation(value = "Get records by xpath")
	@RequestMapping(value = "/search/xpath", method = RequestMethod.GET)
	public @ResponseBody List<String> getMetadataRecords(@ApiIgnore HttpServletRequest request) throws Exception {
		return eCatIds;
	}

	private List<String> query(Map<String, String> queryFields, HttpServletRequest request) throws JDOMException {
		
		XPath _uuid = XPath.newInstance(
				"/response/metadata/geonet:info/uuid");
		_uuid.addNamespace(Geonet.Namespaces.GEONET);
		
		ApplicationContext applicationContext = ApplicationContextHolder.get();
		SearchManager searchMan = applicationContext.getBean(SearchManager.class);
		ServiceContext context = ApiUtils.createServiceContext(request);
		Element params = new Element("params");
		queryFields.forEach((k, v) -> params.addContent(new Element(k).setText(v)));

		Element elData = SearchDefaults.getDefaultSearch(context, params);

		LuceneSearcher searcher = null;

		try {
			searcher = (LuceneSearcher) searchMan.newSearcher(SearcherType.LUCENE, Geonet.File.SEARCH_LUCENE);

			ServiceConfig config = new ServiceConfig();
			searcher.search(context, elData, config);

			Element to = params.getChild("to");

			if (to == null) {
				params.addContent(new Element("to").setText(searcher.getSize() + ""));
			} else {
				params.getChild("to").setText(searcher.getSize() + "");
			}

			Element result = searcher.present(context, params, config);
			
			@SuppressWarnings("unchecked")
			List<Element> coll = (List<Element>)_uuid.selectNodes(new Document(result));
			List<String> uuids = coll.stream().map(Content::getValue).collect(Collectors.toList());
			
			return uuids;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
