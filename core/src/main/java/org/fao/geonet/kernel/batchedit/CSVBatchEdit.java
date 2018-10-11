//==============================================================================
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
//===	Contact: Joseph John,
//===	Canberra - Australia. email: joseph.john@ga.gov.au
//==============================================================================
package org.fao.geonet.kernel.batchedit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.search.IndexAndTaxonomy;
import org.fao.geonet.kernel.search.LuceneConfig;
import org.fao.geonet.kernel.search.LuceneQueryBuilder;
import org.fao.geonet.kernel.search.LuceneQueryInput;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.search.index.LuceneIndexLanguageTracker;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.utils.Log;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import jeeves.server.context.ServiceContext;

/**
 * 
 * @author Joseph John U89263
 * 
 * This class updates the records by processing the data given in csv.
 *
 */
public class CSVBatchEdit implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;
	Map<String, XPath> xpathExpr = new HashMap<>();
	List<BatchEditReport> reports = new ArrayList<>();
	
	LuceneIndexLanguageTracker tracker;
	IndexAndTaxonomy indexAndTaxonomy;
	IndexSearcher searcher;
	LuceneConfig luceneConfig;

	public CSVBatchEdit(ApplicationContext context) {
	
		tracker = context.getBean(LuceneIndexLanguageTracker.class);
		try {
			indexAndTaxonomy = tracker.acquire("eng", -1);
		} catch (IOException e) {
			Log.error(Geonet.SEARCH_ENGINE, "Unable to index and taxonomy, "+e.getMessage());
		}
		searcher = new IndexSearcher(indexAndTaxonomy.indexReader);
		luceneConfig = context.getBean(LuceneConfig.class);
		
	}
	
	/**
	 * Remove/Add elements 
	 * @param context
	 * @param serviceContext
	 * @param header
	 * @param csvr
	 * @param _xpath
	 * @param metadata
	 * @param listOfUpdates
	 * @param mode
	 * @return
	 */
	public BatchEditReport removeOrAddElements(ApplicationContext context, ServiceContext serviceContext, Map.Entry<String, Integer> header, CSVRecord csvr, 
			XPath _xpath, Document metadata, List<BatchEditParam> listOfUpdates, String mode) {

		BatchEditReport report = new BatchEditReport();
		final SchemaManager schemaManager = context.getBean(SchemaManager.class);
		String headerVal = header.getKey();
		EditElement editElement = EditElementFactory.getElementType(headerVal);
		if(editElement == null){
			Path p = schemaManager.getSchemaDir("iso19115-3").resolve("csv").resolve(headerVal + ".xml");
			if(p != null && p.toFile().exists()){
				Log.debug(Geonet.SEARCH_ENGINE, "p.toFile().getAbsolutePath() ---> "+p.toFile().getAbsolutePath());
				editElement = new CustomElement();
			}
		}
			
		if(StringUtils.isNotEmpty(csvr.get(headerVal).trim())){
			if(editElement != null){
				
				if(checkDependencies(headerVal, csvr) && mode.equals("remove")){

					try {
						List<Element> elements = _xpath.selectNodes(metadata);
						if(elements != null && elements.size() > 0){
							Log.debug(Geonet.SEARCH_ENGINE, "elements.size() ---> "+elements.size());
							elements.iterator().forEachRemaining(e -> {
								//e.removeContent();
								e.detach();
							});	
						}
						
					} catch (Exception e) {
						//Log.error(Geonet.SEARCH_ENGINE, "Unable to remove existing element for eCatId/UUID " + id +" for the value " + csvr.get(headerVal) +", " + e.getLocalizedMessage());
						report.getErrorInfo().add("Unable to remove existing element for the value " + csvr.get(headerVal) +", " + e.getMessage());
					}
				}
				
				editElement.removeAndAddElement(this, context, serviceContext, header, csvr, _xpath, listOfUpdates, report);
				
			}else if(_xpath.getXPath().contains("/@")){
				try {
					Attribute attr = (Attribute) _xpath.selectSingleNode(metadata);
					if(attr != null){
						attr.setValue(csvr.get(headerVal));
					}else{
						BatchEditParam e = new BatchEditParam(_xpath.getXPath(), csvr.get(headerVal));
						listOfUpdates.add(e);
					}
				} catch (Exception e) {
					//Log.error(Geonet.SEARCH_ENGINE, "Unable to set the attribute for eCatId/UUID " + id +" for the value " + csvr.get(headerVal) +", " + e.getMessage());
					report.getErrorInfo().add("Unable to set the attribute for the value, " + headerVal + ": "+ csvr.get(headerVal) +", " + e.getMessage());
				}
			} else {
				try {
					Element element = (Element) _xpath.selectSingleNode(metadata);
					if(element != null){
						element.setText(csvr.get(headerVal));
					}
					else{
						BatchEditParam e = new BatchEditParam(_xpath.getXPath(),
								"<gn_add>" + csvr.get(headerVal) + "</gn_add>");
						listOfUpdates.add(e);
					}
				} catch (Exception e) {
					//Log.error(Geonet.SEARCH_ENGINE, "Unable to set text for eCatId/UUID " + id +" for the value " + csvr.get(headerVal) +", " + e.getLocalizedMessage());
					report.getErrorInfo().add("Unable to set text for the value, " + headerVal + ": "+ csvr.get(headerVal) +", " + e.getMessage());
				}	
			}
		}
		
		return report;
	}

	
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
	
	/**
	 * If one of the element is not defined in CSV, it won't remove both element. It only adds the element.
	 * 
	 * @param header
	 * @param csvr
	 * @return
	 */
	private boolean checkDependencies(String header, CSVRecord csvr){
		
		if(Geonet.EditType.GEOBOX.equals(header)){
			if(!csvr.isMapped(Geonet.EditType.VERTICAL)){
				return false;
			}
		}
		
		if(Geonet.EditType.VERTICAL.equals(header)){
			if(!csvr.isMapped(Geonet.EditType.GEOBOX)){
				return false;
			}
		}
		
		return true;
	}


	/**
	 * Search metadata using lucene index
	 * @param context
	 * @param srvContext
	 * @param request
	 * @return
	 * @throws BatchEditException
	 */
	public Metadata getMetadataByLuceneSearch(ApplicationContext context, ServiceContext srvContext, Element request) throws BatchEditException {

		LuceneQueryInput luceneQueryInput = new LuceneQueryInput(request);
		
		Query _query = new LuceneQueryBuilder(luceneConfig, luceneConfig.getTokenizedField(),
				SearchManager.getAnalyzer(Geonet.DEFAULT_LANGUAGE, true), Geonet.DEFAULT_LANGUAGE)
						.build(luceneQueryInput);

		MetadataRepository mdRepo = context.getBean(MetadataRepository.class);

		try {
			
			TopDocs tdocs = searcher.search(_query, 1);
			DocumentStoredFieldVisitor docVisitor = new DocumentStoredFieldVisitor("_uuid");

			indexAndTaxonomy.indexReader.document(tdocs.scoreDocs[0].doc, docVisitor);
			org.apache.lucene.document.Document doc = docVisitor.getDocument();

			String uuid = doc.get("_uuid");
			Log.debug(Geonet.SEARCH_ENGINE, "getMetadataByLuceneSearch --> uuid: " + uuid);

			if (uuid != null) {
				Metadata md = mdRepo.findOneByUuid(uuid);
				return md;
			}

		} catch (Exception e) {
			throw new BatchEditException("failed to get this record using lucene search");
		}

		return null;
	}
	
	/**
	 * Retrieves CRS reference system
	 * @param crsId
	 * @return
	 */
	public Crs getById(String crsId) {
        for (Object object : ReferencingFactoryFinder
            .getCRSAuthorityFactories(null)) {
            CRSAuthorityFactory factory = (CRSAuthorityFactory) object;

            try {
                Set<String> codes = factory
                    .getAuthorityCodes(CoordinateReferenceSystem.class);
                for (Object codeObj : codes) {
                    String code = (String) codeObj;
                    if (code.equals(crsId)) {
                        String authorityTitle = (factory.getAuthority()
                            .getTitle() == null ? "" : factory
                            .getAuthority().getTitle().toString());
                        String authorityEdition = (factory.getAuthority()
                            .getEdition() == null ? "" : factory
                            .getAuthority().getEdition().toString());

                        String authorityCodeSpace = "";
                        Collection<? extends Identifier> ids = factory
                            .getAuthority().getIdentifiers();
                        for (Identifier id : ids) {
                            authorityCodeSpace = id.getCode();
                        }

                        String description;
                        try {
                            description = factory.getDescriptionText(code)
                                .toString();
                        } catch (Exception e1) {
                            description = "-";
                        }
                        description += " (" + authorityCodeSpace + ":" + code
                            + ")";

                        return new Crs(code, authorityTitle,
                            authorityEdition, authorityCodeSpace,
                            description);
                    }
                }
            } catch (FactoryException e) {
            }
        }
        return null;
    }
	
	public String toTitleCase(String clval){
		String[] vals = clval.split(" ");
		StringBuilder titleCase = new StringBuilder();
		if(vals.length > 1){
			titleCase.append(vals[0].toLowerCase());
			for(int i = 1; i < vals.length; i++){
				titleCase.append(WordUtils.capitalize(vals[i]));
			}
		}
		return titleCase.toString();
	}
}

class Crs {
    private String code;

    ;
    private String authority;
    private String version;
    private String codeSpace;
    private String description;
    public Crs() {
    }

    public Crs(String code, String authority,
               String version, String codeSpace,
               String description) {
        this.code = code;
        this.authority = authority;
        this.version = version;
        this.codeSpace = codeSpace;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCodeSpace() {
        return codeSpace;
    }

    public void setCodeSpace(String codeSpace) {
        this.codeSpace = codeSpace;
    }

}