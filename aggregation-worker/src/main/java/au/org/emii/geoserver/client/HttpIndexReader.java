package au.org.emii.geoserver.client;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.download.DownloadRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.CalendarDateRange;


public class HttpIndexReader {
    private static final Logger logger = LoggerFactory.getLogger(HttpIndexReader.class);

    private String geoserver = null;


    public HttpIndexReader(String geoserver) {
        this.geoserver = geoserver;
    }

    public List<DownloadRequest> getDownloadRequestList(String layer, String timeField, String urlField, CalendarDateRange subsetTimeRange) throws AggregationException {

        ArrayList<DownloadRequest> downloadList = new ArrayList<>();
        String geoserverWfsEndpoint = String.format("%s/wfs", geoserver);

        try {

            Map<String, String> params = new HashMap<>();
            params.put("typeName", layer);
            params.put("SERVICE", "WFS");
            params.put("outputFormat", "csv");
            params.put("REQUEST", "GetFeature");
            params.put("VERSION", "1.0.0");

            try (
                    InputStream inputStream = doWfsGet(subsetTimeRange, timeField, params, getTimeSortClauseAsc(timeField));
                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
                    InputStreamReader streamReader = new InputStreamReader(dataInputStream);
                    BufferedReader reader = new BufferedReader(streamReader)
            ) {

                String line;
                Integer i = 0;
                int fileUrlIndex = 0;
                int fileSizeIndex = 0;

                while ((line = reader.readLine()) != null) {

                    if (i > 0) { // First line is the headers
                        String[] lineParts = line.split(",");
                        long fileSize = Long.parseLong(lineParts[fileSizeIndex]);
                        URI fileURI = new URI(lineParts[fileUrlIndex]);

                        //  Form the download url
                        URL fileURL = new URL(WpsConfig.getProperty(WpsConfig.DATA_DOWNLOAD_URL_PREFIX_CONFIG_KEY) + fileURI.toString());
                        downloadList.add(new DownloadRequest(fileURL, fileSize));
                    } else {
                        //  The first line is the header - which lists all of the fields returned.
                        //  We are actually only really interested in the 'file_url' field- because
                        //  that is the URL of the file (obviously).  Some collections return different
                        //  sets of columns in the CSV - so find the column position where the file_url is located.
                        String[] headerFields = line.split(",");

                        int headerFieldIndex = 0;
                        for (String currentField : headerFields) {

                            if (currentField.trim().equalsIgnoreCase(urlField)) {
                                logger.info("Found [" + urlField + "] field in CSV output at position [" + headerFieldIndex + "]");
                                fileUrlIndex = headerFieldIndex;
                            }

                            if (currentField.trim().equalsIgnoreCase("size")) {
                                logger.info("Found [size] field in CSV output at position [" + headerFieldIndex + "]");
                                fileSizeIndex = headerFieldIndex;
                            }
                            headerFieldIndex++;
                        }
                    }
                    i++;
                }
            }
            logger.debug("DownloadRequest - # files requested : " + downloadList.size());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("Unable to list file URLs. Layer name [" + layer + "], HttpIndex URL [" + geoserverWfsEndpoint + "]: " + e.getMessage());
            throw new AggregationException(String.format("Could not obtain list of URLs: '%s'", e.getMessage()));
        }

        return downloadList;
    }


    public String getLatestTimeStep(String layer, String timeField) throws AggregationException, TimeNotSupportedException {

        String geoserverWfsEndpoint = String.format("%s/wfs", geoserver);

        try {

            Map<String, String> params = new HashMap<>();
            params.put("typeName", layer);
            params.put("SERVICE", "WFS");
            params.put("outputFormat", "application/json");
            params.put("maxFeatures", "1");
            params.put("REQUEST", "GetFeature");
            params.put("VERSION", "1.0.0");

            try (
                    InputStream inputStream = doWfsGet(null, timeField, params, getTimeSortClauseDesc(timeField));
                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
                    InputStreamReader streamReader = new InputStreamReader(dataInputStream);
                    BufferedReader reader = new BufferedReader(streamReader)
            ) {

                String line;
                StringBuilder jsonBuilder = new StringBuilder();
                //  The request should have returned us a JSON object (only 1 due to the maxFeatures=1 parameter)
                while((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }


                logger.info("JSON response [" + jsonBuilder.toString() + "]");

                /*
                    The JSON returned will look something like this:

                    {"type":"FeatureCollection",
                     "totalFeatures":1,
                     "features":[{
                        "type":"Feature",
                        "id":"bathy_ppb_deakin_url.fid--26525c03_163b2d80395_-75c4",
                        "geometry":null,
                        "properties":{
                            "timestep_id":819995,
                            "collection_name":"bathy_ppb_deakin_url",
                            "file_url":"Deakin_University/bathymetry/Victorian-coast_Bathy_10m.nc",
                            "size":6.3451275957E10,
                            "time":null}
                        }],
                    "crs":null}

                    We would usually expect the 'time' property to have a value - but it is possible that it is null
                    for a layer that doesn't have a time extent (bathymetry for example).
                 */
                if(!jsonBuilder.toString().isEmpty()) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonRoot = mapper.readTree(jsonBuilder.toString());
                    if(jsonRoot != null) {
                        ArrayNode featuresNode = (ArrayNode) jsonRoot.get("features");
                        if (featuresNode != null) {
                            if(featuresNode.get(0) != null) {
                                JsonNode propertiesNode = featuresNode.get(0).get("properties");
                                if (propertiesNode != null) {
                                    JsonNode timePropertyNode = propertiesNode.get("time");
                                    if (timePropertyNode != null) {
                                        String timeNodeValue = timePropertyNode.asText();
                                        if(timeNodeValue != null && timeNodeValue.compareTo("null") != 0) {
                                            logger.info("Time value not null : " + timePropertyNode.asText());
                                            return timePropertyNode.asText();
                                        } else {
                                            throw new TimeNotSupportedException("Null time property value.");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (TimeNotSupportedException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("Unable to determine latest timestamp. Layer name [" + layer + "], HttpIndex URL [" + geoserverWfsEndpoint + "]: " + e.getMessage());
            throw new AggregationException(String.format("Could not obtain list of files: '%s'", e.getMessage()));
        }

        return null;
    }



    private String encodeMapForPostRequest(Map<String, String> params) {
        byte[] postDataBytes = null;
        try {
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            postDataBytes = postData.toString().getBytes("UTF-8");
        } catch (Exception e) {
            logger.error(String.format("Error encoding parameters: '%s'", e.getMessage()));
        }

        return new String(postDataBytes);
    }


    public static void main(String[] args) {
        HttpIndexReader indexReader = new HttpIndexReader("http://geoserver-123.aodn.org.au/geoserver/imos");
        String subsetString = "TIME,2009-01-01T00:00:00.000Z,2017-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0.0,100.0";
        SubsetParameters subsetParams = SubsetParameters.parse(subsetString);
        List<DownloadRequest> downloadList = null;

        try {
            downloadList = indexReader.getDownloadRequestList("imos:acorn_hourly_avg_rot_qc_timeseries_url", "time", "file_url", subsetParams.getTimeRange());
            System.out.println("File list size: " + downloadList.size());
            for (DownloadRequest currentDownload : downloadList) {
                System.out.println("  - " + currentDownload.getUrl().toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Bad Juju!");
        }
    }

    private String getCqlTimeFilter(CalendarDateRange subsetTimeRange, String timeField) {
        String cqlTimeFilter = null;

        if (subsetTimeRange != null) {
            String timeCoverageStart = subsetTimeRange.getStart().toString();
            String timeCoverageEnd = subsetTimeRange.getEnd().toString();

            if (timeCoverageStart != null && timeCoverageEnd != null) {  //  Start + end dates
                cqlTimeFilter = String.format("%s >= %s AND %s <= %s",
                        timeField, timeCoverageStart, timeField, timeCoverageEnd);
            } else if (timeCoverageStart != null) {  //  Start date only
                cqlTimeFilter = String.format("%s >= %s", timeField, timeCoverageStart);
            } else if (timeCoverageEnd != null) {  //  End date only
                cqlTimeFilter = String.format("%s <= %s", timeField, timeCoverageEnd);
            }
        }
        return cqlTimeFilter;
    }


    /**
     * Form sort clause for CQL filter of the form:
     *   &sortBy=<time_field>+A
     *
     * @param timeField
     * @return
     */
    private String getTimeSortClauseAsc(String timeField) {
        if(timeField != null) {
            return "&sortBy=" + timeField + "+A";
        }
        return "";
    }


    /**
     * Form sort clause for CQL filter of the form:
     *   &sortBy=<time_field>+D
     *
     * @param timeField
     * @return
     */
    private String getTimeSortClauseDesc(String timeField) {
        if(timeField != null) {
            return "&sortBy=" + timeField + "+D";
        }
        return "";
    }


    private InputStream doWfsGet(CalendarDateRange subsetTimeRange, String timeField, Map<String, String> params, String timeSortClause) throws IOException {
        String geoserverWfsEndpoint = String.format("%s/wfs", geoserver);

        if(subsetTimeRange != null) {
            //  Apply time filter if time parameters supplied
            String cqlTimeFilter = getCqlTimeFilter(subsetTimeRange, timeField);
            if (cqlTimeFilter != null) {
                //  Add sortBy clause to order the files by descending timestamp
                params.put("CQL_FILTER", cqlTimeFilter);
            }
            logger.info("CQL Time Filter: " + cqlTimeFilter);
        }


        //  URL encode the parameters - except the sortBy parameter
        String getParametersString = encodeMapForPostRequest(params);

        if(timeSortClause != null) {
            getParametersString += timeSortClause;
        }

        byte[] getParamsBytes = getParametersString.getBytes();

        logger.info("GETting list of files from [" + geoserverWfsEndpoint + "?" + new String(getParamsBytes));

        URL url = new URL(geoserverWfsEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(getParamsBytes.length));
        conn.setDoOutput(true);
        conn.getOutputStream().write(getParamsBytes);
        return conn.getInputStream();
    }
}
