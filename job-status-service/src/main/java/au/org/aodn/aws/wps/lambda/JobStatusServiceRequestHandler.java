package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.wps.JobStatusRequest;
import au.org.aodn.aws.wps.JobStatusRequestParameterParser;
import au.org.aodn.aws.wps.JobStatusResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobStatusServiceRequestHandler implements RequestHandler<JobStatusRequest, JobStatusResponse> {

    private Logger LOGGER = LoggerFactory.getLogger(JobStatusServiceRequestHandler.class);

    @Override
    public JobStatusResponse handleRequest(JobStatusRequest request, Context context) {

        JobStatusResponse response;
        JobStatusResponse.ResponseBuilder responseBuilder = new JobStatusResponse.ResponseBuilder();

        String responseBody = "Test Response Body";

        JobStatusRequestParameterParser parameterParser = new JobStatusRequestParameterParser(request);
        String jobId = parameterParser.getJobId();
        String format = parameterParser.getFormat();

        LOGGER.info("Parameters passed: JOBID [" + jobId + "], FORMAT [" + format + "]");

        try {

            //  Execute the request
            responseBuilder.isBase64Encoded(false);
            //  TODO: Headers?
            //responseBuilder.header();
            responseBuilder.statusCode(HttpStatus.SC_OK);
            responseBuilder.body(responseBody);


        } catch (Exception ex) {
            String message = "Exception retrieving status of job [" + jobId + "]: " + ex.getMessage();

            //  TODO:  form HTML/XML output for error?
            //  Bad stuff happened
            LOGGER.info(message);
            //  Execute the request
            responseBuilder.isBase64Encoded(false);
            responseBuilder.statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseBuilder.body(message);
        }

        response = responseBuilder.build();

        return response;
    }
}
