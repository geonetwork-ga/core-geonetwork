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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.kernel.Thesaurus;
import org.fao.geonet.kernel.ThesaurusManager;
import org.fao.geonet.utils.Log;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;

import jeeves.server.context.ServiceContext;

/**
 * This class creates simple keywords by providing a text and codelist values and also create keyword based on thesaurus.
 *  
 * @author Joseph John - U89263
 *
 */
public class KeywordEditElement implements EditElement {

	XMLOutputter out = new XMLOutputter();

	@Override
	public void removeAndAddElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext serContext,
			Entry<String, Integer> header, CSVRecord csvr, XPath _xpath, List<BatchEditParam> listOfUpdates,
			BatchEditReport report) {

		String headerVal = header.getKey();

		//If more keyword exist, it must be separated by ###
		String[] keywords = csvr.get(headerVal).split(content_separator);

		for (String keyword : keywords) {

			Element rootE = null;

			try {
				
				if (headerVal.equalsIgnoreCase(Geonet.EditType.KEYWORD_THESAURUS))
					rootE = getKeywordElementWithThesaurus(keyword, context, serContext);

			} catch (BatchEditException e) {
				report.getErrorInfo().add(e.getMessage());
			}

			if (rootE != null) {
				String strEle = out.outputString(rootE);

				String _val = "<gn_add>" + strEle + "</gn_add>";
				BatchEditParam e = new BatchEditParam(_xpath.getXPath(), _val);
				listOfUpdates.add(e);
			}
		}

	}


	/**
	 * Creates keyword element based on thesaurus. keyword should be provided in the format title~keyword1,keyword2,..,keywordn
	 * Keywords must match exactly that defined in given title. 
	 * @param title_keyword
	 * @param context
	 * @param serContext
	 * @return
	 * @throws BatchEditException
	 */
	private Element getKeywordElementWithThesaurus(String title_keyword, ApplicationContext context,
			ServiceContext serContext) throws BatchEditException {

		try {
			String[] values = title_keyword.split(type_separator);
			ThesaurusManager thesaurusMan = context.getBean(ThesaurusManager.class);
			Thesaurus thes = null;

			if (values.length > 0) {

				Log.debug(Geonet.SEARCH_ENGINE, "CSVBatchEdit, KeywordEditElement --> title: " + values[0]);
				
				Collection<Thesaurus> thesColl = thesaurusMan.getThesauriMap().values();

				//Reduce the Collection of thesaurus  
				thes = thesColl.stream().filter(t -> t.getTitle().equalsIgnoreCase(values[0].trim())).findFirst().get();

				if (thes != null && values.length > 1) {

					//Element descK = new Element("descriptiveKeywords", Geonet.Namespaces.MRI);
					Element mdK = new Element("MD_Keywords", Geonet.Namespaces.MRI);

					String[] keywords = values[1].split(",");

					for (String keyword : keywords) {
						Log.debug(Geonet.SEARCH_ENGINE, "CSVBatchEdit, KeywordEditElement --> keyword -> " + keyword);
						Element k = new Element("keyword", Geonet.Namespaces.MRI);
						Element ch = new Element("CharacterString", Geonet.Namespaces.GCO_3).setText(keyword);
						k.addContent(ch);
						mdK.addContent(k);
					}

					Element type = new Element("type", Geonet.Namespaces.MRI);
					Element cl = new Element("MD_KeywordTypeCode", Geonet.Namespaces.MRI);
					cl.setAttribute("codeList", "codeListLocation#MD_KeywordTypeCode");
					cl.setAttribute("codeListValue", thes.getDname());
					type.addContent(cl);
					//descK.addContent(mdK.addContent(Arrays.asList(type, getThesaurus(thes))));
					mdK.addContent(Arrays.asList(type, getThesaurus(thes)));
					return mdK;
				} else {
					Log.debug(Geonet.SEARCH_ENGINE, "CSVBatchEdit, KeywordEditElement --> ThesaurusByName is null");
				}

			}
		} catch (Exception e) {
			throw new BatchEditException("Unable to process Keyword Element with Thesaurus having keyword "
					+ title_keyword + " - " + e.getMessage());
		}
		return null;

	}

	/**
	 * Creates Thesaurus element
	 * @param the
	 * @return
	 * @throws BatchEditException
	 */
	private Element getThesaurus(Thesaurus the) throws BatchEditException {

		try {
			Element theName = new Element("thesaurusName", Geonet.Namespaces.MRI);
			Element citation = new Element("CI_Citation", Geonet.Namespaces.CIT);
			Element title = new Element("title", Geonet.Namespaces.CIT);
			Element date = new Element("date", Geonet.Namespaces.CIT);
			Element ciDate = new Element("CI_Date", Geonet.Namespaces.CIT);
			Element date1 = new Element("date", Geonet.Namespaces.CIT);
			Element dateType = new Element("dateType", Geonet.Namespaces.CIT);
			Element identifier = new Element("identifier", Geonet.Namespaces.CIT);
			Element mdIdentifier = new Element("MD_Identifier", Geonet.Namespaces.MCC);
			Element code = new Element("code", Geonet.Namespaces.MCC);
			Element anchor = new Element("Anchor", Geonet.Namespaces.GCX);

			title.addContent(new Element("CharacterString", Geonet.Namespaces.GCO_3).setText(the.getTitle()));

			if (the.getDate().contains("T")) {
				date1.addContent(new Element("DateTime", Geonet.Namespaces.GCO_3).setText(the.getDate()));
			} else {
				date1.addContent(new Element("Date", Geonet.Namespaces.GCO_3).setText(the.getDate()));
			}

			dateType.addContent(new Element("CI_DateTypeCode", Geonet.Namespaces.CIT)
					.setAttribute("codeList", "codeListLocation#CI_DateTypeCode")
					.setAttribute("codeListValue", "publication"));
			date.addContent(ciDate.addContent(Arrays.asList(date1, dateType)));

			Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
			anchor.addNamespaceDeclaration(xlink);
			anchor.setAttribute("href", the.getDownloadUrl(), xlink).setText("geonetwork.thesaurus." + the.getKey());

			identifier.addContent(mdIdentifier.addContent(code.addContent(anchor)));

			theName.addContent(citation.addContent(Arrays.asList(title, date, identifier)));

			return theName;
		} catch (Exception e) {
			throw new BatchEditException("Exception while creating Thesaurus element..");
		}
	}

}
