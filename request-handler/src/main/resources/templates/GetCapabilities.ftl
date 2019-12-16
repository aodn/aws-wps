<?xml version="1.0" encoding="UTF-8"?>
<wps:Capabilities xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:wps="http://www.opengis.net/wps/1.0.0"
                  xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xlink="http://www.w3.org/1999/xlink"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xml:lang="en" service="WPS" version="1.0.0"
                  xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
    <ows:ServiceIdentification>
        <ows:Title>AODN Web Processing Service (WPS)</ows:Title>
        <ows:Abstract>The Australian Ocean Data Network (AODN) is an interoperable online network of marine and climate data resources.
The AODN Web Processing Service provides subsetting and aggregation services for AODN gridded data.</ows:Abstract>
        <ows:ServiceType>WPS</ows:ServiceType>
        <ows:ServiceTypeVersion>1.0.0</ows:ServiceTypeVersion>
        <ows:Fees/>
        <ows:AccessConstraints>Open</ows:AccessConstraints>
    </ows:ServiceIdentification>
    <ows:ServiceProvider>
        <ows:ProviderName>Australian Ocean Data Network (AODN)</ows:ProviderName>
        <ows:ServiceContact>
            <ows:IndividualName>Data Officer</ows:IndividualName>
            <ows:PositionName/>
            <ows:ContactInfo>
                <ows:Phone>
                    <ows:Voice/>
                    <ows:Facsimile/>
                </ows:Phone>
                <ows:Address>
                    <ows:DeliveryPoint/>
                    <ows:City/>
                    <ows:AdministrativeArea>Tasmania</ows:AdministrativeArea>
                    <ows:PostalCode/>
                    <ows:Country>Australia</ows:Country>
                    <ows:ElectronicMailAddress>info@aodn.org.au</ows:ElectronicMailAddress>
                </ows:Address>
            </ows:ContactInfo>
        </ows:ServiceContact>
    </ows:ServiceProvider>
    <ows:OperationsMetadata>
        <ows:Operation name="GetCapabilities">
            <ows:DCP>
                <ows:HTTP>
                    <ows:Get xlink:href="${wpsEndpointURL}"/>
                    <ows:Post xlink:href="${wpsEndpointURL}"/>
                </ows:HTTP>
            </ows:DCP>
        </ows:Operation>
        <ows:Operation name="DescribeProcess">
            <ows:DCP>
                <ows:HTTP>
                    <ows:Get xlink:href="${wpsEndpointURL}"/>
                    <ows:Post xlink:href="${wpsEndpointURL}"/>
                </ows:HTTP>
            </ows:DCP>
        </ows:Operation>
        <ows:Operation name="Execute">
            <ows:DCP>
                <ows:HTTP>
                    <ows:Post xlink:href="${wpsEndpointURL}"/>
                </ows:HTTP>
            </ows:DCP>
        </ows:Operation>
    </ows:OperationsMetadata>
    <wps:ProcessOfferings>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:GoGoDuck</ows:Identifier>
            <ows:Title>GoGoDuck</ows:Title>
            <ows:Abstract>Subset and download gridded collection as NetCDF files</ows:Abstract>
        </wps:Process>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:NetcdfOutput</ows:Identifier>
            <ows:Title>NetCDF download</ows:Title>
            <ows:Abstract>Subset and download collection as NetCDF files</ows:Abstract>
        </wps:Process>
    </wps:ProcessOfferings>
    <wps:Languages>
        <wps:Default>
            <ows:Language>en-US</ows:Language>
        </wps:Default>
        <wps:Supported>
            <ows:Language>en-US</ows:Language>
        </wps:Supported>
    </wps:Languages>
</wps:Capabilities>
