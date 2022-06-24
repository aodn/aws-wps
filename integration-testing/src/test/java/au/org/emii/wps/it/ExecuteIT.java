package au.org.emii.wps.it;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import au.org.emii.wps.util.ExecuteRequestBuilder;
import com.google.common.io.Files;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static au.org.emii.wps.util.GPathMatcher.hasGPath;
import static au.org.emii.wps.util.Matchers.validateWith;
import static au.org.emii.wps.util.NcmlValidatable.getNcml;

public class ExecuteIT {
    private static final Duration TWENTY_MINUTES = Duration.of(30, ChronoUnit.MINUTES);
    private static RequestSpecification spec;
    private static String SERVICE_ENDPOINT = System.getenv("WPS_ENDPOINT");
    public static final int BUFFER_SIZE = 1024;

    protected Logger logger = LoggerFactory.getLogger(ExecuteIT.class);

    // We need to use the mashaller and unmashalelr from our generated jaxb package
    protected class CustomObjectMapper implements ObjectMapper {

        private Marshaller marshaller;

        public CustomObjectMapper() throws JAXBException {
            JAXBContext jaxbContext = JAXBContext.newInstance(ExecuteResponse.class);
            marshaller = jaxbContext.createMarshaller();
        }

        // Should not have call this
        @Override
        public Object deserialize(ObjectMapperDeserializationContext content) {
            throw new UnsupportedOperationException("Have not implemented");
        }

        @Override
        public Object serialize(ObjectMapperSerializationContext content) {
            Object raw = content.getObjectToSerialize();
            StringWriter writer = new StringWriter();
            try {
                marshaller.marshal(raw, writer);
            }
            catch (JAXBException e) {
                logger.error("Error happens during marshall: {}", raw.toString());
            }
            finally {
                return writer.toString();
            }
        }
    }

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
    public void testAcornSubset() throws IOException, JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        getNcml(outputLocation).content(
                // Check global attributes overridden as required
                hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", Matchers.equalTo("2017-01-01T00:00:00Z")),
                hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", Matchers.equalTo("2017-01-07T23:04:00Z")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_min']/@value", Matchers.equalTo("-33.145229 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_max']/@value", Matchers.equalTo("-31.485632 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_min']/@value", Matchers.equalTo("114.849838 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_max']/@value", Matchers.equalTo("115.359207 ")),
                // Check all variables have been included
                hasGPath("netcdf.variable.@name", containsInAnyOrder(
                        "TIME","LATITUDE","LONGITUDE","GDOP","UCUR","VCUR","UCUR_sd","VCUR_sd","NOBS1","NOBS2",
                        "UCUR_quality_control","VCUR_quality_control"
                        )
                )
        );
    }


    @Test
    public void testTimeseries() throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        Response res = given()
                .spec(spec)
                .when()
                .get(outputLocation);

        res.then()
                .statusCode(200)
                .contentType("text/csv");


        String raw = res.asString();
        assertEquals("Body content is the same ",
                "TIME (UTC),LATITUDE (degrees_north),LONGITUDE (degrees_east),GDOP (Degrees),UCUR (m s-1),VCUR (m s-1),UCUR_sd (m s-1),VCUR_sd (m s-1),NOBS1 (1),NOBS2 (1),UCUR_quality_control,VCUR_quality_control\n" +
                        "2017-01-04T10:30:00Z,-31.810335,115.019623,68.80474,0.019122316,0.5347731,0.04222228,0.044319205,6,6,1,1\n" +
                        "2017-01-04T11:30:00Z,-31.810335,115.019623,68.80474,-0.009952986,0.55120397,0.034548346,0.036436576,6,6,1,1\n",
                raw);
    }


    @Test
    public void testTestModeTimeseries()  throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
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
    public void testTimeseriesInTimestampOrder()  throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
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
    public void testSrsSubset() throws IOException, JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        getNcml(outputLocation).content(
                // Check global attributes overridden as required
                hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", Matchers.equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='start_time']/@value", Matchers.equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", Matchers.equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='stop_time']/@value", Matchers.equalTo("2017-10-19T03:20:00Z")),
                hasXPath("/netcdf/attribute[@name='northernmost_latitude']/@value", Matchers.equalTo("19.989999771118164 ")),
                hasXPath("/netcdf/attribute[@name='southernmost_latitude']/@value", Matchers.equalTo("-69.98999786376953 ")),
                hasXPath("/netcdf/attribute[@name='westernmost_longitude']/@value", Matchers.equalTo("70.01000213623047 ")),
                hasXPath("/netcdf/attribute[@name='easternmost_longitude']/@value", Matchers.equalTo("189.99000549316406 ")),
                // Check only requested variables have been included
                hasGPath("netcdf.variable.@name", containsInAnyOrder(
                        "time", "lat", "lon", "dt_analysis", "l2p_flags", "quality_level", "satellite_zenith_angle",
                        "sea_surface_temperature", "sses_bias", "sses_count", "sses_standard_deviation", "sst_dtime"
                        )
                ),
                // Ensure type has been overridden as required
                hasXPath("/netcdf/variable[@name='sea_surface_temperature']/@type", Matchers.equalTo("float"))
        );
    }

    @Test
    public void testCarsSubset() throws IOException, JAXBException  {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        getNcml(outputLocation).content(
                // Check global attributes overridden as required
                hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", Matchers.equalTo("2009-12-19T00:00:00Z")),
                hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", Matchers.equalTo("2009-12-26T00:00:00Z")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_max']/@value", Matchers.equalTo("-32.5 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lat_min']/@value", Matchers.equalTo("-33.0 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_max']/@value", Matchers.equalTo("115.5 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_lon_min']/@value", Matchers.equalTo("114.5 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_vertical_max']/@value", Matchers.equalTo("100.0 ")),
                hasXPath("/netcdf/attribute[@name='geospatial_vertical_min']/@value", Matchers.equalTo("0.0 ")),
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
    public void testTimeseriesZip()  throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
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
    public void testTestModeTimeseriesZip()  throws JAXBException  {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
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
    public void testTimeseriesInTimestampOrderZip()  throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
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
    public void testCarsSubsetZip() throws IOException, JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");


        //  Output will be in ZIP file format.  We need to unzip the file and then verify the
        //  output.
        File tempDir = Files.createTempDir();
        try {
            File ncFile = getFileFromZip(tempDir, outputLocation, ".nc");

            getNcml(ncFile.getAbsolutePath()).content(
                    // Check global attributes overridden as required
                    hasXPath("/netcdf/attribute[@name='time_coverage_start']/@value", Matchers.equalTo("2009-12-19T00:00:00Z")),
                    hasXPath("/netcdf/attribute[@name='time_coverage_end']/@value", Matchers.equalTo("2009-12-26T00:00:00Z")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lat_max']/@value", Matchers.equalTo("-32.5 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lat_min']/@value", Matchers.equalTo("-33.0 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lon_max']/@value", Matchers.equalTo("115.5 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_lon_min']/@value", Matchers.equalTo("114.5 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_vertical_max']/@value", Matchers.equalTo("100.0 ")),
                    hasXPath("/netcdf/attribute[@name='geospatial_vertical_min']/@value", Matchers.equalTo("0.0 ")),
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
    public void testProvenanceRequested()  throws JAXBException  {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .body("ExecuteResponse.ProcessOutputs.Output.Identifier", Matchers.equalTo("provenance"))
                .extract()
                .path("ExecuteResponse.ProcessOutputs.Output.Reference.@href");

        given()
                .spec(spec)
                .when()
                .get(outputLocation)
                .then()
                .statusCode(200)
                .contentType("application/xml")
                .body("document.entity.findAll{i -> i.@id='timeExtent'}.temporalExtent.EX_Extent.temporalElement.EX_TemporalExtent.extent.TimePeriod.beginPosition", equalTo("2017-01-01T00:00:00Z"))
                .body("document.entity.findAll{i -> i.@id='timeExtent'}.temporalExtent.EX_Extent.temporalElement.EX_TemporalExtent.extent.TimePeriod.endPosition", equalTo("2017-01-07T23:04:00Z"))
                .body("document.entity.findAll{i -> i.@id='spatialExtent'}.boundingBox.EX_Extent.geographicElement.EX_GeographicBoundingBox.westBoundLongitude.Decimal", equalTo("114.82"))
                .body("document.entity.findAll{i -> i.@id='spatialExtent'}.boundingBox.EX_Extent.geographicElement.EX_GeographicBoundingBox.eastBoundLongitude.Decimal", equalTo("115.39"))
                .body("document.entity.findAll{i -> i.@id='spatialExtent'}.boundingBox.EX_Extent.geographicElement.EX_GeographicBoundingBox.southBoundLatitude.Decimal", equalTo("-33.18"))
                .body("document.entity.findAll{i -> i.@id='spatialExtent'}.boundingBox.EX_Extent.geographicElement.EX_GeographicBoundingBox.northBoundLatitude.Decimal", equalTo("-31.45"))
                .body("document.entity.location", hasItem("https://raw.githubusercontent.com/aodn/geoserver-config/production/wps/templates.xml"))
                .body("document.entity.location", hasItem("https://catalogue-imos.aodn.org.au:443/geonetwork/srv/api/records/028b9801-279f-427c-964b-0ffcdf310b59"))
                .body("document.softwareAgent.location", equalTo("https://github.com/aodn/geoserver-build/blob/master/src/extension/wps/doc/GOGODUCK_README.md"))
                .body("document.other.identifier", Matchers.equalTo(getJobId(statusUrl)))
                .body("document.entity.location", hasItem("acorn_hourly_avg_rot_qc_timeseries_url"));
    }

    @Test
    public void testNoProvenanceRequested()  throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .body("ExecuteResponse.ProcessOutputs.Output.Identifier", equalTo("result"))
                .body("ExecuteResponse.ProcessOutputs.Output.Identifier", not(equalTo("provenance")));
    }


    @Test
    public void testBathy()  throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()))
                .body("ExecuteResponse.ProcessOutputs.Output.Identifier", equalTo("result"))
                .body("ExecuteResponse.ProcessOutputs.Output.Identifier", not(equalTo("provenance")));
    }


    @Test
    public void testNoEmail()  throws JAXBException {
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
                .body("ExecuteResponse.Status.ProcessSucceeded", not(emptyOrNullString()));
    }


    @Test
    public void testHtmlStatusPagesUpdated() throws IOException, JAXBException {
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
                .body(Matchers.containsString(jobId));
    }


    private String submitAndWaitToComplete(Execute request, Duration maxWait) throws JAXBException {
        String statusUrl = submit(request);
        CountDownLatch latch = new CountDownLatch(1);

        Awaitility.await().atMost(maxWait).until(() -> {
            logger.info("Waiting for process to complete with success or failed status..");
            latch.await(20, TimeUnit.SECONDS);

            Response res = given()
                            .log().method()
                            .log().uri()
                            .get(statusUrl);

            res.then()
                .log()
                .status()
                .statusCode(200);

            XmlPath xmlPath = new XmlPath(res.asString());

            // Restassured used dot notation for xpath query
            return !xmlPath.get("ExecuteResponse.Status.ProcessSucceeded").toString().equalsIgnoreCase("")
                    || !xmlPath.get("ExecuteResponse.Status.ProcessFailed").toString().equalsIgnoreCase("");
        });

        logger.info("Process completed");

        return statusUrl;
    }

    private String submit(Execute request) throws JAXBException {
        return given()
                .spec(spec)
                .body(request, new CustomObjectMapper())
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
