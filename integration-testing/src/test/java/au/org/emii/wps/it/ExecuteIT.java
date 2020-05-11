package au.org.emii.wps.it;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.*;

import au.org.emii.wps.util.ExecuteRequestBuilder;
import com.google.common.io.Files;
import com.jayway.awaitility.Duration;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import net.opengis.wps.v_1_0_0.Execute;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static au.org.emii.wps.util.GPathMatcher.hasGPath;
import static au.org.emii.wps.util.Matchers.validateWith;
import static au.org.emii.wps.util.NcmlValidatable.getNcml;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.xml.HasXPath.hasXPath;

public class ExecuteIT {
    private static final Duration TWENTY_MINUTES = new Duration(20, TimeUnit.MINUTES);
    private static RequestSpecification spec;
    private static String SERVICE_ENDPOINT = System.getenv("WPS_ENDPOINT");
    public static final int BUFFER_SIZE = 1024;

    @BeforeClass
    public static void initSpec() {
        spec = new RequestSpecBuilder()
                .setBaseUri(SERVICE_ENDPOINT)
                .setContentType(ContentType.XML)
                .addFilter(new ResponseLoggingFilter())
                .addFilter(new RequestLoggingFilter())
                .build();
    }

    @Test
    public void testNoParameters() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoduck")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"));
    }


    private void hasPageContainingJobId(String pageUrl, String jobId) {
        given()
                .spec(spec)
                .when()
                .get(pageUrl)
                .then()
                .statusCode(200)
                .body(containsString(jobId));
    }


    private String submitAndWaitToComplete(Execute request, Duration maxWait) {
        String statusUrl = submit(request);

        System.out.println("Waiting for process to complete...");

        await().atMost(maxWait).until(() ->
                given()
                        .log().method()
                        .log().path()
                        .get(statusUrl)
                        .then()
                        .log().status()
                        .statusCode(200)
                        .body(anyOf(
                                hasXPath("/ExecuteResponse/Status/ProcessSucceeded"),
                                hasXPath("/ExecuteResponse/Status/ProcessFailed")))
        );

        System.out.println("Process completed");

        return statusUrl;
    }


    private String submit(Execute request) {
        return given()
                .spec(spec)
                .content(request, ObjectMapperType.JAXB)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .extract()
                .path("ExecuteResponse.@statusLocation");
    }

    private String getJobId(String statusUrl) {
        // Get query parameters as a list

        List<NameValuePair> queryParams;

        try {
            URIBuilder parser = new URIBuilder(statusUrl);
            queryParams = parser.getQueryParams();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Find jobId parameter and return its value

        for (NameValuePair param: queryParams) {
            if (param.getName().equals("jobId")) {
                return param.getValue();
            }
        }

        return null;
    }

    private File getFileFromZip(File tempDir, String outputLocation, String fileExtension) throws Exception {
        File zipFile = new File(tempDir, "zipOut.zip");
        FileUtils.copyURLToFile(new URL(outputLocation), zipFile);

        unzipFiles(new ZipFile(zipFile), tempDir);

        //  Find the NC file
        String[] ls = tempDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(fileExtension)) {
                    return true;
                }
                return false;
            }
        });

        System.out.println("LS output size: " + ls.length);

        if (ls.length > 0) {

            File ncFile = new File(tempDir, ls[0]);
            return ncFile;
        }

        return null;
    }


    /**
     * Unzip the provided zip file to the location provided.
     *
     * @param zipFile  The zip file
     * @param location A location to unzip the files to
     */
    public static void unzipFiles(ZipFile zipFile, File location) {
        if(zipFile != null) {
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while(zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();

                if(location.isDirectory() && !location.exists()) {
                    boolean created = location.mkdirs();
                }

                File parent = location.getParentFile();
                if(parent != null) {
                    boolean created = parent.mkdirs();
                }

                File outFile = new File(location, entry.getName());

                try (FileOutputStream outStream = new FileOutputStream(outFile);
                     InputStream inStream = zipFile.getInputStream(entry)) {

                    byte[] bytes = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while((bytesRead = inStream.read(bytes)) > 0) {
                        outStream.write(bytes, 0, bytesRead);
                    }

                    outStream.flush();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
