<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:srv="http://standards.iso.org/iso/19115/-3/srv/2.0"
                xmlns:mds="http://standards.iso.org/iso/19115/-3/mds/1.0"
                xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
                xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
                xmlns:mrs="http://standards.iso.org/iso/19115/-3/mrs/1.0"
                xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0"
                xmlns:mco="http://standards.iso.org/iso/19115/-3/mco/1.0"
                xmlns:msr="http://standards.iso.org/iso/19115/-3/msr/1.0"
                xmlns:lan="http://standards.iso.org/iso/19115/-3/lan/1.0"
                xmlns:gcx="http://standards.iso.org/iso/19115/-3/gcx/1.0"
                xmlns:mdq="http://standards.iso.org/iso/19157/-2/mdq/1.0"
                xmlns:gex="http://standards.iso.org/iso/19115/-3/gex/1.0"
                xmlns:dqm="http://standards.iso.org/iso/19157/-2/dqm/1.0"
                xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
                xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0"
                xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
				xmlns:mmi="http://standards.iso.org/iso/19115/-3/mmi/1.0"
				xmlns:mrl="http://standards.iso.org/iso/19115/-3/mrl/1.0"
				xmlns:gml="http://www.opengis.net/gml/3.2"
                xmlns:gn="http://www.fao.org/geonetwork"
                xmlns:gn-fn-core="http://geonetwork-opensource.org/xsl/functions/core"
                xmlns:gn-fn-iso19115-3="http://geonetwork-opensource.org/xsl/functions/profiles/iso19115-3"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="#all" version="2.0">

  <xsl:import href="utility-fn.xsl"/>
  <xsl:import href="utility-tpl.xsl"/>

  <xsl:template mode="csv" match="mdb:MD_Metadata|*[@gco:isoType='mdb:MD_Metadata']"
                priority="2">
    <metadata>
      <xsl:variable name="langId" select="gn-fn-iso19115-3:getLangId(., $lang)"/>
      
	  <eCatId>
         <xsl:value-of select="mdb:alternativeMetadataReference/*/cit:identifier/*/mcc:code"/>
	  </eCatId>
      
	  <Title>
        <xsl:apply-templates mode="localised"
                             select="mdb:identificationInfo/*/mri:citation/*/cit:title">
          <xsl:with-param name="langId" select="$langId"/>
        </xsl:apply-templates>
      </Title>
      
	  <Abstract>
        <xsl:apply-templates mode="localised" select="mdb:identificationInfo/*/mri:abstract">
          <xsl:with-param name="langId" select="$langId"/>
        </xsl:apply-templates>
      </Abstract>

	  <!-- <Category>
        <xsl:value-of select="mdb:metadataScope/*/mdb:resourceScope/*/@codeListValue"/>
      </Category> -->
	  
	  <MetadataScope>
	    <xsl:value-of select="mdb:metadataScope/*/mdb:name"/>~<xsl:value-of select="mdb:metadataScope/*/mdb:resourceScope/*/@codeListValue"/>
      </MetadataScope>
	  
	  <ParentMetadata>
        <xsl:value-of select="mdb:parentMetadata/cit:CI_Citation/cit:identifier[last()]/mcc:MD_Identifier/mcc:code/gcx:FileName/text()"/>
      </ParentMetadata>
	  
	  	      
      <xsl:for-each select="mdb:identificationInfo/*/mri:citation/cit:CI_Citation/cit:date/cit:CI_Date">
        <xsl:element name="CitationDate">
          <xsl:value-of select="cit:date/*/text()"/>~<xsl:value-of select="cit:dateType/*/@codeListValue"/>
        </xsl:element>
      </xsl:for-each>
	  
	  <Purpose>
        <xsl:value-of select="mdb:identificationInfo/*/mri:purpose/gco:CharacterString"/>
      </Purpose>
	  
	  <Status>
        <xsl:value-of select="mdb:identificationInfo/*/mri:status/mcc:MD_ProgressCode/@codeListValue"/>
      </Status>
	  
      <xsl:for-each select="mdb:identificationInfo/*/mri:graphicOverview/*/mcc:fileName">
        <image>
          <xsl:value-of select="*/text()"/>
        </image>
      </xsl:for-each>

	  <xsl:for-each select="mdb:identificationInfo/*/mri:descriptiveKeywords/*[not(mri:thesaurusName)]">
			<Keyword>
				<xsl:value-of select="mri:keyword/gco:CharacterString" />~<xsl:value-of select="mri:type/mri:MD_KeywordTypeCode/@codeListValue" />
			</Keyword>
		</xsl:for-each>
	  
	  <xsl:for-each
			select="mdb:identificationInfo/*/mri:descriptiveKeywords/*[mri:thesaurusName]">
			<Keyword-Thesaurus>
				<xsl:value-of select="mri:thesaurusName/*/cit:title/gco:CharacterString" />~
				<xsl:for-each select="mri:keyword">
					<xsl:value-of select="gco:CharacterString" />
					<xsl:if test="position() != last()">
						<xsl:value-of select="','"/>
					  </xsl:if>
				</xsl:for-each>
			</Keyword-Thesaurus>
		</xsl:for-each>
	  
	  <TopicCategory>
                <xsl:value-of select="mdb:identificationInfo/*/mri:topicCategory/mri:MD_TopicCategoryCode"/>
	  </TopicCategory>
	  
	 <MaintenanceFrequency>
                <xsl:value-of select="mdb:identificationInfo/*/mri:resourceMaintenance/*/mmi:MD_MaintenanceFrequencyCode/@codeListValue"/>
	  </MaintenanceFrequency>

	  <xsl:for-each select="mdb:identificationInfo/*/mri:citation/cit:CI_Citation/cit:citedResponsibleParty/cit:CI_Responsibility">
        <ResponsibleParty>
			<xsl:value-of select="cit:party/*/cit:name/*/text()"/>~<xsl:value-of select="cit:role/cit:CI_RoleCode/@codeListValue"/>
        </ResponsibleParty>
      </xsl:for-each>
	  
	  
      <!-- One column per contact role -->
	  <xsl:for-each select="mdb:identificationInfo/*/mri:pointOfContact">
        <xsl:element name="ResourceContact">
          <xsl:apply-templates mode="localised" select="*/cit:party/*/cit:name">
            <xsl:with-param name="langId" select="$langId"/>
          </xsl:apply-templates>~
          <xsl:apply-templates mode="localised" select="*/cit:role/*/@codeListValue">
            <xsl:with-param name="langId" select="$langId"/>
          </xsl:apply-templates>
        </xsl:element>
      </xsl:for-each>
	  
	  <xsl:for-each select="mdb:contact">
        <xsl:element name="MetadataContact">
          <xsl:apply-templates mode="localised" select="*/cit:party/*/cit:name">
            <xsl:with-param name="langId" select="$langId"/>
          </xsl:apply-templates>~
          <xsl:apply-templates mode="localised" select="*/cit:role/*/@codeListValue">
            <xsl:with-param name="langId" select="$langId"/>
          </xsl:apply-templates>
        </xsl:element>
      </xsl:for-each>

	  
      <xsl:for-each select="mdb:identificationInfo/*//gex:EX_GeographicBoundingBox">
        <GeographicalExtent>
            <xsl:value-of select="gex:westBoundLongitude"/>~
            <xsl:value-of select="gex:eastBoundLongitude"/>~
            <xsl:value-of select="gex:southBoundLatitude"/>~
            <xsl:value-of select="gex:northBoundLatitude"/>
        </GeographicalExtent>
      </xsl:for-each>
	  
	  <SpatialExtentDescription>
                <xsl:value-of select="mdb:identificationInfo/*/mri:extent/*/gex:description/gco:CharacterString"/>
	  </SpatialExtentDescription>
	  
	  <HorizontalSpatialReferenceSystem>
                <xsl:value-of select="mdb:referenceSystemInfo/*/mrs:referenceSystemIdentifier/mcc:MD_Identifier/mcc:code/gco:CharacterString"/>
	  </HorizontalSpatialReferenceSystem>
	  
	  <VerticalExtent>
		<xsl:if test="mdb:identificationInfo/*/mri:extent/*/gex:verticalElement">
            <xsl:value-of select="mdb:identificationInfo/*/mri:extent/*/gex:verticalElement/*/gex:minimumValue/gco:Real"/>~<xsl:value-of select="mdb:identificationInfo/*/mri:extent/*/gex:verticalElement/*/gex:maximumValue/gco:Real"/>
		</xsl:if>
	  </VerticalExtent>
	  
	   <VerticalCRS>
		        <xsl:value-of select="mdb:identificationInfo/*/mri:extent/gex:EX_Extent/gex:verticalElement/*/gex:verticalCRSId/*/mrs:referenceSystemIdentifier/mcc:MD_Identifier/mcc:code/gco:CharacterString"/>
	  </VerticalCRS>
	  
	  
	 <TemporalExtent>
		<xsl:if test="mdb:identificationInfo/*/mri:extent/*/gex:temporalElement">
                <xsl:value-of select="mdb:identificationInfo/*/mri:extent/*/gex:temporalElement/*/gex:extent/gml:TimePeriod/gml:beginPosition"/>~<xsl:value-of select="mdb:identificationInfo/*/mri:extent/*/gex:temporalElement/*/gex:extent/gml:TimePeriod/gml:endPosition"/>
		</xsl:if>
	 </TemporalExtent>
	  
	  
     <xsl:for-each select="mdb:metadataConstraints/*">
        <MetadataConstraints>
          <xsl:copy-of select="."/>
        </MetadataConstraints>
      </xsl:for-each>

      <xsl:for-each select="mdb:identificationInfo/*/*/mco:MD_SecurityConstraints">
        <SecurityConstraints>
          <xsl:value-of select="*/mco:classification/*/@codeListValue"/>
        </SecurityConstraints>
      </xsl:for-each>

      <xsl:for-each select="mdb:identificationInfo/*/mri:resourceConstraints/mco:MD_LegalConstraints">
        <ResourceLegalConstraints>
			<xsl:value-of select="mco:reference/cit:CI_Citation/cit:title/*/text()"/>~<xsl:value-of select="mco:accessConstraints/*/@codeListValue"/>~<xsl:value-of select="mco:useConstraints/*/@codeListValue"/>~<xsl:value-of select="mco:otherConstraints/*/text()"/>
        </ResourceLegalConstraints>
      </xsl:for-each>
	  
	  <UseLimitations>
                <xsl:value-of select="mdb:identificationInfo/*/mri:resourceConstraints/mco:MD_LegalConstraints/mco:useLimitation/gco:CharacterString"/>
	  </UseLimitations>

      <xsl:for-each select="mdb:distributionInfo/*/mrd:transferOptions/*/mrd:onLine/*">
        <DistributionLink>
			<xsl:value-of select="cit:name/*/text()"/>~<xsl:value-of select="cit:description/*/text()"/>~<xsl:value-of select="cit:linkage/*/text()"/>
        </DistributionLink>
      </xsl:for-each>
	  
	  <xsl:for-each select="mdb:distributionInfo/*/mrd:distributionFormat">
        <DistributionFormat>
			<xsl:value-of select="*/mrd:formatSpecificationCitation/*/cit:title/*/text()"/>~<xsl:value-of select="*/mrd:formatSpecificationCitation/*/cit:edition/*/text()"/>
        </DistributionFormat>
      </xsl:for-each>
	  
	  <xsl:for-each select="mdb:identificationInfo/*/mri:citation/cit:CI_Citation/cit:onlineResource/cit:CI_OnlineResource">
        <DataStorageLink>
          <xsl:value-of select="cit:name/*/text()"/>~<xsl:value-of select="cit:description/*/text()"/>~<xsl:value-of select="cit:linkage/*/text()"/> 
        </DataStorageLink>
      </xsl:for-each>
      
	  <xsl:for-each select="mdb:identificationInfo/*/mri:resourceFormat">
        <DataStorageFormat>
			<xsl:value-of select="*/mrd:formatSpecificationCitation/*/cit:title/*/text()"/>~<xsl:value-of select="*/mrd:formatSpecificationCitation/*/cit:edition/*/text()"/>
        </DataStorageFormat>
      </xsl:for-each>
 
	<Lineage>
		<xsl:value-of
			select="mdb:resourceLineage/mrl:LI_Lineage/mrl:statement/gco:CharacterString" />
	</Lineage>

	<SourceDescription>
		<xsl:value-of
			select="mdb:resourceLineage/mrl:LI_Lineage/mrl:source/mrl:LI_Source/mrl:description/gco:CharacterString" />
	</SourceDescription>

	  
<!--	   <AssociatedResourcesCode>
                <xsl:value-of select="mdb:identificationInfo/*/mri:associatedResource/mri:MD_AssociatedResource/mri:associationType/mri:DS_AssociationTypeCode/@codeListValue"/>
	  </AssociatedResourcesCode>
	  
	   <AssociatedResourcesTitle>
                <xsl:value-of select="mdb:identificationInfo/mri:MD_DataIdentification/mri:associatedResource/mri:MD_AssociatedResource/mri:metadataReference/cit:CI_Citation/cit:title/gco:CharacterString"/>
	  </AssociatedResourcesTitle>
	  
	   <AssociatedResourcesIdentifier>
                <xsl:value-of select="mdb:identificationInfo/mri:MD_DataIdentification/mri:associatedResource/mri:MD_AssociatedResource/mri:metadataReference/cit:CI_Citation/cit:identifier/mcc:MD_Identifier/mcc:code/gco:CharacterString"/>
	  </AssociatedResourcesIdentifier>
	  
	   <AssociatedResourcesIdentifierDescription>
                <xsl:value-of select="mdb:identificationInfo/mri:MD_DataIdentification/mri:associatedResource/mri:MD_AssociatedResource/mri:metadataReference/cit:CI_Citation/cit:identifier/mcc:MD_Identifier/mcc:description/gco:CharacterString"/>
	  </AssociatedResourcesIdentifierDescription>
	-->  
	  <xsl:for-each select="mdb:identificationInfo/*/mri:associatedResource/*/mri:metadataReference/cit:CI_Citation/cit:onlineResource/cit:CI_OnlineResource">
        <AssociatedResourcesLink>
          <xsl:value-of select="cit:name/*/text()"/>~<xsl:value-of select="cit:description/*/text()"/>~<xsl:value-of select="cit:linkage/*/text()"/> 
        </AssociatedResourcesLink>
      </xsl:for-each>
	
	 <xsl:for-each select="mdb:identificationInfo/*/mri:additionalDocumentation/cit:CI_Citation">
        <AdditionalInformationLink>
          <xsl:value-of select="cit:title/*/text()"/>~<xsl:value-of select="cit:onlineResource/*/cit:description/*/text()"/>~<xsl:value-of select="cit:onlineResource/*/cit:linkage/*/text()"/>
        </AdditionalInformationLink>
      </xsl:for-each>
	  
      <xsl:copy-of select="gn:info"/>
    </metadata>
  </xsl:template>
</xsl:stylesheet>
