package org.fao.geonet.guiservices.status;

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.lib.Lib;
import org.jdom.Element;

import jeeves.constants.Jeeves;
import jeeves.interfaces.Service;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;

public class List implements Service
{
	public void init(String appPath, ServiceConfig params) throws Exception {}

	//--------------------------------------------------------------------------
	//---
	//--- Service
	//---
	//--------------------------------------------------------------------------

	public Element exec(Element params, ServiceContext context) throws Exception
	{
		Dbms dbms = (Dbms) context.getResourceManager().open (Geonet.Res.MAIN_DB);
		
		return Lib.local.retrieve(dbms, "StatusValues").setName(Jeeves.Elem.RESPONSE);
	}
}
