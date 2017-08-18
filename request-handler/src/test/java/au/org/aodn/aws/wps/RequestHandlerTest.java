package au.org.aodn.aws.wps;

import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Created by craigj on 7/08/17.
 */
public class RequestHandlerTest {

    @Test
    public void handleRequest() throws IOException, InterruptedException {
        System.out.println(new Date());
        AwsApiRequest input = new AwsApiRequest();

        input.setBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?><wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" +
            "  <ows:Identifier>aggregation-job:1</ows:Identifier>\n" +
            "  <wps:DataInputs>\n" +
            "    <wps:Input>\n" +
            "      <ows:Identifier>urlList</ows:Identifier>\n" +
            "      <wps:Data>\n" +
            "        <wps:LiteralData>http://geoserver-123.aodn.org.au/geoserver/imos/ows?service=WFS&amp;version=1.0.0&amp;request=GetFeature&amp;typeName=imos:acorn_hourly_avg_rot_qc_timeseries_url&amp;maxFeatures=5&amp;outputFormat=csv&amp;sortBy=time&amp;propertyName=file_url,size</wps:LiteralData>\n" +
            "      </wps:Data>\n" +
            "    </wps:Input>\n" +
            "    <wps:Input>\n" +
            "      <ows:Identifier>outputFile</ows:Identifier>\n" +
            "      <wps:Data>\n" +
            "        <wps:LiteralData>s3://imos-test-data.aodn.org.au/demo/agg.nc</wps:LiteralData>\n" +
            "      </wps:Data>\n" +
            "    </wps:Input>\n" +
            "    <wps:Input>\n" +
            "      <ows:Identifier>format</ows:Identifier>\n" +
            "      <wps:Data>\n" +
            "        <wps:LiteralData>csv</wps:LiteralData>\n" +
            "      </wps:Data>\n" +
            "    </wps:Input>\n" +
            "  </wps:DataInputs>\n" +
            "  <wps:ResponseForm>\n" +
            "    <wps:RawDataOutput mimeType=\"application/octet-stream\">\n" +
            "      <ows:Identifier>result</ows:Identifier>\n" +
            "    </wps:RawDataOutput>\n" +
            "  </wps:ResponseForm>\n" +
            "</wps:Execute>");

        RequestHandler dispatcher = new RequestHandler();

        Properties config = new Properties();
        config.setProperty("STATUS_LOCATION", "http://bucket/prefix/");
        config.setProperty("STATUS_FILENAME", "status.xml");
        config.setProperty("JOB_NAME", "javaduck");
        config.setProperty("JOB_QUEUE_NAME", "javaduck-small-in");
        config.setProperty("AWS_REGION", "us-east-1");

        input.setHttpMethod("POST");
        dispatcher.handleRequest(input, config);
        System.out.println(new Date());
    }
}