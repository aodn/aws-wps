package au.org.aodn.aws.wps.operation;

import org.junit.Test;

import java.util.HashMap;

import static au.org.aodn.aws.wps.status.WpsConfig.WPS_ENDPOINT_TEMPLATE_KEY;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.containsString;

public class GetCapabilitiesReaderTest {
    @Test
    public void testRead() throws Exception {
        GetCapabilitiesReader reader = new GetCapabilitiesReader();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(WPS_ENDPOINT_TEMPLATE_KEY, "https://example.com/wps");
        String result = reader.read(parameters);
        assertThat(result, containsString("GetCapabilities"));
    }

}