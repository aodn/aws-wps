package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.util.S3Utils;
import au.org.aodn.aws.wps.JobStatusFormatEnum;
import au.org.aodn.aws.wps.JobStatusRequest;
import au.org.aodn.aws.wps.JobStatusRequestParameterParser;
import au.org.aodn.aws.wps.JobStatusResponse;
import au.org.aodn.aws.wps.status.QueuePosition;
import au.org.aodn.aws.wps.status.StatusHelper;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.DescribeJobsRequest;
import com.amazonaws.services.batch.model.DescribeJobsResult;
import com.amazonaws.services.batch.model.JobStatus;
import com.amazonaws.services.batch.model.JobSummary;
import com.amazonaws.services.batch.model.ListJobsRequest;
import com.amazonaws.services.batch.model.ListJobsResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.StringInputStream;
import net.opengis.wps._1_0.ExecuteResponse;
import net.opengis.wps._1_0.ProcessFailedType;
import net.opengis.wps._1_0.ProcessStartedType;
import net.opengis.wps._1_0.StatusType;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

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

        JobStatusFormatEnum requestedStatusFormat = null;

        //  TODO: throw error or default to particular format if format not recognized?
        try {
            requestedStatusFormat = JobStatusFormatEnum.valueOf(format.toUpperCase());
            LOGGER.info("Valid job status format requested : " + requestedStatusFormat.name());
        } catch (IllegalArgumentException iae) {
            String jobStatusValuesString = "";

            JobStatusFormatEnum[] validJobStatuses = JobStatusFormatEnum.values();
            for(int index = 0; index <= validJobStatuses.length -1; index++)
            {
                jobStatusValuesString += validJobStatuses[index].name();
                if(index + 1 <= validJobStatuses.length -1)
                {
                    jobStatusValuesString += ", ";
                }
            }
            LOGGER.error("UNKNOWN job status format requested [" + format + "].  Supported values : [" + jobStatusValuesString + "]");
        }



        try {

            String statusFilename = WpsConfig.getConfig(WpsConfig.STATUS_S3_FILENAME_CONFIG_KEY);
            String statusS3Bucket = WpsConfig.getConfig(WpsConfig.STATUS_S3_BUCKET_CONFIG_KEY);

            String s3Key = jobId + "/" + statusFilename;

            //  Check for the existence of the status document
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            boolean statusExists = s3Client.doesObjectExist(statusS3Bucket, s3Key);

            LOGGER.info("Status file exists for jobId [" + jobId + "]? " + statusExists);

            if(statusExists) {

                String statusXMLString = S3Utils.readS3ObjectAsString(statusS3Bucket, s3Key, null);

                //  Read the status document
                ExecuteResponse currentResponse = StatusHelper.unmarshallExecuteResponse(statusXMLString);

                LOGGER.info("Unmarshalled XML for jobId [" + jobId + "]");
                LOGGER.info(statusXMLString);

                //  If the ExecuteResponse indicates that the job has been accepted but not
                //  started, completed or failed - then we will update the position indicator.
                StatusType currentStatus = currentResponse.getStatus();
                String processAccepted = currentStatus.getProcessAccepted();
                ProcessFailedType processFailed = currentStatus.getProcessFailed();
                ProcessStartedType processPaused = currentStatus.getProcessPaused();
                ProcessStartedType processStarted = currentStatus.getProcessStarted();
                String processSucceeded = currentStatus.getProcessSucceeded();

                LOGGER.info("processAccepted null? = " + (processAccepted == null));
                LOGGER.info("processFailed   null? = " + (processFailed == null));
                LOGGER.info("processPaused   null? = " + (processPaused == null));
                LOGGER.info("processStarted  null? = " + (processStarted == null));
                LOGGER.info("processFailed   null? = " + (processFailed == null));

                if(processAccepted != null &&
                        (processFailed == null &&
                         processPaused == null &&
                         processStarted == null &&
                         processSucceeded == null)) {


                    LOGGER.info("Updating XML with progress description for jobId [" + jobId + "]");
                    //  TODO:  hook here to perform a queue position lookup + insert the position information into the XML
                    //  All we have to do is setProcessAccepted to a string that includes some queue
                    //  position information.
                    String jobProgressDescription = getProgressDescription(jobId);

                    LOGGER.info("Progress description: " + jobProgressDescription);

                    currentStatus.setProcessAccepted(currentStatus.getProcessAccepted() + " " + jobProgressDescription);
                    currentResponse.setStatus(currentStatus);

                    responseBody = StatusHelper.createResponseXmlDocument(currentResponse);
                }
                else
                {
                    responseBody = statusXMLString;
                }


                if(requestedStatusFormat.equals(JobStatusFormatEnum.HTML)) {
                    //  TODO: apply HTML transform to XML
                    LOGGER.info("HTML output format requested.  Running transform.");

                    responseBody = generateHTML(currentResponse);
                }

                //  Execute the request
                responseBuilder.isBase64Encoded(false);
                //  TODO: Headers?
                //responseBuilder.header();
                responseBuilder.statusCode(HttpStatus.SC_OK);
                responseBuilder.body(responseBody);

            } else {

                String notFoundResponseBody = "Not found.  Job ID [" + jobId + "]";
                LOGGER.info(notFoundResponseBody);
                //  Status document was not found in the S3 bucket
                responseBuilder.statusCode(HttpStatus.SC_OK);
                responseBuilder.body(notFoundResponseBody);
            }

        } catch (Exception ex) {
            String message = "Exception retrieving status of job [" + jobId + "]: " + ex.getMessage();

            //  TODO:  form HTML/XML output for error?
            //  Bad stuff happened
            LOGGER.error(message, ex);
            //  Execute the request
            responseBuilder.statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseBuilder.body(message);
        }

        responseBuilder.isBase64Encoded(false);
        response = responseBuilder.build();

        return response;
    }


    private String getProgressDescription(String jobId) {

        String description = "";

        if(jobId != null) {
            AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();

            String queueName = getQueueName(batchClient, jobId);

            LOGGER.info("Queue name for jobId [" + jobId + "] = " + queueName);

            if(queueName != null) {
                QueuePosition queuePosition = getQueuePosition(batchClient, jobId, queueName);

                if(queuePosition != null) {
                    description = "Queue position " + queuePosition.getPosition() + " of " + queuePosition.getNumberInQueue();
                }
            }
        }

        return description;
    }

    /**
     *
     * @param jobId
     * @return
     */
    private QueuePosition getQueuePosition(AWSBatch batchClient, String jobId, String queueName) {

        ArrayList<JobSummary> allJobs = new ArrayList<>();

        ListJobsRequest submittedJobsRequest = new ListJobsRequest();
        submittedJobsRequest.setJobStatus(JobStatus.SUBMITTED);
        submittedJobsRequest.setJobQueue(queueName);

        ListJobsResult submittedJobsResult = batchClient.listJobs(submittedJobsRequest);
        LOGGER.info("# SUBMITTED jobs: " + submittedJobsResult.getJobSummaryList().size());
        allJobs.addAll(submittedJobsResult.getJobSummaryList());


        ListJobsRequest pendingJobsRequest = new ListJobsRequest();
        pendingJobsRequest.setJobStatus(JobStatus.PENDING);
        pendingJobsRequest.setJobQueue(queueName);

        ListJobsResult pendingJobsResult = batchClient.listJobs(pendingJobsRequest);
        LOGGER.info("# PENDING jobs: " + pendingJobsResult.getJobSummaryList().size());
        allJobs.addAll(pendingJobsResult.getJobSummaryList());


        ListJobsRequest runnableJobsRequest = new ListJobsRequest();
        runnableJobsRequest.setJobStatus(JobStatus.RUNNABLE);
        runnableJobsRequest.setJobQueue(queueName);

        ListJobsResult runnableJobsResult = batchClient.listJobs(runnableJobsRequest);
        LOGGER.info("# RUNNABLE jobs: " + runnableJobsResult.getJobSummaryList().size());
        allJobs.addAll(runnableJobsResult.getJobSummaryList());

        ListJobsRequest startingJobsRequest = new ListJobsRequest();
        startingJobsRequest.setJobStatus(JobStatus.STARTING);
        startingJobsRequest.setJobQueue(queueName);

        ListJobsResult startingJobsResult = batchClient.listJobs(startingJobsRequest);
        LOGGER.info("# STARTING jobs: " + startingJobsResult.getJobSummaryList().size());
        allJobs.addAll(startingJobsResult.getJobSummaryList());

        LOGGER.info("TOTAL JOBS : " + allJobs.size());

        int jobIndex = -1;
        JobSummary[] jobSummaries = new JobSummary[allJobs.size()];
        jobSummaries = allJobs.toArray(jobSummaries);
        for(int index = 0; index <= jobSummaries.length -1; index ++)
        {
            LOGGER.info("Search queue : jobId [" + jobSummaries[index].getJobId() + "], Match? [" + jobSummaries[index].getJobId().equalsIgnoreCase(jobId) + "]");

            if(jobSummaries[index].getJobId().equalsIgnoreCase(jobId)) {
                jobIndex = index;
                LOGGER.info("Found Job at index [" + jobIndex + "] in queue");
            }
        }

        int jobPosition = 0;
        if(jobIndex >= 0)
        {
            jobPosition = jobIndex + 1;
        }

        return new QueuePosition(jobPosition, allJobs.size());
    }


    private String getQueueName(AWSBatch batchClient, String jobId) {

        if(batchClient != null && jobId != null) {


            try {
                DescribeJobsRequest describeRequest = new DescribeJobsRequest();
                ArrayList<String> jobList = new ArrayList<>();
                jobList.add(jobId);
                describeRequest.setJobs(jobList);

                DescribeJobsResult describeResult = batchClient.describeJobs(describeRequest);

                if (describeResult != null && describeResult.getJobs().size() > 0) {
                    return describeResult.getJobs().get(0).getJobQueue();
                }
            }
            catch(Exception ex) {
                LOGGER.error("Unable to determine the queue for jobId [" + jobId + "]", ex);
            }
        }

        return null;
    }


    private String generateHTML(ExecuteResponse status) {
        // Create Transformer
        TransformerFactory tf = TransformerFactory.newInstance();
        String xslString;

        String configS3Bucket = WpsConfig.getConfig(WpsConfig.STATUS_SERVICE_CONFIG_S3_BUCKET_CONFIG_KEY);
        String xslS3Key = WpsConfig.getConfig(WpsConfig.STATUS_HTML_XSL_S3_KEY_CONFIG_KEY);

        try {
            //  Read XSL from S3
            xslString = S3Utils.readS3ObjectAsString(configS3Bucket, xslS3Key, null);
            StringInputStream xslInputStream = new StringInputStream(xslString);
            StreamSource xslt = new StreamSource(xslInputStream);

            Transformer transformer = tf.newTransformer(xslt);

            // Source
            JAXBContext jc = JAXBContext.newInstance(ExecuteResponse.class);
            JAXBSource source = new JAXBSource(jc, status);

            // Transform
            StringWriter htmlWriter = new StringWriter();
            transformer.transform(source, new StreamResult(htmlWriter));

            return htmlWriter.toString();

        } catch (JAXBException jex) {
            LOGGER.error("Unable to generate JAXB context : " + jex.getMessage(), jex);
        } catch (TransformerException tex) {
            LOGGER.error("Unable to generate JAXB context : " + tex.getMessage(), tex);
        } catch (UnsupportedEncodingException uex) {
            LOGGER.error("Unable to generate JAXB context : " + uex.getMessage(), uex);
        } catch (IOException ioex) {
            LOGGER.error("Unable to read XSL file from S3. Bucket [" + configS3Bucket + "], Key [" + xslS3Key + "]: " + ioex.getMessage(), ioex);
        }

        return null;
    }

}
