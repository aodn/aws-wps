package au.org.emii.wps.it;

import au.org.emii.wps.util.ExecuteRequestBuilder;
import com.jayway.awaitility.Duration;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.specification.RequestSpecification;
import net.opengis.wps.v_1_0_0.Execute;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static au.org.emii.wps.util.Matchers.validateWith;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasXPath;

public class ExecuteIT {
    private static final Duration TWENTY_MINUTES = new Duration(20, TimeUnit.MINUTES);
    private static RequestSpecification spec;
    private static String SERVICE_ENDPOINT = System.getenv("WPS_ENDPOINT");

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
    public void testAcornSubset() {
        Execute request = new ExecuteRequestBuilder()
            .identifer("gs:GoGoDuck")
            .input("layer", "imos:acorn_hourly_avg_rot_qc_timeseries_url")
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
            .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"));
    }

    @Test
    public void testTimeseries() {
        Execute request = new ExecuteRequestBuilder()
            .identifer("gs:GoGoDuck")
            .input("layer", "imos:acorn_hourly_avg_rot_qc_timeseries_url")
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
    public void testSrsSubset() {
        Execute request = new ExecuteRequestBuilder()
            .identifer("gs:GoGoDuck")
            .input("layer", "srs_ghrsst_l3s_1d_day_url")
            .input("subset", "TIME,2017-10-19T03:20:00.000Z,2017-10-19T03:20:00.000Z;LATITUDE,-90.0,90.0;LONGITUDE,-180.0,180.0")
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
            .body(hasXPath("/ExecuteResponse/Status/ProcessSucceeded"));
    }

    @Test
    public void testProvenanceRequested() {
        Execute request = new ExecuteRequestBuilder()
            .identifer("gs:GoGoDuck")
            .input("layer", "imos:acorn_hourly_avg_rot_qc_timeseries_url")
            .input("subset", "TIME,2017-01-01T00:00:00.000Z,2017-01-07T23:04:00.000Z;LATITUDE,-33.18,-31.45;LONGITUDE,114.82,115.39")
            .input("callbackParams", "imos-wps-testing@mailinator.com")
            .output("result", "application/x-netcdf")
            .output("provenance", "text/xml")
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
            .body(hasXPath("/ExecuteResponse/ProcessOutputs/Output/Identifier[text()='provenance']"));
    }

    @Test
    public void testNoProvenanceRequested() {
        Execute request = new ExecuteRequestBuilder()
            .identifer("gs:GoGoDuck")
            .input("layer", "imos:acorn_hourly_avg_rot_qc_timeseries_url")
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
            .input("layer", "imos:acorn_hourly_avg_rot_qc_timeseries_url")
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

    private String submitAndWaitToComplete(Execute request, Duration maxWait) {
        String statusUrl = given()
            .spec(spec)
            .content(request, ObjectMapperType.JAXB)
        .when()
            .post()
        .then()
            .statusCode(200)
            .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
        .extract()
            .path("ExecuteResponse.@statusLocation");

        await().atMost(maxWait).until(() ->
            get(statusUrl).then()
                .statusCode(200)
                .body(anyOf(
                    hasXPath("/ExecuteResponse/Status/ProcessSucceeded"),
                    hasXPath("/ExecuteResponse/Status/ProcessFailed")))
        );

        return statusUrl;
    }

}
