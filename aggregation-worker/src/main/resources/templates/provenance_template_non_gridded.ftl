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
        xmlns:wps="http://www.opengis.net/wps/1.0.0"
        xsi:schemaLocation="http://www.w3.org/ns/prov# http://www.w3.org/ns/prov.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dcterms.xsd http://standards.iso.org/iso/19115/-3/gex/1.0 http://standards.iso.org/iso/19115/-3/gex/1.0/gex.xsd http://standards.iso.org/iso/19115/-3/msr/1.0 http://standards.iso.org/iso/19115/-3/msr/1.0/msr.xsd http://standards.iso.org/iso/19115/-3/cit/1.0 http://standards.iso.org/iso/19115/-3/cit/1.0/cit.xsd  http://standards.iso.org/iso/19115/-3/gco/1.0 http://standards.iso.org/iso/19115/-3/gco/1.0/gco.xsd http://geonetwork-opensource.org/prov-xml http://geonetwork-opensource.org/prov-xml/gnprov.xsd http://geonetwork-opensource.org/aodn/prov-xml http://geonetwork-opensource.org/aodn/prov-xml/aodnprov.xsd http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">

    <prov:entity prov:id="WPS-Aggregator Dataset UID">
        <prov:location>${aggregatedDataUrl?xml}</prov:location>
        <prov:type codeList="codeListLocation#type" codeListValue="output">output</prov:type>
    </prov:entity>

    <prov:activity prov:id="WPS-NonGridded-Aggregator-Service-Job:${jobId}">
        <prov:startTime>${startTime}</prov:startTime>
        <prov:endTime>${endTime}</prov:endTime>
        <dc:description>This is a WPS service that returns a zip file of subsetted NetCDF files matching a query</dc:description>
    </prov:activity>

    <prov:entity prov:id="WPSQuery">
        <aodnprov:wpsQuery>
            <wps:Data>
                <LiteralData>${wpsQuery?xml}</LiteralData>
            </wps:Data>
        </aodnprov:wpsQuery>
        <prov:type codeList="codeListLocation#type" codeListValue="ecqlFilter">ecqlFilter</prov:type>
    </prov:entity>

    <prov:entity prov:id="layerName">
        <prov:location>${layerName}</prov:location>
        <prov:type codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:type>
    </prov:entity>

    <prov:entity prov:id="outputAggregationSettings">
        <prov:location>https://github.com/aodn/geoserver-config/tree/production/${settingsPath}</prov:location>
        <prov:type codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:type>
    </prov:entity>

    <prov:entity prov:id="sourceData">
        <prov:location>${sourceMetadataUrl}</prov:location>
        <prov:type codeList="codeListLocation#type" codeListValue="inputData">inputData</prov:type>
    </prov:entity>

    <prov:softwareAgent prov:id="JavaCode">
        <prov:location>https://github.com/aodn/geoserver-build/blob/master/src/extension/wps/doc/NCDFGENERATOR_README.md</prov:location>
    </prov:softwareAgent>

    <prov:wasAssociatedWith>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:agent prov:ref="JavaCode"/>
        <prov:role codeList="codeListLocation#type" codeListValue="softwareSystem">softwareSystem</prov:role>
    </prov:wasAssociatedWith>

    <prov:used>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="WPSQuery"/>
        <prov:role codeList="codeListLocation#type" codeListValue="ecqLFilter">ecqlFilter</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="layerName"/>
        <prov:role codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="outputAggregationSettings"/>
        <prov:role codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:role>
    </prov:used>

    <prov:wasGeneratedBy>
        <prov:entity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasGeneratedBy>

    <prov:wasDerivedFrom>
        <prov:generatedEntity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:usedEntity prov:ref="sourceData"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasDerivedFrom>

    <prov:other>
        <dc:identifier>${jobId}</dc:identifier>
        <dc:title>Provenance document describing a non gridded WPS result</dc:title>
        <dc:description>This non gridded WPS used a ecqlFilter and a layer definition to produce a zip file of NetCDF output</dc:description>
        <dc:subject>WPS</dc:subject>
        <dct:created>${.now?iso_utc}</dct:created>
    </prov:other>

</prov:document>
