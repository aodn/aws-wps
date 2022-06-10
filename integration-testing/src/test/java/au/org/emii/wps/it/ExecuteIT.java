package au.org.emii.wps.it;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.*;

import au.org.emii.wps.util.ExecuteRequestBuilder;
import com.google.common.io.Files;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import net.opengis.wps.v_1_0_0.Execute;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.awaitility.Awaitility;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static au.org.emii.wps.util.GPathMatcher.hasGPath;
import static au.org.emii.wps.util.Matchers.validateWith;
import static au.org.emii.wps.util.NcmlValidatable.getNcml;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.xml.HasXPath.hasXPath;

public class ExecuteIT {
    private static final Duration TWENTY_MINUTES = Duration.of(20, ChronoUnit.MINUTES);
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
    public void testAcornSubset() throws IOException {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-01T00:00:00.000Z,2017-01-07T23:04:00.000Z;LATITUDE,-33.18,-31.45;LONGITUDE,114.82,115.39")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "application/x-netcdf")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        getNcml(outputLocation).content(
                // Check global attributes overridden as required
                hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", equalTo("2017-01-01T00:00:00Z")),
                hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", equalTo("2017-01-07T23:04:00Z")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_min']/@value", equalTo("-33.145229 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_max']/@value", equalTo("-31.485632 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_min']/@value", equalTo("114.849838 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_max']/@value", equalTo("115.359207 ")),
                // Check all variables have been included
                hasGPath("netcdf.variable.@name", containsInAnyOrder(
                        "TIME","LATITUDE","LONGITUDE","GDOP","UCUR","VCUR","UCUR_sd","VCUR_sd","NOBS1","NOBS2",
                        "UCUR_quality_control","VCUR_quality_control"
                        )
                )
        );
    }


    @Test
    public void testTimeseries() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-04T10:30:00.000Z,2017-01-04T11:30:00.000Z;LATITUDE,-31.8009,-31.8009;LONGITUDE,115.0227,115.0227")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "text/csv")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        given()
                .spec(spec)
                .when()
                .get(outputLocation)
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .body(equalTo(
                        "TIME (UTC),LATITUDE (degrees_north),LONGITUDE (degrees_east),GDOP (Degrees),UCUR (m s-1),VCUR (m s-1),UCUR_sd (m s-1),VCUR_sd (m s-1),NOBS1 (1),NOBS2 (1),UCUR_quality_control,VCUR_quality_control\n" +
                                "2017-01-04T10:30:00Z,-31.810335,115.019623,68.80474,0.019122316,0.5347731,0.04222228,0.044319205,6,6,1,1\n" +
                                "2017-01-04T11:30:00Z,-31.810335,115.019623,68.80474,-0.009952986,0.55120397,0.034548346,0.036436576,6,6,1,1\n"
                ));

    }


    @Test
    public void testTestModeTimeseries() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("TestMode", "true")
                .input("subset", "TIME,2017-01-04T10:30:00.000Z,2017-01-04T11:30:00.000Z;LATITUDE,-31.8009,-31.8009;LONGITUDE,115.0227,115.0227")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "text/csv")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        Response response = given()
                .spec(spec)
                .when()
                .get(outputLocation);

        String csvOutput = response.getBody().print();

        //  Check that there is at least one line of data in the CSV file - ie: 2 rows (1st is header)
        try {
            CSVParser parser = CSVParser.parse(csvOutput, CSVFormat.DEFAULT);
            int lineNum = 0;
            boolean dataReturned = false;
            for(CSVRecord record : parser) {
                if(lineNum > 0) {
                    if(record != null && record.size() > 0) {
                        dataReturned = true;
                    }
                    break;
                }
                lineNum++;
            }

            if(!dataReturned) {
                fail("No data returned in CSV output file!");
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
            fail("Unable to parse CSV output [" + csvOutput + "]: " + ioex.getMessage());
        }
    }


    @Test
    public void testTimeseriesInTimestampOrder() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-04T11:30:00.000Z,2017-01-06T11:30:00.000Z;LATITUDE,-31.8009,-31.8009;LONGITUDE,115.0227,115.0227")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "text/csv")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        Response response = given()
                .spec(spec)
                .when()
                .get(outputLocation);

        String csvOutput = response.getBody().print();

        //  First line is CSV header
        //  Time field is index 0
        //  Check that the timestamps are in ascending time order
        try {
            CSVParser parser = CSVParser.parse(csvOutput, CSVFormat.DEFAULT);
            int lineNum = 0;
            Instant currentInstant = null;
            Instant nextInstant;
            for(CSVRecord record : parser) {
                if(lineNum > 0) {
                    if(currentInstant == null) {
                        currentInstant = Instant.parse(record.get(0));
                    } else {
                        nextInstant = Instant.parse(record.get(0));

                        if(!currentInstant.isBefore(nextInstant)) {
                            //  Fail
                            fail("Timestamps not in ascending order. Current [" + currentInstant.toString() + "], Next [" + nextInstant.toString() +"]");
                        } else {
                            currentInstant = nextInstant;
                        }
                    }
                }
                lineNum++;
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
            fail("Unable to parse CSV output [" + csvOutput + "]: " + ioex.getMessage());
        }
    }

    @Test
    public void testSrsSubset() throws IOException {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "srs_ghrsst_l3s_1d_day_url")
                .input("subset", "TIME,2017-10-19T03:20:00.000Z,2017-10-19T03:20:00.000Z;LATITUDE,-90.0,90.0;LONGITUDE,-180.0,180.0")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "application/x-netcdf")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        getNcml(outputLocation).content(
                // Check global attributes overridden as required
                hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='start_time']/@value", equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='stop_time']/@value", equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='northernmost_latitude']/@value", equalTo("19.989999771118164 ")),
                hasXPath("/netcdf/attribute[@name='southernmost_latitude']/@value", equalTo("-69.98999786376953 ")),
                hasXPath("/netcdf/attribute[@name='westernmost_longitude']/@value", equalTo("70.01000213623047 ")),
                hasXPath("/netcdf/attribute[@name='easternmost_longitude']/@value", equalTo("189.99000549316406 ")),
                // Check only requested variables have been included
                hasGPath("netcdf.variable.@name", containsInAnyOrder(
                        "time", "lat", "lon", "dt_analysis", "l2p_flags", "quality_level", "satellite_zenith_angle",
                        "sea_surface_temperature", "sses_bias", "sses_count", "sses_standard_deviation", "sst_dtime"
                        )
                ),
                // Ensure type has been overridden as required
                hasXPath("/netcdf/variable[@name='sea_surface_temperature']/@type", equalTo("float"))
        );
    }

    @Test
    public void testCarsSubset() throws IOException {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "csiro_cars_weekly_url")
                .input("subset", "TIME,2009-12-19T00:00:00.000Z,2009-12-26T00:00:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0,100")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "application/x-netcdf")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        getNcml(outputLocation).content(
                // Check global attributes overridden as required
                hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", equalTo("2009-12-19T00:00:00Z")),
                hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", equalTo("2009-12-26T00:00:00Z")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_max']/@value", equalTo("-32.5 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_min']/@value", equalTo("-33.0 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_max']/@value", equalTo("115.5 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_min']/@value", equalTo("114.5 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_vertical_max']/@value", equalTo("100.0 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_vertical_min']/@value", equalTo("0.0 ")),
                // Check only requested variables have been included
                hasGPath("netcdf.variable.@name", containsInAnyOrder(
                        "DAY_OF_YEAR", "DEPTH", "LATITUDE", "LONGITUDE", "TEMP", "TEMP_mean", "TEMP_std_dev",
                        "TEMP_RMSspatialresid", "TEMP_RMSresid", "TEMP_sumofwgts", "TEMP_map_error", "TEMP_anomaly", "PSAL",
                        "PSAL_mean", "PSAL_std_dev", "PSAL_RMSspatialresid", "PSAL_RMSresid", "PSAL_sumofwgts",
                        "PSAL_map_error", "PSAL_anomaly", "DOX2", "DOX2_mean", "DOX2_RMSspatialresid", "DOX2_RMSresid",
                        "DOX2_sumofwgts", "DOX2_anomaly", "DENS", "DENS_mean", "DENS_anomaly", "NTR2", "NTR2_mean",
                        "NTR2_RMSspatialresid", "NTR2_RMSresid", "NTR2_sumofwgts", "NTR2_anomaly", "SLC2", "SLC2_mean",
                        "SLC2_RMSspatialresid", "SLC2_RMSresid", "SLC2_sumofwgts", "SLC2_anomaly", "PHOS", "PHOS_mean",
                        "PHOS_RMSspatialresid", "PHOS_RMSresid", "PHOS_sumofwgts", "PHOS_anomaly"
                        )
                )
        );
    }



    @Test
    public void testTimeseriesZip() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-04T10:30:00.000Z,2017-01-04T11:30:00.000Z;LATITUDE,-31.8009,-31.8009;LONGITUDE,115.0227,115.0227")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .input("aggregationOutputMime", "text/csv")
                .output("result", "application/zip")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        //  Output will be in ZIP file format.  We need to unzip the file and then verify the
        //  output.
        File tempDir = Files.createTempDir();
        try {
            File csvFile = getFileFromZip(tempDir, outputLocation, ".csv");

            BufferedReader fileReader = new BufferedReader(new FileReader(csvFile));

            String line;

            if((line = fileReader.readLine()) != null) {
                assertEquals(line.trim(), "TIME (UTC),LATITUDE (degrees_north),LONGITUDE (degrees_east),GDOP (Degrees),UCUR (m s-1),VCUR (m s-1),UCUR_sd (m s-1),VCUR_sd (m s-1),NOBS1 (1),NOBS2 (1),UCUR_quality_control,VCUR_quality_control");
            } else {
                fail("No lines returned in CSV file.");
            }

            if((line = fileReader.readLine()) != null) {
                assertEquals(line.trim(), "2017-01-04T10:30:00Z,-31.810335,115.019623,68.80474,0.019122316,0.5347731,0.04222228,0.044319205,6,6,1,1");
            } else {
                fail("Only one line returned in CSV file.  Expected 3");
            }

            if((line = fileReader.readLine()) != null) {
                assertEquals(line.trim(), "2017-01-04T11:30:00Z,-31.810335,115.019623,68.80474,-0.009952986,0.55120397,0.034548346,0.036436576,6,6,1,1");
            } else {
                fail("Only two line returned in CSV file.  Expected 3");
            }

        } catch (Exception ex) {
            fail("Unable to retrieve ZIP file & extract contents: " + ex.getMessage());
        } finally {
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch(IOException ioex) {}
        }
    }


    @Test
    public void testTestModeTimeseriesZip() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("TestMode", "true")
                .input("subset", "TIME,2017-01-04T10:30:00.000Z,2017-01-04T11:30:00.000Z;LATITUDE,-31.8009,-31.8009;LONGITUDE,115.0227,115.0227")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .input("aggregationOutputMime", "text/csv")
                .output("result", "application/zip")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        //  Output will be in ZIP file format.  We need to unzip the file and then verify the
        //  output.
        File tempDir = Files.createTempDir();
        try {
            File csvFile = getFileFromZip(tempDir, outputLocation, ".csv");
            //  Check that there is at least one line of data in the CSV file - ie: 2 rows (1st is header)
            CSVParser parser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.DEFAULT);
            int lineNum = 0;
            boolean dataReturned = false;
            for (CSVRecord record : parser) {
                if (lineNum > 0) {
                    if (record != null && record.size() > 0) {
                        dataReturned = true;
                    }
                    break;
                }
                lineNum++;
            }

            if (!dataReturned) {
                fail("No data returned in CSV output file!");
            }
        } catch (Exception ex) {
            fail("Unable to retrieve ZIP file & extract contents: " + ex.getMessage());
        } finally {
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch(IOException ioex) {}
        }
    }


    @Test
    public void testTimeseriesInTimestampOrderZip() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-04T11:30:00.000Z,2017-01-06T11:30:00.000Z;LATITUDE,-31.8009,-31.8009;LONGITUDE,115.0227,115.0227")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .input("aggregationOutputMime", "text/csv")
                .output("result", "application/zip")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        //  Output will be in ZIP file format.  We need to unzip the file and then verify the
        //  output.
        File tempDir = Files.createTempDir();
        try {
            File csvFile = getFileFromZip(tempDir, outputLocation, ".csv");

            //  First line is CSV header
            //  Time field is index 0
            //  Check that the timestamps are in ascending time order
            CSVParser parser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.DEFAULT);
            int lineNum = 0;
            Instant currentInstant = null;
            Instant nextInstant;
            for(CSVRecord record : parser) {
                if(lineNum > 0) {
                    if(currentInstant == null) {
                        currentInstant = Instant.parse(record.get(0));
                    } else {
                        nextInstant = Instant.parse(record.get(0));

                        if(!currentInstant.isBefore(nextInstant)) {
                            //  Fail
                            fail("Timestamps not in ascending order. Current [" + currentInstant.toString() + "], Next [" + nextInstant.toString() +"]");
                        } else {
                            currentInstant = nextInstant;
                        }
                    }
                }
                lineNum++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unable to parse CSV output: " + ex.getMessage());
        } finally {
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch(IOException ioex) {}
        }
    }


    @Test
    public void testCarsSubsetZip() throws IOException {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "csiro_cars_weekly_url")
                .input("subset", "TIME,2009-12-19T00:00:00.000Z,2009-12-26T00:00:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0,100")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .input("aggregationOutputMime", "application/x-netcdf")
                .output("result", "application/zip")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");


        //  Output will be in ZIP file format.  We need to unzip the file and then verify the
        //  output.
        File tempDir = Files.createTempDir();
        try {
            File ncFile = getFileFromZip(tempDir, outputLocation, ".nc");

            getNcml(ncFile.getAbsolutePath()).content(
                    // Check global attributes overridden as required
                    hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", equalTo("2009-12-19T00:00:00Z")),
                    hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", equalTo("2009-12-26T00:00:00Z")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lat_max']/@value", equalTo("-32.5 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lat_min']/@value", equalTo("-33.0 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lon_max']/@value", equalTo("115.5 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lon_min']/@value", equalTo("114.5 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_vertical_max']/@value", equalTo("100.0 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_vertical_min']/@value", equalTo("0.0 ")),
                    // Check only requested variables have been included
                    hasGPath("netcdf.variable.@name", containsInAnyOrder(
                            "DAY_OF_YEAR", "DEPTH", "LATITUDE", "LONGITUDE", "TEMP", "TEMP_mean", "TEMP_std_dev",
                            "TEMP_RMSspatialresid", "TEMP_RMSresid", "TEMP_sumofwgts", "TEMP_map_error", "TEMP_anomaly", "PSAL",
                            "PSAL_mean", "PSAL_std_dev", "PSAL_RMSspatialresid", "PSAL_RMSresid", "PSAL_sumofwgts",
                            "PSAL_map_error", "PSAL_anomaly", "DOX2", "DOX2_mean", "DOX2_RMSspatialresid", "DOX2_RMSresid",
                            "DOX2_sumofwgts", "DOX2_anomaly", "DENS", "DENS_mean", "DENS_anomaly", "NTR2", "NTR2_mean",
                            "NTR2_RMSspatialresid", "NTR2_RMSresid", "NTR2_sumofwgts", "NTR2_anomaly", "SLC2", "SLC2_mean",
                            "SLC2_RMSspatialresid", "SLC2_RMSresid", "SLC2_sumofwgts", "SLC2_anomaly", "PHOS", "PHOS_mean",
                            "PHOS_RMSspatialresid", "PHOS_RMSresid", "PHOS_sumofwgts", "PHOS_anomaly"
                            )
                    )
            );

        } catch (Exception ex) {
            fail("Unable to retrieve ZIP file & extract contents: " + ex.getMessage());
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    @Test
    public void testProvenanceRequested() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-01T00:00:00.000Z,2017-01-07T23:04:00.000Z;LATITUDE,-33.18,-31.45;LONGITUDE,114.82,115.39")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("provenance", "text/xml")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        String outputLocation = given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .body("ExecuteResponse.ProcessOutputs.Output.Identifier", equalTo("provenance"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        given()
                .spec(spec)
                .when()
                .get(outputLocation)
                .then()
                .statusCode(200)
                .contentType("application/xml")
                .body(
                        hasXPath("//TimePeriod/beginPosition", equalTo("2017-01-01T00:00:00Z")),
                        hasXPath("//TimePeriod/endPosition", equalTo("2017-01-07T23:04:00Z")),
                        hasXPath("//EX_GeographicBoundingBox/westBoundLongitude/Decimal", equalTo("114.82")),
                        hasXPath("//EX_GeographicBoundingBox/eastBoundLongitude/Decimal", equalTo("115.39")),
                        hasXPath("//EX_GeographicBoundingBox/southBoundLatitude/Decimal", equalTo("-33.18")),
                        hasXPath("//EX_GeographicBoundingBox/northBoundLatitude/Decimal", equalTo("-31.45")),
                        hasXPath("//entity[@id='layerName']/location", equalTo("acorn_hourly_avg_rot_qc_timeseries_url")),
                        hasXPath("//entity[@id='outputAggregationSettings']/location", not(isEmptyOrNullString())),
                        hasXPath("//entity[@id='sourceData']/location", endsWith("geonetwork/srv/api/records/028b9801-279f-427c-964b-0ffcdf310b59")),
                        hasXPath("//softwareAgent[@id='JavaCode']/location", not(isEmptyOrNullString())),
                        hasXPath("//other/identifier", equalTo(getJobId(statusUrl)))
                );

    }

    @Test
    public void testNoProvenanceRequested() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-01T00:00:00.000Z,2017-01-07T23:04:00.000Z;LATITUDE,-33.18,-31.45;LONGITUDE,114.82,115.39")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "application/x-netcdf")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .body(hasXPath("/ExecuteResponse/ProcessOutputs/Output/Identifier[text()='result']"))
                .body(not(hasXPath("/ExecuteResponse/ProcessOutputs/Output/Identifier[text()='provenance']")));
    }


    @Test
    public void testBathy() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "imos:bathy_ppb_deakin_url")
                .input("subset", "TIME,2017-01-01T00:00:00.000Z,2017-01-07T23:04:00.000Z;LATITUDE,-33.18,-31.45;LONGITUDE,114.82,115.39")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "application/x-netcdf")
                .build();

        String statusUrl = submitAndWaitToComplete(request, TWENTY_MINUTES);

        given()
                .spec(spec)
                .when()
                .get(statusUrl)
                .then()
                .statusCode(200)
                .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
                .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"))
                .body(hasXPath("/ExecuteResponse/ProcessOutputs/Output/Identifier[text()='result']"))
                .body(not(hasXPath("/ExecuteResponse/ProcessOutputs/Output/Identifier[text()='provenance']")));
    }


    @Test
    public void testNoEmail() {
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "acorn_hourly_avg_rot_qc_timeseries_url")
                .input("subset", "TIME,2017-01-01T00:00:00.000Z,2017-01-07T23:04:00.000Z;LATITUDE,-33.18,-31.45;LONGITUDE,114.82,115.39")
                .output("result", "application/x-netcdf")
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


    @Test
    public void testHtmlStatusPagesUpdated() throws IOException {
        // Create longer running request
        Execute request = new ExecuteRequestBuilder()
                .identifer("gs:GoGoDuck")
                .input("layer", "srs_ghrsst_l3s_1d_day_url")
                .input("subset", "TIME,2017-10-19T03:20:00.000Z,2017-10-19T03:20:00.000Z;LATITUDE,-90.0,90.0;LONGITUDE,-180.0,180.0")
                .input("callbackParams", "imos-wps-testing@mailinator.com")
                .output("result", "application/x-netcdf")
                .build();

        // Submit request
        String statusUrl = submit(request);
        String jobId = getJobId(statusUrl);

        // Should have a html status page for the job

        String htmlStatusUrl = statusUrl.replaceFirst("XML$", "HTML");
        hasPageContainingJobId(htmlStatusUrl, jobId);

        // Should have an admin page for the job

        String htmlAdminUrl = statusUrl.replaceFirst("XML$", "ADMIN");
        hasPageContainingJobId(htmlAdminUrl, jobId);

        // Job should appear on QUEUE page

        String htmlQueueUrl = statusUrl.replaceFirst("jobId.*$", "format=QUEUE");
        hasPageContainingJobId(htmlQueueUrl, jobId);
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

        Awaitility.await().atMost(maxWait).until(() -> {
            given()
                    .log().method()
                    .log().uri()
                    .get(statusUrl)
                    .then()
                    .log().status()
                    .statusCode(200)
                    .body(anyOf(
                            hasXPath("/ExecuteResponse/Status/ProcessSucceeded"),
                            hasXPath("/ExecuteResponse/Status/ProcessFailed")));
            return true;
        });

        System.out.println("Process completed");

        return statusUrl;
    }


    private String submit(Execute request) {
        return given()
                .spec(spec)
                .body(request, ObjectMapperType.JAXB)
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
