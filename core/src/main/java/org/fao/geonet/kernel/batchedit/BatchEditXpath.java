package org.fao.geonet.kernel.batchedit;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jeeves.server.sources.http.ServletPathFinder;

@Configuration
public class BatchEditXpath implements ApplicationContextAware{

	Map<String, XPath> xpathExpr = new HashMap<>();
	Properties prop = new Properties();
	
	private ApplicationContext context;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
		
	}
	
	public Map<String, XPath> getXPathExpr(){
		return xpathExpr;
	}
	
	@Bean
	public Map<String, XPath> loadCustomXpath() throws JDOMException, IOException {
		
		ServletPathFinder finder = new ServletPathFinder(this.context.getBean(ServletContext.class));
        Path appPath = finder.getAppPath();
        
		Path customXPath = appPath.resolve("WEB-INF/data/config/schema_plugins/iso19115-3/csv").resolve("xpath.properties");
		
		InputStream input = new FileInputStream(customXPath.toFile());
		prop.load(input);

		Enumeration<?> e = prop.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = prop.getProperty(key);
			//Log.debug(Geonet.SEARCH_ENGINE, " BatchEditXpath ----> Key : " + key + ", Value : " + value);
			
			xpathExpr.put(key, getXPath(value));
		}
		
		return xpathExpr;
		
	}
	public XPath getXPath(String xpath) throws JDOMException {
		XPath _xpath = XPath.newInstance(xpath);
		return _xpath;
	}
	
}
