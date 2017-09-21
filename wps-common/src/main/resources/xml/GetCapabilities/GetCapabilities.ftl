<?xml version="1.0" encoding="UTF-8"?>
<wps:Capabilities xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:wps="http://www.opengis.net/wps/1.0.0"
                  xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xlink="http://www.w3.org/1999/xlink"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xml:lang="en" service="WPS" version="1.0.0"
                  xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">
    <ows:ServiceIdentification>
        <ows:Title>Prototype GeoServer WPS</ows:Title>
        <ows:Abstract/>
        <ows:ServiceType>WPS</ows:ServiceType>
        <ows:ServiceTypeVersion>1.0.0</ows:ServiceTypeVersion>
    </ows:ServiceIdentification>
    <ows:ServiceProvider>
        <ows:ProviderName>GeoServer</ows:ProviderName>
        <ows:ProviderSite/>
        <ows:ServiceContact/>
    </ows:ServiceProvider>
    <ows:OperationsMetadata>
        <ows:Operation name="GetCapabilities">
            <ows:DCP>
                <ows:HTTP>
                    <ows:Get xlink:href="${geoserverWPSEndpointURL}"/>
                    <ows:Post xlink:href="${geoserverWPSEndpointURL}"/>
                </ows:HTTP>
            </ows:DCP>
        </ows:Operation>
        <ows:Operation name="DescribeProcess">
            <ows:DCP>
                <ows:HTTP>
                    <ows:Get xlink:href="${geoserverWPSEndpointURL}"/>
                    <ows:Post xlink:href="${geoserverWPSEndpointURL}"/>
                </ows:HTTP>
            </ows:DCP>
        </ows:Operation>
        <ows:Operation name="Execute">
            <ows:DCP>
                <ows:HTTP>
                    <ows:Get xlink:href="${geoserverWPSEndpointURL}"/>
                    <ows:Post xlink:href="${geoserverWPSEndpointURL}"/>
                </ows:HTTP>
            </ows:DCP>
        </ows:Operation>
    </ows:OperationsMetadata>
    <wps:ProcessOfferings>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:GeorectifyCoverage</ows:Identifier>
            <ows:Title>Georectify Coverage</ows:Title>
            <ows:Abstract>Georectifies a raster via Ground Control Points using gdal_warp</ows:Abstract>
        </wps:Process>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:GetFullCoverage</ows:Identifier>
            <ows:Title>GetFullCoverage</ows:Title>
            <ows:Abstract>Returns a raster from the catalog, with optional filtering</ows:Abstract>
        </wps:Process>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:GoGoDuck</ows:Identifier>
            <ows:Title>GoGoDuck</ows:Title>
            <ows:Abstract>Subset and download gridded collection as NetCDF files</ows:Abstract>
        </wps:Process>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:Import</ows:Identifier>
            <ows:Title>Import to Catalog</ows:Title>
            <ows:Abstract>Imports a feature collection into the catalog</ows:Abstract>
        </wps:Process>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:NetcdfOutput</ows:Identifier>
            <ows:Title>NetCDF download</ows:Title>
            <ows:Abstract>Subset and download collection as NetCDF files</ows:Abstract>
        </wps:Process>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:PagedUnique</ows:Identifier>
            <ows:Title>PagedUnique</ows:Title>
            <ows:Abstract>Gets the list of unique values for the given featurecollection on a specified field, allows
                optional paging
            </ows:Abstract>
        </wps:Process>
        <wps:Process wps:processVersion="1.0.0">
            <ows:Identifier>gs:StoreCoverage</ows:Identifier>
            <ows:Title>Store Coverage</ows:Title>
            <ows:Abstract>Stores a raster on the server.</ows:Abstract>
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
