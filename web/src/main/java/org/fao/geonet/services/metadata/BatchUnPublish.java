//=============================================================================
//===	Copyright (C) 2001-2007 Geoscience Australia
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
//===	Contact: Joseph John
//===	Canberra - Australia. email: joseph.john@ga.gov.au
//==============================================================================
package org.fao.geonet.services.metadata;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Params;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.MdInfo;
import org.fao.geonet.kernel.SelectionManager;
import org.fao.geonet.services.NotInReadOnlyModeService;
import org.fao.geonet.util.ISODate;
import org.jdom.Element;

import jeeves.constants.Jeeves;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;

/**
 * Batch unpublish all selected metadata.
 */
public class BatchUnPublish extends NotInReadOnlyModeService {

	//--------------------------------------------------------------------------
		//---
		//--- Init
		//---
		//--------------------------------------------------------------------------

		public void init(String appPath, ServiceConfig params) throws Exception {
	        super.init(appPath, params);
	    }

		//--------------------------------------------------------------------------
		//---
		//--- Service
		//---
		//--------------------------------------------------------------------------

		public Element serviceSpecificExec(Element params, ServiceContext context) throws Exception
		{
			GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);

			DataManager dm = gc.getDataManager();
			AccessManager accessMan = gc.getAccessManager();
			UserSession us = context.getUserSession();

			Dbms dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);

			context.info("Get selected metadata");
			SelectionManager sm = SelectionManager.getManager(us);

			Set<Integer> metadata = new HashSet<Integer>();
			Set<Integer> notFound = new HashSet<Integer>();
			Set<Integer> notOwner = new HashSet<Integer>();

			synchronized(sm.getSelection("metadata")) {
				for (Iterator<String> iter = sm.getSelection("metadata").iterator(); iter.hasNext();) {
					String uuid = (String) iter.next();
					String id   = dm.getMetadataId(dbms, uuid);
										
					//--- check access
	
					MdInfo info = dm.getMetadataInfo(dbms, id);
					if (info == null) {
						notFound.add(Integer.valueOf(id));
					} else if (!accessMan.isOwner(context, id)) {
						notOwner.add(Integer.valueOf(id));
					} else {
						updatePriviledge(context, info, dbms, id, dm, us);
						dm.deleteAllMetadataCateg(dbms, id);
						metadata.add(Integer.valueOf(id));
					}
				}
			}

			dbms.commit();

			updateStatus(context, gc, dbms, metadata);
			
			//--- reindex metadata
			context.info("Re-indexing metadata");
			BatchOpsMetadataReindexer r = new BatchOpsMetadataReindexer(dm, dbms, metadata);
			r.process();
			// -- for the moment just return the sizes - we could return the ids
			// -- at a later stage for some sort of result display
			return new Element(Jeeves.Elem.RESPONSE)
							.addContent(new Element("done")    .setText(metadata.size()+""))
							.addContent(new Element("notOwner").setText(notOwner.size()+""))
							.addContent(new Element("notFound").setText(notFound.size()+""));
		}
		

		private void updatePriviledge(ServiceContext context, MdInfo info, Dbms dbms, String id, DataManager dm, UserSession us) throws Exception{
			//--- remove old operations
			boolean skip = false;

			//--- in case of owner, privileges for groups 0,1 and GUEST are 
			//--- disabled and are not sent to the server. So we cannot remove them
			boolean isAdmin = Geonet.Profile.ADMINISTRATOR.equals(us.getProfile());
			boolean isReviewer= Geonet.Profile.REVIEWER.equals(us.getProfile());

			if (us.getUserId().equals(info.owner) && !isAdmin && !isReviewer)
				skip = true;

			dm.deleteMetadataOper(dbms, id, skip);
		}

		private void updateStatus(ServiceContext context,GeonetContext gc, Dbms dbms, Set<Integer> metadata) throws Exception{
			String changeDate = new ISODate().toString();

			//--- use StatusActionsFactory and StatusActions class to 
		    //--- change status and carry out behaviours for status changes
		    StatusActionsFactory saf = new StatusActionsFactory(gc.getStatusActionsClass());
	
		    StatusActions sa = saf.createStatusActions(context, dbms);
	
		    Set<Integer> noChange = saf.statusChange(sa, Params.Status.DRAFT, metadata, changeDate, "");
		}

}
