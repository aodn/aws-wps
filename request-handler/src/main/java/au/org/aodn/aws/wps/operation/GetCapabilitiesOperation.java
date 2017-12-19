package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.wps.status.WpsConfig;
import net.opengis.wps.v_1_0_0.GetCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static au.org.aodn.aws.wps.status.WpsConfig.*;

public class GetCapabilitiesOperation implements Operation {
    private final GetCapabilities request;
    Logger LOGGER = LoggerFactory.getLogger(GetCapabilitiesOperation.class);

    public GetCapabilitiesOperation(GetCapabilities request) {
        this.request = request;
    }

    public GetCapabilities getRequest()
    {
        return this.request;
    }

    @Override
    public String execute() {
        String wpsEndpointUrl = WpsConfig.getProperty(WPS_ENDPOINT_URL_CONFIG_KEY);

        GetCapabilitiesReader capabilitiesReader;
        String getCapabilitiesDocument;

        try
        {
            capabilitiesReader = new GetCapabilitiesReader();
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put(WPS_ENDPOINT_TEMPLATE_KEY, wpsEndpointUrl);

            //  Run the template and return the XML document
            getCapabilitiesDocument = capabilitiesReader.read(parameters);
        }
        catch(Exception ex)
        {
            LOGGER.error("Unable to retrieve GetCapabilities XML: " + ex.getMessage(), ex);
            return JobFileUtil.getExceptionReportString("Unable to retrieve GetCapabilities document: " + ex.getMessage(), "ProcessingError");
        }

        return getCapabilitiesDocument;
    }
}
