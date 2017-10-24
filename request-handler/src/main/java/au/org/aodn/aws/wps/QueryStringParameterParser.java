package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.operation.DescribeProcessOperation;
import au.org.aodn.aws.wps.operation.GetCapabilitiesOperation;
import au.org.aodn.aws.wps.operation.Operation;
import au.org.aodn.aws.wps.operation.OperationFactory;
import au.org.aodn.aws.wps.status.WpsConfig;
import net.opengis.ows.v_1_1_0.AcceptVersionsType;
import net.opengis.ows.v_1_1_0.CodeType;
import net.opengis.wps.v_1_0_0.DescribeProcess;
import net.opengis.wps.v_1_0_0.GetCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class QueryStringParameterParser implements RequestParser {

    Logger LOGGER = LoggerFactory.getLogger(QueryStringParameterParser.class);

    private final Map<String, String> queryParameters;

    private final String REQUEST_NAME_PARAMETER_NAME = "request";
    private final String VERSION_REQUEST_PARAMETER_NAME = "version";
    private final String IDENTIFIER_REQUEST_PARAMETER_NAME = "identifier";

    public QueryStringParameterParser(AwsApiRequest request) {
        this.queryParameters = request.getQueryStringParameters();
    }

    @Override
    public Operation getOperation() {

        if(getMapValueIgnoreCase(REQUEST_NAME_PARAMETER_NAME, queryParameters) != null)
        {
            String request = getMapValueIgnoreCase(REQUEST_NAME_PARAMETER_NAME,queryParameters);

            LOGGER.info("Request name: " + request);

            Operation operation = OperationFactory.getInstance(request);

            LOGGER.info("Operation returned: " + operation.getClass().getName());

            if (operation instanceof DescribeProcessOperation)
            {
                //  Set relevant parameters
                DescribeProcessOperation describeOperation = (DescribeProcessOperation) operation;

                DescribeProcess describeRequest = describeOperation.getRequest();
                describeRequest.setLanguage(WpsConfig.getConfig(WpsConfig.LANGUAGE_KEY));

                String identifierParamValue = getMapValueIgnoreCase(IDENTIFIER_REQUEST_PARAMETER_NAME, queryParameters);
                LOGGER.info("New Identifier param: " + identifierParamValue);

                if(identifierParamValue != null && !identifierParamValue.trim().isEmpty()) {

                    //  It is possible to get multiple identifiers passed in the HTTP request.
                    //  They are passed as a comma-separated value for the identifier HTTP parameter.
                    //  eg:  identifier=gs:NetcdfOutput,gs:GoGoDuck  should result in a ProcessDescription for
                    //  each service appearing in the output.
                    StringTokenizer commaTokenizer = new StringTokenizer(identifierParamValue, ",");

                    if(commaTokenizer.countTokens() > 0) {

                        while(commaTokenizer.hasMoreTokens()) {
                            String identifierValue = commaTokenizer.nextToken();

                            CodeType identifier = new CodeType();

                            if (identifierValue != null) {
                                identifier.setValue(identifierValue);
                            }

                            describeRequest.getIdentifier().add(identifier);
                        }
                    }
                }
            }
            else if (operation instanceof GetCapabilitiesOperation)
            {
                GetCapabilitiesOperation capabilitiesOperation = (GetCapabilitiesOperation) operation;
                GetCapabilities capabilitiesRequest = capabilitiesOperation.getRequest();
                capabilitiesRequest.setLanguage(WpsConfig.getConfig(WpsConfig.LANGUAGE_KEY));
                AcceptVersionsType acceptedVersions = new AcceptVersionsType();
                acceptedVersions.getVersion().add(getMapValueIgnoreCase(VERSION_REQUEST_PARAMETER_NAME,queryParameters));
                capabilitiesRequest.setAcceptVersions(acceptedVersions);
            }

            return operation;
        }
        else
        {
            LOGGER.error("No Operation found.  Request parameter value: " + getMapValueIgnoreCase(REQUEST_NAME_PARAMETER_NAME, queryParameters));
            return null;
        }
    }


    private String getMapValueIgnoreCase(String searchKey, Map<String, String> map)
    {
        //  Look for uppercase or lowercase or mixed case matches
        if(searchKey != null && map != null) {
            Set<String> keys = map.keySet();
            for(String key : keys)
            {
                if(key.toLowerCase().equalsIgnoreCase(searchKey.toLowerCase()))
                {
                    return map.get(key);
                }
            }
        }
        return null;
    }
}
