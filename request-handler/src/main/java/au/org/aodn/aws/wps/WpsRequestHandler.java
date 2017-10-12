package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.AwsApiResponse.ResponseBuilder;
import au.org.aodn.aws.wps.exception.ValidationException;
import au.org.aodn.aws.wps.operation.Operation;
import au.org.aodn.aws.util.JobFileUtil;
import net.opengis.wps._1_0.ExecuteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;

public class WpsRequestHandler implements RequestHandler, RequestValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsRequestHandler.class);

    public AwsApiResponse handleRequest(AwsApiRequest request) {

        ResponseBuilder responseBuilder = new ResponseBuilder();

        try {
            JAXBContext context = JAXBContext.newInstance(ExecuteResponse.class);
            RequestParserFactory requestParserFactory = new RequestParserFactory(context);
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
        } catch (Exception e) {
            LOGGER.error("Exception : " + e.getMessage(), e);
            responseBuilder.statusCode(500);
            String exceptionReportString = JobFileUtil.getExceptionReportString(e.getMessage(), "ExecutionError");
            responseBuilder.body(exceptionReportString);
        }

        return responseBuilder.build();
    }

    @Override
    public void validate(AwsApiRequest request) throws ValidationException {

    }
}
