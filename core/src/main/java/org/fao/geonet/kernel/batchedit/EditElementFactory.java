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

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.utils.Log;

/**
 * 
 * @author Joseph John - U89263
 *
 */
public class EditElementFactory {

	public static EditElement getElementType(String type) {
		
		switch (type) {
		case Geonet.EditType.KEYWORD_THESAURUS:
			return new KeywordEditElement();
		case Geonet.EditType.RES_CONTACT:
		case Geonet.EditType.MD_CONTACT:
		case Geonet.EditType.RESPONSIBLE_PARTY:
			return new ContactEditElement();
		case Geonet.EditType.DATA_STORAGE_LINK:
		case Geonet.EditType.ASSOCIATED_RES:
		case Geonet.EditType.ADDITIONAL_INFO:
		case Geonet.EditType.DISTRIBUTION_LINK:
			return new OnlineResourceEditElement();
		case Geonet.EditType.CITATION_DATE:
			return new DateEditElement();
		case Geonet.EditType.MD_PARENT:
			return new MetadataEditElement();
		case Geonet.EditType.RES_LEGAL_CONSTRAINT:
			return new ConstraintsEditElement();
		default:
			return null;
		}
	
	}

}
