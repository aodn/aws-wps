package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.wps.AwsApiRequest;
import au.org.aodn.aws.wps.AwsApiResponse;
import au.org.aodn.aws.wps.AwsApiResponse.ResponseBuilder;
import au.org.aodn.aws.wps.RequestParser;
import au.org.aodn.aws.wps.RequestParserFactory;
import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.wps.operation.Operation;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;


public class WpsLambdaRequestHandler implements RequestHandler<AwsApiRequest, AwsApiResponse> {

    private Logger LOGGER = LoggerFactory.getLogger(WpsLambdaRequestHandler.class);
    public static final String X_FORWARDED_FOR_HTTP_HEADER_KEY = "X-Forwarded-For";

    @Override
    public AwsApiResponse handleRequest(AwsApiRequest request, Context context) {

        ResponseBuilder responseBuilder = new ResponseBuilder();

        Map<String, String> httpHeaders = request.getHeaders();

        //  Log environment variables
        LOGGER.info("Environment Variables");
        for (String key : System.getenv().keySet()) {
            LOGGER.info(String.format("%s = %s", key, System.getenv(key)));
        }

        String ipAddress = getClientIpAddress(request);

        //  Log all HTTP headers
        if(httpHeaders != null) {
            Set<String> httpHeaderKeys = httpHeaders.keySet();
            for(String key : httpHeaderKeys) {
                if(httpHeaders.get(key) != null) {
                    LOGGER.info("  HTTP HEADER : Key [" + key + "], Value [" + httpHeaders.get(key) + "]");
                }
            }
        }


        try {
            RequestParserFactory requestParserFactory = new RequestParserFactory();
            RequestParser requestParser = requestParserFactory.getRequestParser(request);

            if(requestParser != null) {
                Operation operation = requestParser.getOperation();
                if(operation != null) {

                    //  Log the entire request plus associated metadata (operation name, ipaddress) for consumption by SumoLogic
                    LOGGER.info("Operation [" + operation.getClass().getName() + "] requested. " + getRequestDetails(request));
                    String result = operation.execute();
                    LOGGER.info("Executed");
                    responseBuilder.body(result);
                }
                else
                {
                    LOGGER.error("Operation : NULL. " + getRequestDetails(request));
                    responseBuilder.statusCode(500);
                    String exceptionReportString = JobFileUtil.getExceptionReportString("No operation found.", "ExecutionError");
                    responseBuilder.body(exceptionReportString);
                }
            }
            else
            {
                LOGGER.error("Request parser is NULL. " + getRequestDetails(request));
                responseBuilder.statusCode(500);
                String exceptionReportString = JobFileUtil.getExceptionReportString("Unable to build request parser.", "ExecutionError");
                responseBuilder.body(exceptionReportString);
            }
        } catch (OGCException oe) {
            LOGGER.error("Could not handle request. " + getRequestDetails(request), oe);
            responseBuilder.statusCode(500);
            String exceptionReportString = JobFileUtil.getExceptionReportString(oe.getExceptionText(),
                oe.getExceptionCode(), oe.getLocator());
            responseBuilder.body(exceptionReportString);
        } catch (Exception e) {
            LOGGER.error("Exception : " + e.getMessage(), e);
            responseBuilder.statusCode(500);
            String exceptionReportString = JobFileUtil.getExceptionReportString(e.getMessage(), "ExecutionError");
            responseBuilder.body(exceptionReportString);
        }

        responseBuilder.header("Content-Type", "application/xml");

        return responseBuilder.build();
    }


    /**
     *
     * @param request
     * @return
     */
    private String getClientIpAddress(AwsApiRequest request) {

        //  ApiGateway forwards the ipAddress of the caller to lambda in the X-Forwarded-For HTTP header.
        //  The first IP Address in the list of Ips forwarded should be the address of the client who
        //  invoked the function via the ApiGateway
        Map<String, String> httpHeaders = request.getHeaders();

        if(httpHeaders != null) {
            String ipAddressList = httpHeaders.get(X_FORWARDED_FOR_HTTP_HEADER_KEY);

            if (ipAddressList != null) {
                String[] addresses = ipAddressList.split("\\s*,\\s*");
                if(addresses != null && addresses.length > 0) {
                    return addresses[0];
                }
            }
        }

        return null;
    }


    private String getRequestDetails(AwsApiRequest request) {

        String ipAddress = getClientIpAddress(request);
        if(request != null) {
            String getParams = "";
            if (request.getQueryStringParameters() != null) {
                getParams = request.getQueryStringParameters().toString();
            }

            return "IPAddress [" + ipAddress + "], Request body [" + stripRequestBodyWhitespace(request.getBody()) + "], Request params [ " + getParams +"]";
        }

        return null;
    }


    private String stripRequestBodyWhitespace(String requestBody) {

        StringBuilder strippedBody = new StringBuilder();
        if(requestBody != null) {
            LOGGER.info("requestBody length before strip: " + requestBody.length());

            try(StringReader bodyReader = new StringReader(requestBody);
                BufferedReader reader = new BufferedReader(bodyReader)) {

                String line;
                while((line = reader.readLine()) != null) {
                    //  Strip all whitespace from each line
                    strippedBody.append(line.trim());
                }

            } catch(IOException ioex) {
                LOGGER.error("Exception closing reader.", ioex);
            }

            LOGGER.info("requestBody length after strip: " + strippedBody.toString().length());
        }

        return strippedBody.toString();
    }
}
