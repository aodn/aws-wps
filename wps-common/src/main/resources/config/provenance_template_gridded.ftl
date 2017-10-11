<?xml version="1.0" encoding="UTF-8"?>
<prov:document
        xmlns:prov="http://www.w3.org/ns/prov#"
        xmlns:gnprov="http://geonetwork-opensource.org/prov-xml"
        xmlns:aodnprov="http://geonetwork-opensource.org/aodn/prov-xml"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dct="http://purl.org/dc/terms/"
        xmlns:msr="http://standards.iso.org/iso/19115/-3/msr/1.0"
        xmlns:gex="http://standards.iso.org/iso/19115/-3/gex/1.0"
        xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
        xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
        xmlns:gml="http://www.opengis.net/gml/3.2"
        xsi:schemaLocation="http://www.w3.org/ns/prov# http://www.w3.org/ns/prov.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dcterms.xsd http://standards.iso.org/iso/19115/-3/gex/1.0 http://standards.iso.org/iso/19115/-3/gex/1.0/gex.xsd http://standards.iso.org/iso/19115/-3/msr/1.0 http://standards.iso.org/iso/19115/-3/msr/1.0/msr.xsd http://standards.iso.org/iso/19115/-3/cit/1.0 http://standards.iso.org/iso/19115/-3/cit/1.0/cit.xsd  http://standards.iso.org/iso/19115/-3/gco/1.0 http://standards.iso.org/iso/19115/-3/gco/1.0/gco.xsd http://geonetwork-opensource.org/prov-xml http://geonetwork-opensource.org/prov-xml/gnprov.xsd http://geonetwork-opensource.org/aodn/prov-xml http://geonetwork-opensource.org/aodn/prov-xml/aodnprov.xsd">

    <prov:entity prov:id="WPS-Aggregator Dataset UID">
        <prov:location>${downloadUrl?xml}</prov:location>
        <prov:type codeList="codeListLocation#type" codeListValue="output">output</prov:type>
    </prov:entity>

    <prov:activity prov:id="WPS-Gridded-Aggregator-Service-Job:${jobId}">
        <prov:startTime>${startTime}</prov:startTime>
        <prov:endTime>${endTime}</prov:endTime>
        <dc:description>The GoGoDuck subsets and aggregates gridded data. Data is returned as a NetCDF file</dc:description>
    </prov:activity>

    <prov:entity prov:id="timeExtent">
        <gnprov:temporalExtent>
            <gex:EX_Extent>
                <gex:temporalElement>
                    <gex:EX_TemporalExtent>
                        <gex:extent>
                            <gml:TimePeriod gml:id="A1234">
                                <gml:beginPosition>${parameters.timeRange.start}</gml:beginPosition>
                                <gml:endPosition>${parameters.timeRange.end}</gml:endPosition>
                            </gml:TimePeriod>
                        </gex:extent>
                    </gex:EX_TemporalExtent>
                </gex:temporalElement>
            </gex:EX_Extent>
        </gnprov:temporalExtent>
        <prov:type codeList="codeListLocation#type" codeListValue="timeExtent">timeExtent</prov:type>
    </prov:entity>

<#assign eastBL = parameters.bbox.lonMin>
<#assign westBL = parameters.bbox.lonMax>
<#assign southBL = parameters.bbox.latMin>
<#assign northBL = parameters.bbox.latMax>

    <prov:entity prov:id="spatialExtent">
        <aodnprov:boundingBox>
            <gex:EX_Extent>
                <gex:geographicElement>
                    <gex:EX_GeographicBoundingBox>
                        <gex:westBoundLongitude>
                            <gco:Decimal>${westBL}</gco:Decimal>
                        </gex:westBoundLongitude>
                        <gex:eastBoundLongitude>
                            <gco:Decimal>${eastBL}</gco:Decimal>
                        </gex:eastBoundLongitude>
                        <gex:southBoundLatitude>
                            <gco:Decimal>${southBL}</gco:Decimal>
                        </gex:southBoundLatitude>
                        <gex:northBoundLatitude>
                            <gco:Decimal>${northBL}</gco:Decimal>
                        </gex:northBoundLatitude>
                    </gex:EX_GeographicBoundingBox>
                </gex:geographicElement>
            </gex:EX_Extent>
        </aodnprov:boundingBox>
        <prov:type codeList="codeListLocation#type" codeListValue="boundingBox">boundingBox</prov:type> <!-- helper provides this -->
    </prov:entity>

    <prov:entity prov:id="layerName">
        <prov:location>${layer}</prov:location>
        <prov:type codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:type>
    </prov:entity>

    <prov:entity prov:id="outputAggregationSettings">
        <#if settingsPath?has_content>
        <prov:location>https://github.com/aodn/geoserver-config/tree/production/${settingsPath}</prov:location>
        <#else>
        <prov:location>https://github.com/aodn/geoserver-config/tree/production/wps/gogoduck.xml</prov:location>
        </#if>
        <prov:type codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:type>
    </prov:entity>

    <prov:entity prov:id="sourceData">
        <prov:location>${sourceMetadataUrl}</prov:location>
        <prov:type codeList="codeListLocation#type" codeListValue="inputData">inputData</prov:type>
    </prov:entity>

    <prov:softwareAgent prov:id="JavaCode">
        <prov:location>https://github.com/aodn/geoserver-build/blob/master/src/extension/wps/doc/GOGODUCK_README.md</prov:location>
    </prov:softwareAgent>

    <prov:wasAssociatedWith>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:agent prov:ref="JavaCode"/>
        <prov:role codeList="codeListLocation#type" codeListValue="softwareSystem">softwareSystem</prov:role>
    </prov:wasAssociatedWith>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="timeExtent"/>
        <prov:role codeList="codeListLocation#type" codeListValue="timeExtent">timeExtent</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="spatialExtent"/>
        <prov:role codeList="codeListLocation#type" codeListValue="boundingBox">boundingBox</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="layerName"/>
        <prov:role codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="outputAggregationSettings"/>
        <prov:role codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:role>
    </prov:used>

    <prov:wasGeneratedBy>
        <prov:entity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasGeneratedBy>

    <prov:wasDerivedFrom>
        <prov:generatedEntity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:usedEntity prov:ref="sourceData"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasDerivedFrom>

    <prov:other>
        <dc:identifier>${jobId}</dc:identifier>
        <dc:title>Provenance document describing a gridded WPS result</dc:title>
        <dc:description>This gridded WPS used time, space and a layer definition to produce an aggregated NetCDF gridded output file</dc:description>
        <dc:coverage>northlimit=${northBL};southlimit=${southBL};eastlimit=${eastBL};westlimit=${westBL}</dc:coverage>
        <dc:subject>WPS</dc:subject>
        <dct:created>${.now?iso_utc}</dct:created>
    </prov:other>

</prov:document>
