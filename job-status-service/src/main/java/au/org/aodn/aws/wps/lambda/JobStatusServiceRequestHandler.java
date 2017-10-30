package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.util.S3Utils;
import au.org.aodn.aws.wps.status.JobStatusFormatEnum;
import au.org.aodn.aws.wps.JobStatusRequest;
import au.org.aodn.aws.wps.JobStatusRequestParameterParser;
import au.org.aodn.aws.wps.JobStatusResponse;
import au.org.aodn.aws.wps.status.QueuePosition;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.DescribeJobsRequest;
import com.amazonaws.services.batch.model.DescribeJobsResult;
import com.amazonaws.services.batch.model.JobDetail;
import com.amazonaws.services.batch.model.JobStatus;
import com.amazonaws.services.batch.model.JobSummary;
import com.amazonaws.services.batch.model.ListJobsRequest;
import com.amazonaws.services.batch.model.ListJobsResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.StringInputStream;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.StatusType;
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
import java.time.Instant;
import java.util.ArrayList;

public class JobStatusServiceRequestHandler implements RequestHandler<JobStatusRequest, JobStatusResponse> {

    private static final JobStatusFormatEnum DEFAULT_FORMAT = JobStatusFormatEnum.XML;

    private static final String ACCEPTED_STATUS_DESCRIPTION = "Job accepted";
    private static final String FAILED_STATUS_DESCRIPTION = "Job failed";
    private static final String STARTED_STATUS_DESCRIPTION = "Job processing started";
    private static final String SUCCEEDED_STATUS_DESCRIPTION = "Download ready";
    private static final String PAUSED_STATUS_DESCRIPTION = "Job processing paused";
    private static final String UNKNOWN_STATUS_DESCRIPTION = "Job status unknown";

    private Logger LOGGER = LoggerFactory.getLogger(JobStatusServiceRequestHandler.class);

    @Override
    public JobStatusResponse handleRequest(JobStatusRequest request, Context context) {

        JobStatusResponse response;
        JobStatusResponse.ResponseBuilder responseBuilder = new JobStatusResponse.ResponseBuilder();
        String responseBody;

        JobStatusRequestParameterParser parameterParser = new JobStatusRequestParameterParser(request);
        String jobId = parameterParser.getJobId();
        String format = parameterParser.getFormat();

        LOGGER.info("Parameters passed: JOBID [" + jobId + "], FORMAT [" + format + "]");

        //  Determine the format to send the response in
        JobStatusFormatEnum requestedStatusFormat = null;

        if (format != null) {

            try {
                requestedStatusFormat = JobStatusFormatEnum.valueOf(format.toUpperCase());
                LOGGER.info("Valid job status format requested : " + requestedStatusFormat.name());
            } catch (IllegalArgumentException iae) {
                String jobStatusValuesString = "";

                JobStatusFormatEnum[] validJobStatuses = JobStatusFormatEnum.values();
                for (int index = 0; index <= validJobStatuses.length - 1; index++) {
                    jobStatusValuesString += validJobStatuses[index].name();
                    if (index + 1 <= validJobStatuses.length - 1) {
                        jobStatusValuesString += ", ";
                    }
                }
                LOGGER.error("UNKNOWN job status format requested [" + format + "].  Supported values : [" + jobStatusValuesString + "].  Defaulting to [" + DEFAULT_FORMAT.name() + "]");
                requestedStatusFormat = DEFAULT_FORMAT;
            }
        } else {
            LOGGER.info("No format parameter passed.  Defaulting to [" + DEFAULT_FORMAT.name() + "]");
            requestedStatusFormat = DEFAULT_FORMAT;
        }


        //  Read the status file for the jobId passed (if it exists)
        try {

            String statusFilename = WpsConfig.getConfig(WpsConfig.STATUS_S3_FILENAME_CONFIG_KEY);
            String jobPrefix = WpsConfig.getConfig(WpsConfig.AWS_BATCH_JOB_S3_KEY);
            String statusS3Bucket = WpsConfig.getConfig(WpsConfig.STATUS_S3_BUCKET_CONFIG_KEY);

            String s3Key = jobPrefix + jobId + "/" + statusFilename;

            //  Check for the existence of the status document
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            boolean statusExists = s3Client.doesObjectExist(statusS3Bucket, s3Key);

            LOGGER.info("Status file exists for jobId [" + jobId + "]? " + statusExists);

            //  If the status file exists and the job is in an 'waiting' state (we have accepted the job but processing
            //  has not yet commenced) we will attempt to work out the queue position of the job and add that to
            //  the status information we send back to the caller.  If the job is being processed or processing has
            //  completed (successful or failed), then we will return the information contained in the status file unaltered.
            if (statusExists) {

                String statusXMLString = S3Utils.readS3ObjectAsString(statusS3Bucket, s3Key, null);

                //  Read the status document
                ExecuteResponse currentResponse = JobFileUtil.unmarshallExecuteResponse(statusXMLString);

                LOGGER.info("Unmarshalled XML for jobId [" + jobId + "]");
                LOGGER.info(statusXMLString);

                //  If the ExecuteResponse indicates that the job has been accepted but not
                //  started, completed or failed - then we will update the position indicator.
                StatusType currentStatus = currentResponse.getStatus();
                AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();



                if(isJobWaiting(currentStatus)) {

                    LOGGER.info("Updating XML with progress description for jobId [" + jobId + "]");

                    //  Perform a queue position lookup + insert the position information into the XML
                    //  All we have to do is setProcessAccepted to a string that includes some queue
                    //  position information.
                    String jobProgressDescription = getProgressDescription(batchClient, jobId);

                    LOGGER.info("Progress description: " + jobProgressDescription);

                    if(jobProgressDescription != null) {
                        currentStatus.setProcessAccepted(currentStatus.getProcessAccepted() + " " + jobProgressDescription);
                        currentResponse.setStatus(currentStatus);
                    }

                    responseBody = JobFileUtil.createXmlDocument(currentResponse);
                } else {
                    //  Return unaltered status XML
                    responseBody = statusXMLString;
                }


                //  If requested output format is HTML - perform a transform on the XML
                if (requestedStatusFormat.equals(JobStatusFormatEnum.HTML)) {

                    LOGGER.info("HTML output format requested.  Running transform.");
                    responseBody = generateHTML(currentResponse, batchClient, jobId);
                }


                //  Build the response
                responseBuilder.isBase64Encoded(false);
                responseBuilder.header("Content-Type", requestedStatusFormat.mimeType());
                responseBuilder.statusCode(HttpStatus.SC_OK);
                responseBuilder.body(responseBody);

            } else {

                //  TODO:  output XML or HTML error information
                String notFoundResponseBody = "Not found.  Job ID [" + jobId + "]";
                LOGGER.info(notFoundResponseBody);
                //  Status document was not found in the S3 bucket
                responseBuilder.statusCode(HttpStatus.SC_OK);
                responseBuilder.body(notFoundResponseBody);
            }
        } catch (Exception ex) {
            String message = "Exception retrieving status of job [" + jobId + "]: " + ex.getMessage();

            //  TODO: output XML or HTML error information
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


    /**
     * Get a text description of the progress of the job.
     * Currently this description indicates the position of the AWS batch
     * job in the processing queue in the form
     * 'Queue position <POSITION_OF_THE_JOB> of <TOTAL_NUMBER_OF_JOBS_QUEUED>'
     *
     * @param jobId
     * @param batchClient
     * @return
     */
    private String getProgressDescription(AWSBatch batchClient, String jobId) {

        String description = null;
        JobDetail jobDetail = getJobDetail(batchClient, jobId);

        if (jobDetail != null && jobDetail.getJobId() != null) {

            //  Determine the queue that the job has been assigned to
            String queueName = jobDetail.getJobQueue();
            LOGGER.info("Queue name for jobId [" + jobDetail.getJobId() + "] = " + queueName);

            if (queueName != null) {
                //  Determine the position of the job in the queue
                QueuePosition queuePosition = getQueuePosition(batchClient, jobDetail);

                if (queuePosition != null) {
                    description = "Queue position " + queuePosition.getPosition() + " of " + queuePosition.getNumberInQueue();
                }
            }
        }

        return description;
    }

    /**
     *
     * @param batchClient
     * @param jobDetail
     * @return
     */
    private QueuePosition getQueuePosition(AWSBatch batchClient, JobDetail jobDetail) {

        ArrayList<JobSummary> allJobs = new ArrayList<>();

        ListJobsRequest submittedJobsRequest = new ListJobsRequest();
        submittedJobsRequest.setJobStatus(JobStatus.SUBMITTED);
        submittedJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult submittedJobsResult = batchClient.listJobs(submittedJobsRequest);
        LOGGER.info("# SUBMITTED jobs: " + submittedJobsResult.getJobSummaryList().size());
        allJobs.addAll(submittedJobsResult.getJobSummaryList());


        ListJobsRequest pendingJobsRequest = new ListJobsRequest();
        pendingJobsRequest.setJobStatus(JobStatus.PENDING);
        pendingJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult pendingJobsResult = batchClient.listJobs(pendingJobsRequest);
        LOGGER.info("# PENDING jobs: " + pendingJobsResult.getJobSummaryList().size());
        allJobs.addAll(pendingJobsResult.getJobSummaryList());


        ListJobsRequest runnableJobsRequest = new ListJobsRequest();
        runnableJobsRequest.setJobStatus(JobStatus.RUNNABLE);
        runnableJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult runnableJobsResult = batchClient.listJobs(runnableJobsRequest);
        LOGGER.info("# RUNNABLE jobs: " + runnableJobsResult.getJobSummaryList().size());
        allJobs.addAll(runnableJobsResult.getJobSummaryList());

        ListJobsRequest startingJobsRequest = new ListJobsRequest();
        startingJobsRequest.setJobStatus(JobStatus.STARTING);
        startingJobsRequest.setJobQueue(jobDetail.getJobQueue());

        ListJobsResult startingJobsResult = batchClient.listJobs(startingJobsRequest);
        LOGGER.info("# STARTING jobs: " + startingJobsResult.getJobSummaryList().size());
        allJobs.addAll(startingJobsResult.getJobSummaryList());

        LOGGER.info("TOTAL JOBS : " + allJobs.size());

        int jobIndex = -1;
        JobSummary[] jobSummaries = new JobSummary[allJobs.size()];
        jobSummaries = allJobs.toArray(jobSummaries);
        for (int index = 0; index <= jobSummaries.length - 1; index++) {
            LOGGER.info("Search queue : jobId [" + jobSummaries[index].getJobId() + "], Match? [" + jobSummaries[index].getJobId().equalsIgnoreCase(jobDetail.getJobId()) + "]");

            if (jobSummaries[index].getJobId().equalsIgnoreCase(jobDetail.getJobId())) {
                jobIndex = index;
                LOGGER.info("Found Job at index [" + jobIndex + "] in queue");
            }
        }

        int jobPosition = 0;
        if (jobIndex >= 0) {
            jobPosition = jobIndex + 1;
        }

        return new QueuePosition(jobPosition, allJobs.size());
    }


    private JobDetail getJobDetail(AWSBatch batchClient, String jobId) {

        if (batchClient != null && jobId != null) {


            try {
                DescribeJobsRequest describeRequest = new DescribeJobsRequest();
                ArrayList<String> jobList = new ArrayList<>();
                jobList.add(jobId);
                describeRequest.setJobs(jobList);

                DescribeJobsResult describeResult = batchClient.describeJobs(describeRequest);

                if (describeResult != null && describeResult.getJobs().size() > 0) {
                    return describeResult.getJobs().get(0);
                }
            } catch (Exception ex) {
                LOGGER.error("Unable to determine the queue for jobId [" + jobId + "]", ex);
            }
        }

        return null;
    }


    private String generateHTML(ExecuteResponse status, AWSBatch batchClient, String jobId) {
        // Create Transformer
        TransformerFactory tf = TransformerFactory.newInstance();
        String xslString;

        String configS3Bucket = WpsConfig.getConfig(WpsConfig.STATUS_SERVICE_CONFIG_S3_BUCKET_CONFIG_KEY);
        String xslS3Key = WpsConfig.getConfig(WpsConfig.STATUS_HTML_XSL_S3_KEY_CONFIG_KEY);

        //  TODO: cater for ExceptionReport responses

        try {
            //  Read XSL from S3
            xslString = S3Utils.readS3ObjectAsString(configS3Bucket, xslS3Key, null);
            StringInputStream xslInputStream = new StringInputStream(xslString);
            StreamSource xslt = new StreamSource(xslInputStream);

            Transformer transformer = tf.newTransformer(xslt);

            //  Get a friendly description of the status
            String statusDescription = getStatusDescription(status);
            long unixTimestamp;
            JobDetail jobDetail = getJobDetail(batchClient, jobId);

            //  Pass in the jobId
            if(jobDetail != null && jobDetail.getJobId() != null) {
                //  Get the timestamp from the AWS batch API
                //  getCreateAt returns milliseconds - we want to pass the seconds since epoch
                unixTimestamp = jobDetail.getCreatedAt() / 1000;
                jobId = jobDetail.getJobId();
                Instant instant = Instant.ofEpochSecond(unixTimestamp/1000);
                LOGGER.info("Unix timestamp = " + unixTimestamp);
                LOGGER.info("Instant of submission = " + instant.toString());
            } else {
                //  Pass a -1 to indicate that we don't know the submit
                //  time.  The javascript generated by the XSLT will
                //  recognise this as an unknown timestamp.
                unixTimestamp = -1;
            }

            //  Pass the job ID & status description
            transformer.setParameter("jobid", jobId);
            transformer.setParameter("statusDescription", statusDescription);
            //  Pass the unix timestamp to the XSLT - which will render the date in the
            //  locale of the browser.  Passed as seconds since epoch.
            transformer.setParameter("submittedTime", "" + unixTimestamp);

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


    private boolean isJobWaiting(StatusType currentStatus) {
        //  WPS status will be ProcessAccepted from the time the job is submitted & when it is
        //  picked up for processing.
        if (currentStatus.isSetProcessAccepted() &&
                (!currentStatus.isSetProcessFailed() && !currentStatus.isSetProcessStarted() && !currentStatus.isSetProcessSucceeded())) {
            return true;
        }
        return false;
    }


    private String getStatusDescription(ExecuteResponse status) {
        StatusType currentStatus = status.getStatus();

        if(status != null) {
            if (currentStatus.isSetProcessAccepted()) {
                return ACCEPTED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessStarted()) {
                return STARTED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessSucceeded()) {
                return SUCCEEDED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessFailed()) {
                return FAILED_STATUS_DESCRIPTION;
            } else if (currentStatus.isSetProcessPaused()) {
                return PAUSED_STATUS_DESCRIPTION;
            }
        }
        return UNKNOWN_STATUS_DESCRIPTION;
    }
}
