package au.org.emii.geoserver.client;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.download.DownloadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpIndexReader implements IndexReader {
    private static final Logger logger = LoggerFactory.getLogger(HttpIndexReader.class);

    protected String geoserver = null;

    public HttpIndexReader(String geoserver) {
        this.geoserver = geoserver;
    }

    @Override
    public URIList getUriList(String profile, String timeField, String urlField, SubsetParameters subset) throws AggregationException {

        URIList uriList = new URIList();

        try {
            String downloadUrl = String.format("%s/wfs", geoserver);


            Map<String, String> params = new HashMap<String, String>();
            params.put("typeName", profile);
            params.put("SERVICE", "WFS");
            params.put("outputFormat", "csv");
            params.put("REQUEST", "GetFeature");
            params.put("VERSION", "1.0.0");

            //  Apply time filter if time parameters supplied
            String cqlTimeFilter = getCqlTimeFilter(subset, timeField);
            if (cqlTimeFilter != null) {
                params.put("CQL_FILTER", cqlTimeFilter);
            }


            byte[] postDataBytes = encodeMapForPostRequest(params);

            URL url = new URL(downloadUrl);
            HttpURLConnection conn = null;
            InputStream inputStream = null;
            BufferedInputStream bufferedStream = null;
            DataInputStream dataInputStream = null;


            //  Make HTTP request to geoserver
            conn = (HttpURLConnection) url.openConnection();

            try
            {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
                int responseCode = conn.getResponseCode();
                String responseMessage = conn.getResponseMessage();

                logger.info("HTTP Response : Code [" + responseCode + "], Message [" + responseMessage + "]");

                inputStream = conn.getInputStream();
                bufferedStream = new BufferedInputStream(inputStream);
                dataInputStream = new DataInputStream(bufferedStream);

                logger.info(String.format("Getting list of files from '%s'", downloadUrl));
                logger.info(String.format("Parameters: '%s'", new String(postDataBytes)));

                //  Read response from geoserver - CSV format
                //  Example structure:
                //  acorn_hourly_avg_rot_qc_timeseries_url.fid-7a10c7e5_15e34df590d_2000,939620,IMOS/ACORN/gridded_1h-avg-current-map_QC/ROT/2017/03/31/IMOS_ACORN_V_20170331T233000Z_ROT_FV01_1-hour-avg.nc,128950,"ROT, Rottnest Shelf",2017-03-31T23:30:00,POINT (1 2)
                String line;
                Integer i = 0;
                while ((line = dataInputStream.readLine()) != null) {
                    if (i > 0) { // Skip first line - it's the headers
                        logger.info("CSV line    = " + line);
                        String[] lineParts = line.split(",");
                        uriList.add(new URI(lineParts[2]));
                    } else {
                        logger.info("CSV headers = " + line);
                    }
                    i++;
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (bufferedStream != null) {
                    bufferedStream.close();
                }

                if (dataInputStream != null) {
                    dataInputStream.close();
                }

                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (Exception e) {
            logger.error("We could not obtain list of URLs, does the collection still exist?");
            throw new AggregationException(String.format("Could not obtain list of URLs: '%s'", e.getMessage()));
        }

        return uriList;
    }


    public Set<DownloadRequest> getDownloadRequestList(String layer, String timeField, String urlField, SubsetParameters subset) throws AggregationException {

        HashSet<DownloadRequest> downloadList = new HashSet<DownloadRequest>();

        try {

            String downloadUrl = String.format("%s/wfs", geoserver);

            Map<String, String> params = new HashMap<String, String>();
            params.put("typeName", layer);
            params.put("SERVICE", "WFS");
            params.put("outputFormat", "csv");
            params.put("REQUEST", "GetFeature");
            params.put("VERSION", "1.0.0");

            //  Apply time filter if time parameters supplied
            String cqlTimeFilter = getCqlTimeFilter(subset, timeField);
            if (cqlTimeFilter != null) {
                params.put("CQL_FILTER", cqlTimeFilter);
            }

            byte[] postDataBytes = encodeMapForPostRequest(params);

            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);

            InputStream inputStream = conn.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));

            logger.info(String.format("Getting list of files from '%s'", downloadUrl));
            logger.info(String.format("Parameters: '%s'", new String(postDataBytes)));
            String line = null;
            Integer i = 0;
            while ((line = dataInputStream.readLine()) != null) {
                if (i > 0) { // Skip first line - it's the headers
                    String[] lineParts = line.split(",");
                    long fileSize = Long.parseLong(lineParts[3]);
                    URI fileURI = new URI(lineParts[2]);

                    //  TODO:  source the base URL from configuration
                    URL fileURL = new URL("http://data.aodn.org.au/" + fileURI.toString());

                    logger.debug("DownloadRequest - URL [" + fileURL + "], Size [" + fileSize + "]");
                    DownloadRequest downloadRequest = new DownloadRequest(fileURL, fileSize);
                    downloadList.add(downloadRequest);
                }
                i++;
            }
        } catch (Exception e) {
            logger.error("We could not obtain list of URLs, does the collection still exist?");
            throw new AggregationException(String.format("Could not obtain list of URLs: '%s'", e.getMessage()));
        }

        return downloadList;
    }


    private byte[] encodeMapForPostRequest(Map<String, String> params) {
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

        return postDataBytes;
    }

    public static void main(String[] args) {
        HttpIndexReader indexReader = new HttpIndexReader("http://geoserver-123.aodn.org.au/geoserver/imos");
        String subsetString = "TIME,2009-01-01T00:00:00.000Z,2017-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0.0,100.0";
        SubsetParameters subsetParams = new SubsetParameters(subsetString);
        Set<DownloadRequest> downloadList = null;

        try {
            downloadList = indexReader.getDownloadRequestList("imos:acorn_hourly_avg_rot_qc_timeseries_url", "time", "file_url", subsetParams);
            System.out.println("File list size: " + downloadList.size());
            for (DownloadRequest currentDownload : downloadList) {
                System.out.println("  - " + currentDownload.getUrl().toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Bad Juju!");
        }
    }

    private String getCqlTimeFilter(SubsetParameters subset, String timeField)
    {
        String cqlTimeFilter = null;

        if(subset.get("TIME") != null) {
            String timeCoverageStart = subset.get("TIME").start;
            String timeCoverageEnd = subset.get("TIME").end;

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
}
