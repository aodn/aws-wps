package au.org.emii.wps.it;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.BeforeClass;
import org.junit.Test;

import static au.org.emii.wps.util.Matchers.validateWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;

public class GetCapabilitiesIT {
    private static final String SERVICE_ENDPOINT = System.getenv("WPS_ENDPOINT");

    private static RequestSpecification spec;

    @BeforeClass
    public static void initSpec() {
        spec = new RequestSpecBuilder()
                .setBaseUri(SERVICE_ENDPOINT)
                .addParam("service", "WPS")
                .addParam("request", "GetCapabilities")
                .addFilter(new ResponseLoggingFilter())
                .addFilter(new RequestLoggingFilter())
                .build();
    }

    @Test
    public void testGetCapabilities() {
        given()
            .spec(spec)
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(ContentType.XML)
            .body(validateWith("/wps/1.0.0/wpsAll.xsd"))
            .body("Capabilities.ProcessOfferings.Process.Identifier", hasItem("gs:GoGoDuck"));
    }
}
