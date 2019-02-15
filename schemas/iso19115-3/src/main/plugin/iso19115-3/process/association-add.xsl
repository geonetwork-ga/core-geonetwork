<?xml version="1.0" encoding="UTF-8"?>
<!--  
Stylesheet used to add a reference to a related record using aggregation info.
-->
<xsl:stylesheet xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:srv="http://standards.iso.org/iso/19115/-3/srv/2.0"
    xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
    xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:gn="http://www.fao.org/geonetwork"
    exclude-result-prefixes="#all" version="2.0">
    
    <xsl:param name="url"/>
    <xsl:param name="_uuid"/>
    <xsl:param name="protocol"/>
    <xsl:param name="name"/>
    <xsl:param name="desc"/>
    <xsl:param name="code"/>
    <xsl:param name="identifierDesc"/>
    <xsl:param name="associationType" select="'crossReference'"/>
    
    <xsl:template match="mri:MD_DataIdentification|
        *[@gco:isoType='mri:MD_DataIdentification']|
        srv:SV_ServiceIdentification">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates select="mri:citation"/>
            <xsl:apply-templates select="mri:abstract"/>
            <xsl:apply-templates select="mri:purpose"/>
            <xsl:apply-templates select="mri:credit"/>
            <xsl:apply-templates select="mri:status"/>
            <xsl:apply-templates select="mri:pointOfContact"/>
            <xsl:apply-templates select="mri:spatialRepresentationType"/>
            <xsl:apply-templates select="mri:spatialResolution"/>
            <xsl:apply-templates select="mri:temporalResolution"/>
            <xsl:apply-templates select="mri:topicCategory"/>
            <xsl:apply-templates select="mri:extent"/>
            <xsl:apply-templates select="mri:additionalDocumentation"/>
            <xsl:apply-templates select="mri:processingLevel"/>
            <xsl:apply-templates select="mri:resourceMaintenance"/>
            <xsl:apply-templates select="mri:graphicOverview"/>
            <xsl:apply-templates select="mri:resourceFormat"/>
            <xsl:apply-templates select="mri:descriptiveKeywords"/>
            <xsl:apply-templates select="mri:resourceSpecificUsage"/>
            <xsl:apply-templates select="mri:resourceConstraints"/>
            <xsl:apply-templates select="mri:associatedResource"/>
            
            <xsl:call-template name="fill"/>
            
            <xsl:apply-templates select="mri:defaultLocale"/>
            <xsl:apply-templates select="mri:otherLocale"/>
            <xsl:apply-templates select="mri:environmentDescription"/>
            <xsl:apply-templates select="mri:supplementalInformation"/>
            
            <xsl:apply-templates select="srv:*"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template name="fill">
       <xsl:if test="count(//mri:associatedResource/mri:MD_AssociatedResource[mri:metadataReference/cit:CI_Citation/cit:identifier/mcc:MD_Identifier/mcc:code/gco:CharacterString = $code]) = 0">
        <mri:associatedResource>
            <mri:MD_AssociatedResource>
                <mri:associationType>
                    <mri:DS_AssociationTypeCode codeList="codeListLocation#DS_AssociationTypeCode"
                        codeListValue="{$associationType}"/>
                </mri:associationType>
                <mri:metadataReference>
                    <cit:CI_Citation>
                        <cit:title>
                            <gco:CharacterString><xsl:value-of select="$name"/></gco:CharacterString>
                        </cit:title> 
                        <cit:identifier>
                            <mcc:MD_Identifier>
                                <mcc:code>
                                    <gco:CharacterString><xsl:value-of select="$code"/></gco:CharacterString>
                                </mcc:code>
                                <mcc:description>
                                    <gco:CharacterString><xsl:value-of select="$identifierDesc"/></gco:CharacterString>
                                </mcc:description>
                            </mcc:MD_Identifier>
                        </cit:identifier>
                        <xsl:if test="$identifierDesc = 'eCat Identifier'">
                            <cit:identifier>
                                <mcc:MD_Identifier>
                                    <mcc:code>
                                        <gco:CharacterString><xsl:value-of select="$_uuid"/></gco:CharacterString>
                                    </mcc:code>
                                    <mcc:description>
                                        <gco:CharacterString>UUID</gco:CharacterString>
                                    </mcc:description>
                                </mcc:MD_Identifier>
                            </cit:identifier>    
                        </xsl:if>
                        <cit:onlineResource>
                            <cit:CI_OnlineResource>
                                <cit:linkage>
                                    <gco:CharacterString><xsl:value-of select="$url"/></gco:CharacterString>
                                </cit:linkage>
                                <cit:protocol>
                                    <gco:CharacterString xsi:type="gco:CodeType"
                                        codeSpace="http://pid.geoscience.gov.au/def/schema/ga/ISO19115-3-2016/codelist/ga_profile_codelists.xml#gapCI_ProtocolTypeCode"><xsl:value-of select="$protocol"/></gco:CharacterString>
                                </cit:protocol>
                                <cit:description>
                                    <gco:CharacterString><xsl:value-of select="$desc"/></gco:CharacterString>
                                </cit:description>
                            </cit:CI_OnlineResource>
                        </cit:onlineResource>
                    </cit:CI_Citation>
                </mri:metadataReference>
            </mri:MD_AssociatedResource>
        </mri:associatedResource> 
       </xsl:if>
    </xsl:template>
    
    <!-- Do a copy of every nodes and attributes -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- Remove geonet:* elements. -->
    <xsl:template match="gn:*"
        priority="2"/>
</xsl:stylesheet>
