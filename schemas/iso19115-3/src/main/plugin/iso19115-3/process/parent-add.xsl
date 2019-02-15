<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0"
  xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
  xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
  xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
  xmlns:gn="http://www.fao.org/geonetwork"
  exclude-result-prefixes="#all" >
  
  <!-- Parent metadata record UUID -->
  <xsl:param name="parentUuid"/>
  <xsl:param name="parenteCatId"/>
  <xsl:param name="parentTitle"/>
  
  <xsl:template match="/mdb:MD_Metadata|*[contains(@gco:isoType, 'mdb:MD_Metadata')]">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates select="mdb:metadataIdentifier"/>
      <xsl:apply-templates select="mdb:defaultLocale"/>
      
      <!-- Only one parent identifier allowed
        - overwriting existing one. -->
      <xsl:if test="normalize-space($parentUuid) != ''">
        <mdb:parentMetadata>
			<cit:CI_Citation>
				<cit:title>
					<gco:CharacterString><xsl:value-of select="$parentTitle" /></gco:CharacterString>
				</cit:title>
				<cit:identifier>
					<mcc:MD_Identifier>
						<mcc:code>
							<gco:CharacterString><xsl:value-of select="$parentUuid" /></gco:CharacterString>
						</mcc:code>
						<mcc:description>
							<gco:CharacterString>UUID</gco:CharacterString>
						</mcc:description>
					</mcc:MD_Identifier>
				</cit:identifier>
				<cit:identifier>
					<mcc:MD_Identifier>
						<mcc:code>
							<gco:CharacterString><xsl:value-of select="$parenteCatId" /></gco:CharacterString>
						</mcc:code>
						<mcc:description>
							<gco:CharacterString>eCat ID</gco:CharacterString>
						</mcc:description>
					</mcc:MD_Identifier>
				</cit:identifier>
			</cit:CI_Citation>
		</mdb:parentMetadata>
      </xsl:if>
      
      <xsl:apply-templates select="mdb:metadataScope"/>
      <xsl:apply-templates select="mdb:contact"/>
      <xsl:apply-templates select="mdb:dateInfo"/>
      <xsl:apply-templates select="mdb:metadataStandard"/>
      <xsl:apply-templates select="mdb:metadataProfile"/>
      <xsl:apply-templates select="mdb:alternativeMetadataReference"/>
      <xsl:apply-templates select="mdb:otherLocale"/>
      <xsl:apply-templates select="mdb:metadataLinkage"/>
      <xsl:apply-templates select="mdb:spatialRepresentationInfo"/>
      <xsl:apply-templates select="mdb:referenceSystemInfo"/>
      <xsl:apply-templates select="mdb:metadataExtensionInfo"/>
      <xsl:apply-templates select="mdb:identificationInfo"/>
      <xsl:apply-templates select="mdb:contentInfo"/>
      <xsl:apply-templates select="mdb:distributionInfo"/>
      <xsl:apply-templates select="mdb:dataQualityInfo"/>
      <xsl:apply-templates select="mdb:resourceLineage"/>
      <xsl:apply-templates select="mdb:portrayalCatalogueInfo"/>
      <xsl:apply-templates select="mdb:metadataConstraints"/>
      <xsl:apply-templates select="mdb:applicationSchemaInfo"/>
      <xsl:apply-templates select="mdb:metadataMaintenance"/>
      <xsl:apply-templates select="mdb:acquisitionInformation"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- Remove geonet:* elements. -->
  <xsl:template match="gn:*" priority="2"/>
  
  <!-- Copy everything. -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
