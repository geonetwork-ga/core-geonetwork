/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

package org.fao.geonet.api.records;

import io.swagger.annotations.*;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiParams;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.api.records.model.related.RelatedItem;
import org.fao.geonet.api.records.model.related.RelatedItemType;
import org.fao.geonet.api.tools.i18n.LanguageUtils;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.TransformManager;
import org.fao.geonet.kernel.metadata.StatusActions;
import org.fao.geonet.kernel.metadata.StatusActionsFactory;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import jeeves.server.context.ServiceContext;
import jeeves.services.ReadWriteController;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_OPS;
import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;
import static org.fao.geonet.api.ApiParams.API_PARAM_RECORD_UUID;

@RequestMapping(value = {
    "/api/records",
    "/api/" + API.VERSION_0_1 +
        "/records"
})
@Api(value = API_CLASS_RECORD_TAG,
    tags = API_CLASS_RECORD_TAG,
    description = API_CLASS_RECORD_OPS)
@Controller("recordWorkflow")
@ReadWriteController
public class MetadataWorkflowApi {

    @Autowired
    LanguageUtils languageUtils;


    @ApiOperation(
        value = "Set record status",
        notes = "",
        nickname = "status")
    @RequestMapping(value = "/{metadataUuid}/status",
        method = RequestMethod.PUT
    )
    @PreAuthorize("hasRole('Editor')")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Status updated."),
        @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status(
        @ApiParam(
            value = API_PARAM_RECORD_UUID,
            required = true)
        @PathVariable
            String metadataUuid,
        @ApiParam(
            value = "status",
            required = true
        )
        // TODO: RequestBody could be more appropriate ?
        @RequestParam(
            required = true
        )
            Integer status,
        @ApiParam(
            value = "coment",
            required = true
        )
        @RequestParam(
            required = true
        )
            String comment,
        HttpServletRequest request
    )
        throws Exception {
        Metadata metadata = ApiUtils.canEditRecord(metadataUuid, request);
        ApplicationContext appContext = ApplicationContextHolder.get();
        Locale locale = languageUtils.parseAcceptLanguage(request.getLocales());
        ServiceContext context = ApiUtils.createServiceContext(request, locale.getISO3Language());
        
        AccessManager am = appContext.getBean(AccessManager.class);
        //--- only allow the owner of the record to set its status
        if (!am.isOwner(context, String.valueOf(metadata.getId()))) {
            throw new SecurityException(String.format(
                "Only the owner of the metadata can set the status. User is not the owner of the metadata"
            ));
        }

        ISODate changeDate = new ISODate();

        //--- use StatusActionsFactory and StatusActions class to
        //--- change status and carry out behaviours for status changes
        StatusActionsFactory saf = appContext.getBean(StatusActionsFactory.class);

        StatusActions sa = saf.createStatusActions(context);

        Set<Integer> metadataIds = new HashSet<Integer>();
        metadataIds.add(metadata.getId());

        sa.statusChange(String.valueOf(status), metadataIds, changeDate, comment);

        TransformManager transMan = appContext.getBean(TransformManager.class);
        SchemaManager schemaManager = appContext.getBean(SchemaManager.class);
        DataManager dataManager = appContext.getBean(DataManager.class);
        
        //Joseph added - Remove service record (associated resource) from dataset, if service record is Retired(3)/Rejected(5) 
        if(status == 3 || status == 5){
        	
        	String schema = dataManager.getMetadataSchema(String.valueOf(metadata.getId()));
        	Element md = dataManager.getMetadata(String.valueOf(metadata.getId()));
        	String publishDate = new ISODate().toString();
        	
        	
        	if(status == 3){//Joseph added - To update Keyword with Retired_Internal, if status Retired(3) - Start
        		md = transMan.
                		updatePublishKeyWord(md, "//mri:descriptiveKeywords/mri:MD_Keywords/mri:keyword[gco:CharacterString = '{}']", 
                				Geonet.Transform.PUBLISH_KEYWORDS, "{}", Geonet.Transform.RETIRED_INTERNAL, false);
                if(md != null){
                	dataManager.updateMetadata(context, String.valueOf(metadata.getId()), md, false, false, false, context.getLanguage(), new ISODate().toString(), false);
                }else{
                	md = dataManager.getMetadata(String.valueOf(metadata.getId()));
            		Map<String, Object> xslParameters = new HashMap<String, Object>();
            		xslParameters.put("publish_keyword", Geonet.Transform.RETIRED_INTERNAL);
            		Path file = schemaManager.getSchemaDir(schema).resolve("process").resolve(Geonet.File.SET_KEYWORD);
            		md = Xml.transform(md, file, xslParameters);
            		dataManager.updateMetadata(context, String.valueOf(metadata.getId()), md, false, false, false, context.getLanguage(), publishDate, false);
                }
                //Joseph added - To update Keyword with Retired_Internal, if status Retired(3) - End
        	}
            
            //Grab all operatesOn uuidref, if record is service
            RelatedItemType[] type = {RelatedItemType.datasets};
            Element services = MetadataUtils.getRelated(context, metadata.getId(), metadata.getUuid(), type, 1, 100, true);
            XPath xpath = XPath.newInstance(".//uuid");
            try {
                @SuppressWarnings("unchecked")
                List<Element> matches = xpath.selectNodes(services);
                for(Element uuid : matches){
                	Metadata dtmetadata = ApiUtils.canEditRecord(uuid.getValue(), request);
                	Element datasetMd = dataManager.getMetadata(String.valueOf(dtmetadata.getId()));
                	Map<String, Object> xslParameters = new HashMap<String, Object>();
            		xslParameters.put("code", metadata.getUuid());
            		xslParameters.put("type", "UUID");
            		Path file = schemaManager.getSchemaDir(schema).resolve("process").resolve("association-remove.xsl");
            		datasetMd = Xml.transform(datasetMd, file, xslParameters);
            		dataManager.updateMetadata(context, String.valueOf(dtmetadata.getId()), datasetMd, false, false, false, context.getLanguage(), publishDate, false);
            		dataManager.indexMetadata(String.valueOf(dtmetadata.getId()), true);
                }
                
            } catch (Exception e) {
                Log.error(Geonet.SEARCH_ENGINE, ": failed on element using XPath '" + xpath +
                        " Exception: " + e.getMessage());
            }
        }
        
        //--- reindex metadata
        dataManager.indexMetadata(String.valueOf(metadata.getId()), true);
    }
}
