package au.org.aodn.aws.wps;

import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Created by craigj on 7/08/17.
 */
public class WpsRequestHandlerTest {

/*    @Test
    public void handleRequest() throws IOException, InterruptedException {
        System.out.println(new Date());
        AwsApiRequest input = new AwsApiRequest();

        input.setBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" +
                "            <ows:Identifier>javaduck-small:9</ows:Identifier>\n" +
                "            <wps:DataInputs>\n" +
                "              <wps:Input>\n" +
                "                <ows:Identifier>layer</ows:Identifier>\n" +
                "                <wps:Data>\n" +
                "                  <wps:LiteralData>imos:acorn_hourly_avg_rot_qc_timeseries_url</wps:LiteralData>\n" +
                "                </wps:Data>\n" +
                "              </wps:Input>\n" +
                "              <wps:Input>\n" +
                "                <ows:Identifier>subset</ows:Identifier>\n" +
                "                <wps:Data>\n" +
                "<wps:LiteralData>TIME,2017-01-01T00:00:00.000Z,2017-12-25T23:04:00.000Z;LATITUDE,-33.18,-31.45;LONGITUDE,114.82,115.39</wps:LiteralData>\n" +
                "                </wps:Data>\n" +
                "              </wps:Input>\n" +
                "            </wps:DataInputs>\n" +
                "            <wps:ResponseForm>\n" +
                "              <wps:RawDataOutput mimeType=\"application/octet-stream\">\n" +
                "                <ows:Identifier>result</ows:Identifier>\n" +
                "              </wps:RawDataOutput>\n" +
                "            </wps:ResponseForm>\n" +
                "        </wps:Execute>");

        WpsRequestHandler dispatcher = new WpsRequestHandler();

        input.setHttpMethod("POST");
        dispatcher.handleRequest(input);
        System.out.println(new Date());
    }*/
}