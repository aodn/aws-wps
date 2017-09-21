package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.operation.DescribeProcessOperation;
import au.org.aodn.aws.wps.operation.GetCapabilitiesOperation;
import au.org.aodn.aws.wps.operation.Operation;
import au.org.aodn.aws.wps.operation.OperationFactory;
import net.opengis.ows._1.AcceptVersionsType;
import net.opengis.ows._1.CodeType;
import net.opengis.wps._1_0.DescribeProcess;
import net.opengis.wps._1_0.GetCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.StringTokenizer;

public class QueryStringParameterParser implements RequestParser {

    Logger LOGGER = LoggerFactory.getLogger(QueryStringParameterParser.class);

    private final Map<String, String> queryParameters;

    private final String SERVICE_REQUEST_PARAMETER_NAME = "service";
    private final String REQUEST_NAME_PARAMETER_NAME = "request";
    private final String VERSION_REQUEST_PARAMETER_NAME = "version";
    private final String IDENTIFIER_REQUEST_PARAMETER_NAME = "identifier";


    public QueryStringParameterParser(AwsApiRequest request) {
        this.queryParameters = request.getQueryStringParameters();
    }

    @Override
    public Operation getOperation() {
        if(queryParameters.get(REQUEST_NAME_PARAMETER_NAME) != null)
        {

            String request = queryParameters.get(REQUEST_NAME_PARAMETER_NAME);

            LOGGER.info("Request name: " + request);

            Operation operation = OperationFactory.getInstance(request);

            LOGGER.info("Operation returned: " + operation.getClass().getName());

            if (operation instanceof DescribeProcessOperation)
            {
                //  Set relevant parameters
                DescribeProcessOperation describeOperation = (DescribeProcessOperation) operation;

                DescribeProcess describeRequest = describeOperation.getRequest();
                describeRequest.setVersion(queryParameters.get(VERSION_REQUEST_PARAMETER_NAME));
                describeRequest.setService(queryParameters.get(SERVICE_REQUEST_PARAMETER_NAME));
                describeRequest.setLanguage("en");

                LOGGER.info("Identifier param: " + queryParameters.get(IDENTIFIER_REQUEST_PARAMETER_NAME));
                String identifierParamValue = queryParameters.get(IDENTIFIER_REQUEST_PARAMETER_NAME);

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
                                LOGGER.info("Identifier: " + identifierValue);
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
                capabilitiesRequest.setLanguage("en");
                AcceptVersionsType acceptedVersions = new AcceptVersionsType();
                acceptedVersions.getVersion().add(queryParameters.get(VERSION_REQUEST_PARAMETER_NAME));
                capabilitiesRequest.setAcceptVersions(acceptedVersions);
            }

            return operation;
        }

        //  TODO: exception?
        return null;
    }
}
