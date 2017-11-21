**The functionality described in this section is still to be finalised.  This section will require revision!!**

# GoGoDuck

GoGoDuck is a simple NetCDF file aggregator

## Configuring GoGoDuck downloads via the Portal

### Metadata Record

A typical Geonetwork record to enable a GoGoDuck download via the Portal will include the following online resource:

```
<gmd:onLine>
  <gmd:CI_OnlineResource>
    <gmd:linkage>
      <gmd:URL> [GEOSERVER_OWS_URL] </gmd:URL>
    </gmd:linkage>
    <gmd:protocol>
      <gco:CharacterString>OGC:WPS--gogoduck</gco:CharacterString>
    </gmd:protocol>
    <gmd:name>
      <gco:CharacterString> [LAYER_NAME] </gco:CharacterString>
    </gmd:name>
    <gmd:description>
      <gco:CharacterString>The GoGoDuck subsets and aggregates gridded data.  Data is returned as a NetCDF file or CSV</gco:CharacterString>
    </gmd:description>
  </gmd:CI_OnlineResource>
</gmd:onLine>
```

A more concrete example would look like:

```
<gmd:onLine>
  <gmd:CI_OnlineResource>
    <gmd:linkage>
      <gmd:URL>http://geoserver-123.aodn.org.au/geoserver/ows</gmd:URL>
    </gmd:linkage>
    <gmd:protocol>
      <gco:CharacterString>OGC:WPS--gogoduck</gco:CharacterString>
    </gmd:protocol>
    <gmd:name>
      <gco:CharacterString>acorn_hourly_avg_rot_nonqc_timeseries_url</gco:CharacterString>
    </gmd:name>
    <gmd:description>
      <gco:CharacterString>The GoGoDuck subsets and aggregates gridded data.  Data is returned as a NetCDF file or CSV</gco:CharacterString>
    </gmd:description>
  </gmd:CI_OnlineResource>
</gmd:onLine>
```

### Configuring Gridded NetCDF Aggregation settings

General Gridded NetCDF Aggregator settings are defined in the cloud formation template.  Default settings 
 can be overridden in the deployment properties file used to create/update the stack. 

#### Aggregation settings

| Element | Description |
| --- | --- |
|  chunkSize | the maximum amount of data (in bytes) to read into memory at a time when performing an aggregation |

#### Download settings

These settings are used when downloading files for aggregation as follows:

| Element | Description |
| --- | --- |
|  workerDownloadAttempts | |
|  workerDownloadDirectory | |
|  workerLocalStorageLimitBytes | use up to this amount of local storage space to buffer files for aggregation |
|  workerPoolSize | the number of threads to be used to download files |
|  workerRetryIntervalMs | |

### Controlling aggregation output

The default behaviour of the NetCDF aggregator is to copy the global attributes and subsetted dimension and variable definitions
from the first file aggregated to the output file prior to aggregating data.   This behaviour can be modified 
using the templates configuration file to specify aggregation override configuration
should be applied to selected layers.

To determine the aggregation configuration to be used for a given layer - each template element in the templates element is 
tested for a match against the regular expression specified in the match attribute.  The name of the first matching template
is used to source aggregation overrides by looking for a file with this name.  If no match
is found no overrides will be applied.

An example aggregation overrides (template) file is as follows:

```
<ac:templates xmlns:ac="http://aodn.org.au/aggregator/configuration">
    <ac:template match="srs_ghrsst.*,srs_sst.*" name="srs_ghrsst">
      <ac:attributes>
        <ac:remove name="start_time"/>
        <ac:remove name="stop_time"/>
        <ac:attribute name="time_coverage_start" type="String" value="${TIME_START}"/>
        <ac:attribute name="time_coverage_end" type="String" value="${TIME_END}"/>
        <ac:attribute name="title" match=".*" type="String" value="${0}, ${TIME_START}, ${TIME_END}"/>
        <ac:attribute name="southernmost_latitude" type="Double" value="${LAT_MIN}"/>
        <ac:attribute name="northernmost_latitude" type="Double" value="${LAT_MAX}"/>
        <ac:attribute name="westernmost_longitude" type="Double" value="${LON_MIN}"/>
        <ac:attribute name="easternmost_longitude" type="Double" value="${LON_MAX}"/>
      </ac:attributes>
      <ac:variables>
        <ac:variable name="time"/>
        <ac:variable name="lat"/>
        <ac:variable name="lon"/>
        <ac:variable name="dt_analysis" type="Float"/>
        <ac:variable name="l2p_flags"/>
        <ac:variable name="quality_level"/>
        <ac:variable name="satellite_zenith_angle" type="Float"/>
    
        <ac:variable name="sea_surface_temperature" type="Float">
          <ac:attribute name="_FillValue" type="Double" value="9.96920996838687e+36"/>
          <ac:attribute name="valid_min" type="Double" value="0.0"/>
          <ac:attribute name="valid_max" type="Double" value="350.0"/>
        </ac:variable>
    
        <ac:variable name="sses_bias" type="Float">
          <ac:attribute name="valid_range" type="Double">
            <ac:value>0.0</ac:value>
            <ac:value>350.0</ac:value>
          </ac:attribute>
        </ac:variable>
        
        <ac:variable name="sses_count" type="Float"/>
        <ac:variable name="sses_standard_deviation" type="Float"/>
    
        <ac:variable name="sst_dtime" type="Float"/>
      </ac:variables>
    </ac:template>
</ac:templates>
```

##### Remove element

A remove element is used to remove a global attribute when copying attributes from the first aggregation file

##### Attribute element

Attribute elements specify an attribute to be added or whose value should be replaced when copying 
attributes from the first aggregation file.

| Attribute | Description |
| --- | --- |
| name | the name of the attribute to add or replace |
| type | the type of the value to be added or used in the replacement |
| match | a java regular expression to be used to capture portions of an existing attributes value (refer [java Pattern class](http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html) and in particular (Groups and Capturing)[http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#cg])|
| value | The value to use for the attribute after substitution value - ${substitution value} - replacements have been made.  See below for valid substitution values |

##### Substitution values

The following substitution values can be used in an attribute value attribute:

| Substitution value | Description |
| --- | --- |
| TIME_START | The requested start time (UTC) for the aggregated dataset in ISO8601 format |
| TIME_END | The requested end time (UTC) of the aggregated dataset in ISO8601 format |
| LAT_MIN | The minimum latitude value of the aggregated dataset |
| LAT_MAX | The maximum latitude value of the aggregated dataset |
| LON_MIN | The minimum longitude value of the aggregated dataset |
| LON_MAX | The maximum longitude value of the aggregated dataset |
| [0-9] | The value of a captured group specified using the match attribute to select portions of the previous value of the attribute |

#### Variables element

The variables element contains the list of variables to be included in the aggregation along with any modified 
type, filler value, valid min, valid max, valid range or missing values (other variable attribute modifications are not supported)
