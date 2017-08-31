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
import au.org.emii.download.Download;
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
        String timeCoverageStart = subset.get("TIME").start;
        String timeCoverageEnd = subset.get("TIME").end;

        URIList uriList = new URIList();

        try {
            String downloadUrl = String.format("%s/wfs", geoserver);
            String cqlFilter = String.format("%s >= %s AND %s <= %s",
                    timeField, timeCoverageStart, timeField, timeCoverageEnd
            );

            Map<String, String> params = new HashMap<String, String>();
            params.put("typeName", profile);
            params.put("SERVICE", "WFS");
            params.put("outputFormat", "csv");
            params.put("REQUEST", "GetFeature");
            params.put("VERSION", "1.0.0");
            params.put("CQL_FILTER", cqlFilter);

            byte[] postDataBytes = encodeMapForPostRequest(params);

            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
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
                    logger.info("CSV line    = " + line);
                    String[] lineParts = line.split(",");
                    uriList.add(new URI(lineParts[2]));
                }
                else
                {
                    logger.info("CSV headers = " + line);
                }
                i++;
            }
        }
        catch (Exception e) {
            logger.error("We could not obtain list of URLs, does the collection still exist?");
            throw new AggregationException(String.format("Could not obtain list of URLs: '%s'", e.getMessage()));
        }

        return uriList;
    }


    public Set<DownloadRequest> getDownloadRequestList(String profile, String timeField, String urlField, SubsetParameters subset) throws AggregationException {
        String timeCoverageStart = subset.get("TIME").start;
        String timeCoverageEnd = subset.get("TIME").end;

        HashSet<DownloadRequest> downloadList = new HashSet<DownloadRequest>();
        //URIList uriList = new URIList();

        try {

            String downloadUrl = String.format("%s/wfs", geoserver);
            String cqlFilter = String.format("%s >= %s AND %s <= %s",
                    timeField, timeCoverageStart, timeField, timeCoverageEnd
            );

            Map<String, String> params = new HashMap<String, String>();
            params.put("typeName", profile);
            params.put("SERVICE", "WFS");
            params.put("outputFormat", "csv");
            params.put("REQUEST", "GetFeature");
            params.put("VERSION", "1.0.0");
            params.put("CQL_FILTER", cqlFilter);

            byte[] postDataBytes = encodeMapForPostRequest(params);

            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
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

                    logger.info("DownloadRequest - URL [" + fileURL + "], Size [" + fileSize + "]");
                    DownloadRequest downloadRequest = new DownloadRequest(fileURL, fileSize);
                    downloadList.add(downloadRequest);
                }
                i++;
            }
        }
        catch (Exception e) {
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
        }
        catch (Exception e) {
            logger.error(String.format("Error encoding parameters: '%s'", e.getMessage()));
        }

        return postDataBytes;
    }

    public static void main(String[] args)
    {
        HttpIndexReader indexReader = new HttpIndexReader("http://geoserver-123.aodn.org.au/geoserver/imos");
        String subsetString = "TIME,2009-01-01T00:00:00.000Z,2017-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0.0,100.0";
        SubsetParameters subsetParams = new SubsetParameters(subsetString);
        URIList uriList = null;

        try {
            uriList = indexReader.getUriList("imos:acorn_hourly_avg_rot_qc_timeseries_url", "time", "file_url", subsetParams);
            System.out.println("uriList size: " + uriList.size());
            for(URI currentUri : uriList)
            {
                System.out.println("  - " + currentUri.toString());
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Bad Juju!");
        }
    }
}
