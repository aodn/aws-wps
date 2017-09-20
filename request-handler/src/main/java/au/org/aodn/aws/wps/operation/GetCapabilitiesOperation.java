package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.Constants;
import au.org.aodn.aws.wps.exception.ValidationException;
import au.org.aodn.aws.wps.status.StatusHelper;
import au.org.aodn.aws.wps.status.WpsConfig;
import net.opengis.ows._1.CapabilitiesBaseType;
import net.opengis.wps._1_0.GetCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;

public class GetCapabilitiesOperation implements Operation {
    private final GetCapabilities request;
    Logger LOGGER = LoggerFactory.getLogger(GetCapabilitiesOperation.class);

    public GetCapabilitiesOperation(GetCapabilities request) {
        this.request = request;
    }

    @Override
    public String execute(Properties config) {

        //  Read freemarker template location
        String templateS3Bucket = config.getProperty(WpsConfig.GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY);
        String templateS3Key = config.getProperty(WpsConfig.GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY);
        String environmentName = config.getProperty(WpsConfig.ENVIRONMENT_NAME_CONFIG_KEY);
        String templateKey = environmentName + "/" + templateS3Key;
        String s3Region = config.getProperty(WpsConfig.AWS_REGION_CONFIG_KEY);
        String geoserverWpsEndpointUrl = config.getProperty(WpsConfig.GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY);

        LOGGER.info("CONFIG: " + config.toString());

        GetCapabilitiesReader capabilitiesReader = null;
        String getCapabilitiesDocument = null;

        try
        {
            capabilitiesReader = new GetCapabilitiesReader(templateS3Bucket, templateKey, s3Region);
            HashMap<String, String> parameters = new HashMap<String, String>();
            parameters.put(WpsConfig.GEOSERVER_WPS_ENDPOINT_TEMPLATE_KEY, geoserverWpsEndpointUrl);

            //  Run the template and return the XML document
            getCapabilitiesDocument = capabilitiesReader.read(parameters);
        }
        catch(Exception ex)
        {
            LOGGER.error("Unable to retrieve GetCapabilities XML: " + ex.getMessage(), ex);
            return StatusHelper.getExceptionReportString("Unable to retrieve GetCapabilities document: " + ex.getMessage(), "ProcessingError");
        }

        return getCapabilitiesDocument;
    }


    @Override
    public void validate(Properties config) throws ValidationException {
        throw new UnsupportedOperationException(Constants.UNSUPPORTED_METHOD_EXCEPTION_MESSAGE);
    }
}
