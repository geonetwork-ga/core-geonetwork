package org.fao.geonet.api.records;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.kernel.search.LuceneSearcher;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.search.SearcherType;
import org.fao.geonet.services.util.SearchDefaults;
import org.fao.geonet.utils.Xml;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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

	@ApiOperation(value = "Get records by xpath")
	@RequestMapping(value = "/search/xpath", consumes = { MediaType.APPLICATION_JSON_VALUE }, method = RequestMethod.POST)
	public @ResponseBody List<String> getMetadataRecordsByXpath(@ApiIgnore HttpServletRequest request,
			@RequestBody Map<String, String> allRequestParams) throws Exception {

		
		List<String> eCatIds = new ArrayList<>();
		if(allRequestParams.containsKey("xpath")){
		
			final XPath _xpath = XPath.newInstance(allRequestParams.get("xpath"));
			allRequestParams.remove("xpath");
			if(allRequestParams.containsKey("fast"))
				allRequestParams.remove("fast");
			if(allRequestParams.containsKey("_isTemplate"))
				allRequestParams.replace("_isTemplate", "n");
		
			XPath _eCatIdPath = XPath.newInstance("/mdb:MD_Metadata/mdb:alternativeMetadataReference/*/cit:identifier/*/mcc:code/gco:CharacterString");
			
			if (allRequestParams.get("resultType") == null) {
				allRequestParams.put("resultType", "details");
			}
			
			Element results = query(allRequestParams, request);
	
			@SuppressWarnings("unchecked")
			List<Element> metadata = results.getChildren("MD_Metadata",Geonet.Namespaces.MDB);
			
			metadata.stream().forEach(md -> {
				try {
					Document doc = sb.build(new StringReader(Xml.getString(md)));
					Element ele = (Element) _xpath.selectSingleNode(doc);
					
					if(ele != null){
						Content eCatId = (Content)_eCatIdPath.selectSingleNode(doc);
						eCatIds.add(eCatId.getValue());
					}
				} catch (JDOMException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		
		
		return eCatIds;

	}

	private Element query(Map<String, String> queryFields, HttpServletRequest request) {
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
			
			if(to == null){
				params.addContent(new Element("to").setText(searcher.getSize() + ""));	
			}else{
				params.getChild("to").setText(searcher.getSize() + "");
			}
			
			Element result = searcher.present(context, params, config);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
