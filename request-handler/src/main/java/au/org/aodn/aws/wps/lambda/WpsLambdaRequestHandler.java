package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.wps.AwsApiRequest;
import au.org.aodn.aws.wps.AwsApiResponse;
import au.org.aodn.aws.wps.WpsRequestHandler;
import au.org.aodn.aws.util.JobFileUtil;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpsLambdaRequestHandler implements RequestHandler<AwsApiRequest, AwsApiResponse> {

    private Logger LOGGER = LoggerFactory.getLogger(WpsLambdaRequestHandler.class);


    @Override
    public AwsApiResponse handleRequest(AwsApiRequest request, Context context) {

        WpsRequestHandler handler = new WpsRequestHandler();
        AwsApiResponse response;

        try {
            //  Execute the request
            response = handler.handleRequest(request);

        } catch (Exception ex) {
            String message = "Exception running WPS Lambda function: " + ex.getMessage();
            //  Bad stuff happened
            LOGGER.info(message);
            //  Send caller a WPS error response
            AwsApiResponse.ResponseBuilder responseBuilder = new AwsApiResponse.ResponseBuilder();
            responseBuilder.statusCode(500);
            responseBuilder.body(JobFileUtil.getExceptionReportString(message, "WPSError"));
            response = responseBuilder.build();
        }

        return response;
    }
}
