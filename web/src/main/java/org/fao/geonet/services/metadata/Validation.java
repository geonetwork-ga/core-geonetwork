package org.fao.geonet.services.metadata;

import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.services.NotInReadOnlyModeService;
import org.jdom.Element;

import jeeves.constants.Jeeves;
import jeeves.exceptions.BadParameterEx;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;

public class Validation extends NotInReadOnlyModeService{
		//--------------------------------------------------------------------------
		//---
		//--- Init
		//---
		//--------------------------------------------------------------------------
		public void init(String appPath, ServiceConfig params) throws Exception
	    {
	    }

		//--------------------------------------------------------------------------
		//---
		//--- Service
		//---
		//--------------------------------------------------------------------------
		public Element serviceSpecificExec(Element params, ServiceContext context) throws Exception
		{
			GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
			DataManager dataMan = gc.getDataManager();

			String schema = dataMan.autodetectSchema(params);
			
	        if (schema == null)
	        	throw new BadParameterEx("Can't detect schema for metadata automatically.", schema);

	        DataManager.validateMetadata(schema, params, context);
			
			//--- update element and return status
			Element elResp = new Element(Jeeves.Elem.RESPONSE);
			elResp.addContent(new Element("valid").setText("y"));
			elResp.addContent(new Element("schema").setText(schema));

			return elResp;
		}
}
