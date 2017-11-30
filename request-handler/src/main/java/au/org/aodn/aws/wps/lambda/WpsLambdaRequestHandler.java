package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.wps.AwsApiRequest;
import au.org.aodn.aws.wps.AwsApiResponse;
import au.org.aodn.aws.wps.AwsApiResponse.ResponseBuilder;
import au.org.aodn.aws.wps.RequestParser;
import au.org.aodn.aws.wps.RequestParserFactory;
import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.wps.operation.Operation;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;


public class WpsLambdaRequestHandler implements RequestHandler<AwsApiRequest, AwsApiResponse> {

    private Logger LOGGER = LoggerFactory.getLogger(WpsLambdaRequestHandler.class);


    @Override
    public AwsApiResponse handleRequest(AwsApiRequest request, Context context) {

        ResponseBuilder responseBuilder = new ResponseBuilder();

        Map<String, String> httpHeaders = request.getHeaders();

        CognitoIdentity identity = context.getIdentity();


        if(httpHeaders != null) {
            Set<String> httpHeaderKeys = httpHeaders.keySet();
            for(String key : httpHeaderKeys) {
                if(httpHeaders.get(key) != null) {
                    LOGGER.info("  HTTP HEADER : Key [" + key + "], Value [" + httpHeaders.get(key) + "]");
                }
            }
        }

        if(context.getClientContext() != null) {
            Map<String, String> clientContextEnvironment = context.getClientContext().getEnvironment();
            Map<String, String> clientContextCustom = context.getClientContext().getCustom();

            Set<String> envKeys = clientContextEnvironment.keySet();
            for (String key : envKeys) {
                if (clientContextEnvironment.get(key) != null) {
                    LOGGER.info("  CLIENT ENV : Key [" + key + "], Value [" + clientContextEnvironment.get(key) + "]");
                }
            }

            Set<String> customKeys = clientContextCustom.keySet();
            for (String key : customKeys) {
                if (clientContextCustom.get(key) != null) {
                    LOGGER.info("  CLIENT CUSTOM : Key [" + key + "], Value [" + clientContextCustom.get(key) + "]");
                }
            }
        }

        try {
            RequestParserFactory requestParserFactory = new RequestParserFactory();
            RequestParser requestParser = requestParserFactory.getRequestParser(request);

            if(requestParser != null) {
                Operation operation = requestParser.getOperation();
                if(operation != null) {
                    LOGGER.info("Operation : " + operation.getClass());
                    String result = operation.execute();
                    LOGGER.info("Executed");
                    responseBuilder.body(result);
                }
                else
                {
                    LOGGER.error("Operation : NULL.");
                    responseBuilder.statusCode(500);
                    String exceptionReportString = JobFileUtil.getExceptionReportString("No operation found.", "ExecutionError");
                    responseBuilder.body(exceptionReportString);
                }
            }
            else
            {
                LOGGER.error("Request parser is NULL.");
                responseBuilder.statusCode(500);
                String exceptionReportString = JobFileUtil.getExceptionReportString("Unable to build request parser.", "ExecutionError");
                responseBuilder.body(exceptionReportString);
            }
        } catch (OGCException oe) {
            LOGGER.error("Could not handle request", oe);
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
}
