package org.fao.geonet.api.records.model.related;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "relatedAssociatedItem", propOrder = { "identifierDesc", "associationType", "protocol" })
public class RelatedAssociatedItem extends RelatedMetadataItem {

	@XmlElement(required = true)
	protected String associationType;
	@XmlElement(required = true)
	protected String identifierDesc;
	@XmlElement(required = true)
	protected String protocol;

	public String getAssociationType() {
		return associationType;
	}

	public void setAssociationType(String associationType) {
		this.associationType = associationType;
	}

	public String getIdentifierDesc() {
		return identifierDesc;
	}

	public void setIdentifierDesc(String identifierDesc) {
		this.identifierDesc = identifierDesc;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

}
