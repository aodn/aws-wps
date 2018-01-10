package au.org.emii.wps.it;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static au.org.emii.wps.util.Matchers.validateWith;

public class DescribeProcessIT {
    private static final String SERVICE_ENDPOINT = System.getenv("WPS_ENDPOINT");

    private static RequestSpecification spec;

    @BeforeClass
    public static void initSpec() {
        spec = new RequestSpecBuilder()
            .setBaseUri(SERVICE_ENDPOINT)
            .addParameter("service", "WPS")
            .addParameter("request", "DescribeProcess")
            .addFilter(new ResponseLoggingFilter())
            .addFilter(new RequestLoggingFilter())
            .build();
    }

    @Test
    public void testDescribeUnknownProcess() throws IOException {
        given()
            .spec(spec)
            .param("identifier", "unknown")
        .when()
            .get()
        .then()
            .statusCode(500)
            .contentType(ContentType.XML)
            .body(validateWith("/ows/1.1.0/owsAll.xsd"))
            .root("ExceptionReport.Exception")
            .body("@exceptionCode", equalTo("InvalidParameterValue"))
            .body("@locator", equalTo("identifieer"))
            .body("ExceptionText", equalTo("No such process 'unknown'"));
    }

    @Test
    public void testDescribeKnownProcess() {
        given()
            .spec(spec)
            .param("identifier", "gs:GoGoDuck")
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(ContentType.XML)
            .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
            .body("ProcessDescriptions.ProcessDescription.Identifier", equalTo("gs:GoGoDuck"));
    }

}
