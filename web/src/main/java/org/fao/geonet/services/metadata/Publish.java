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
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Params;
import org.fao.geonet.exceptions.MetadataNotFoundEx;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.MdInfo;
import org.fao.geonet.services.NotInReadOnlyModeService;
import org.fao.geonet.services.Utils;
import org.fao.geonet.util.ISODate;
import org.jdom.Element;

import jeeves.constants.Jeeves;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;

/**
 * Publishes a selected metadata.
 */
public class Publish extends NotInReadOnlyModeService {

	/**
    *
    * @param appPath
    * @param params
    * @throws Exception
    */
	public void init(String appPath, ServiceConfig params) throws Exception {}
	
	/**
    *
    * @param params
    * @param context
    * @return
    * @throws Exception
    */
	public Element serviceSpecificExec(Element params, ServiceContext context) throws Exception {
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);

		DataManager dm = gc.getDataManager();
		Dbms dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);
		UserSession   us = context.getUserSession();
		String id = Utils.getIdentifierFromParameters(params, context);

		//--- check access
		int iLocalId = Integer.parseInt(id);
		if (!dm.existsMetadata(dbms, iLocalId))
			throw new IllegalArgumentException("Metadata not found --> " + id);
		
		MdInfo info = dm.getMetadataInfo(dbms, id);
		
		if (info == null)
			throw new MetadataNotFoundEx(id);
		
		dm.updateMetadataOwner(dbms, Integer.parseInt(id), us.getUserId());
		updatePriviledge(context, info, dbms, id, dm, us);
		updateCategory(params, context, dbms, id, dm);
		updateStatus(context, gc, dbms, iLocalId);
		
		//--- index metadata
		dm.indexInThreadPool(context,id, dbms);
		
		//--- return id for showing
		return new Element(Jeeves.Elem.RESPONSE).addContent(new Element(Geonet.Elem.ID).setText(id));
				
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

		//--- set new ones

		String[] privValues = {"_1_0", "_1_1", "_1_5", "_1_6"};
		for(String name : privValues) {
			
			if (name.startsWith("_")) {
				StringTokenizer st = new StringTokenizer(name, "_");

				String groupId = st.nextToken();
				String operId  = st.nextToken();
				dm.setOperation(context, dbms, id, groupId, operId);
			}
		}
	}
	
	private void updateCategory(Element params, ServiceContext context, Dbms dbms, String id, DataManager dm) throws Exception{
		//--- remove old operations
		dm.deleteAllMetadataCateg(dbms, id);

		//--- set new ones
		List list = params.getChildren();

		for(int i=0; i<list.size(); i++) {
			Element el = (Element) list.get(i);
			String name = el.getName();

			if (name.startsWith("_"))
				dm.setCategory(context, dbms, id, name.substring(1));
		}
	}
	

	private void updateStatus(ServiceContext context,GeonetContext gc, Dbms dbms, int iLocalId) throws Exception{
		String changeDate = new ISODate().toString();
		Set<Integer> metadataIds = new HashSet<Integer>();
		metadataIds.add(iLocalId);
		//--- use StatusActionsFactory and StatusActions class to 
	    //--- change status and carry out behaviours for status changes
	    StatusActionsFactory saf = new StatusActionsFactory(gc.getStatusActionsClass());

	    StatusActions sa = saf.createStatusActions(context, dbms);

	    Set<Integer> noChange = saf.statusChange(sa, Params.Status.APPROVED, metadataIds, changeDate, "");
	}
}
