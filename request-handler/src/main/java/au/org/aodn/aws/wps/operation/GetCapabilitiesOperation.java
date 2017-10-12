package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.wps.status.WpsConfig;
import net.opengis.wps._1_0.GetCapabilities;
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

        //  Read freemarker template location
        String templateS3Bucket = WpsConfig.getConfig(GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY);
        String templateS3Key = WpsConfig.getConfig(GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY);
        String s3Region = WpsConfig.getConfig(AWS_REGION_CONFIG_KEY);
        String geoserverWpsEndpointUrl = WpsConfig.getConfig(GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY);

        GetCapabilitiesReader capabilitiesReader;
        String getCapabilitiesDocument;

        try
        {
            capabilitiesReader = new GetCapabilitiesReader(templateS3Bucket, templateS3Key, s3Region);
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put(GEOSERVER_WPS_ENDPOINT_TEMPLATE_KEY, geoserverWpsEndpointUrl);

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
