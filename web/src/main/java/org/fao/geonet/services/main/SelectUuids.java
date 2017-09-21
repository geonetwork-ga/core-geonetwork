package org.fao.geonet.services.main;

import java.util.Iterator;

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Params;
import org.fao.geonet.kernel.SelectionManager;
import org.jdom.Element;

import jeeves.constants.Jeeves;
import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;
import jeeves.utils.Util;
/**
 * Select a list of UUIDs stored in session
 * Returns append string of UUIDs
 */

public class SelectUuids implements Service{
String init_type;
	
	public void init(String appPath, ServiceConfig params) throws Exception {
		init_type = params.getValue(Params.TYPE);
	}

	// --------------------------------------------------------------------------
	// ---
	// --- Service
	// ---
	// --------------------------------------------------------------------------

	public Element exec(Element params, ServiceContext context)
			throws Exception {
		
		// Get the selection manager
		UserSession session = context.getUserSession();
		SelectionManager sm = SelectionManager.getManager(session) ;
		String uuids= "";
		if (sm != null) {
			boolean first = true;
			synchronized(sm.getSelection("metadata")) {
				for (Iterator<String> iter = sm.getSelection("metadata").iterator(); iter.hasNext();) {
					String uuid = (String) iter.next();
					if (first) {
						uuids = (String) uuid;
						first = false;
					}
					else 
						uuids = uuids +" or "+ uuid;
				}
			}
            
		} 
		// send ok
		Element response = new Element(Jeeves.Elem.RESPONSE);
		response.addContent(new Element("uuids").setText(uuids));		

		return response;
	}
}
