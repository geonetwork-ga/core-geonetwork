package org.fao.geonet.kernel.batchedit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.fao.geonet.kernel.SchemaManager;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;

import jeeves.server.context.ServiceContext;

public class CustomElement implements EditElement {

	@Override
	public void removeAndAddElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext serContext,
			Entry<String, Integer> header, CSVRecord csvr, XPath _xpath, List<BatchEditParam> listOfUpdates,
			BatchEditReport report) {

		String headerVal = header.getKey();
		
		
		
		final SchemaManager schemaManager = context.getBean(SchemaManager.class);
		
		Path p = schemaManager.getSchemaDir("iso19115-3").resolve("csv").resolve(headerVal + ".xml");
		
		if(p.toFile().exists()){
			try{
				
				String val = csvr.get(headerVal).replaceAll("&(?!amp;)", "&amp;");
				String[] contents = val.split(content_separator);
				
				String xml = new String(Files.readAllBytes(p));
				
				for (String content : contents) {
					
					String _xml = xml;
					
					String[] values = content.split(type_separator);
					String[] searchList = new String[values.length]; 
					for (int i = 0; i < values.length; i++) {
						searchList[i] = "{" + i + "}";
					}

					_xml = StringUtils.replaceEach(_xml, searchList, values);
					
					String _val = "<gn_add>" + _xml + "</gn_add>";
					BatchEditParam e = new BatchEditParam(_xpath.getXPath(), _val);
					listOfUpdates.add(e);
				}
				
				
			}catch(Exception e){
				report.getErrorInfo().add("Unable to process : " + headerVal + ", exception: " +e.getMessage());
			}
		}
		
		

	}

}
