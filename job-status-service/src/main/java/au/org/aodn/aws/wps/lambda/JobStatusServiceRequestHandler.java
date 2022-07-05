package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.wps.Storage;
import au.org.aodn.aws.util.*;
import au.org.aodn.aws.wps.status.JobStatusFormatEnum;
import au.org.aodn.aws.wps.JobStatusRequest;
import au.org.aodn.aws.wps.JobStatusRequestParameterParser;
import au.org.aodn.aws.wps.JobStatusResponse;
import au.org.aodn.aws.wps.status.QueuePosition;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.JobDetail;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.StatusType;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobStatusServiceRequestHandler implements RequestHandler<JobStatusRequest, JobStatusResponse> {

    private static final JobStatusFormatEnum DEFAULT_FORMAT = JobStatusFormatEnum.XML;

    private static final String ACCEPTED_STATUS_DESCRIPTION = "Job accepted";
    private static final String FAILED_STATUS_DESCRIPTION = "Job failed";
    private static final String STARTED_STATUS_DESCRIPTION = "Job processing started";
    private static final String SUCCEEDED_STATUS_DESCRIPTION = "Download ready";
    private static final String PAUSED_STATUS_DESCRIPTION = "Job processing paused";
    private static final String UNKNOWN_STATUS_DESCRIPTION = "Job status unknown";
    private static final String JOB_STATUS_HTML_TEMPLATE = "/templates/job_status_html_template.ftl";
    private static final String JOB_QUEUE_HTML_TEMPLATE = "/templates/job_queue_html_template.ftl";

    private Logger LOGGER = LoggerFactory.getLogger(JobStatusServiceRequestHandler.class);

    private String statusFilename = WpsConfig.getProperty(WpsConfig.STATUS_S3_FILENAME_CONFIG_KEY);
    private String jobFileS3KeyPrefix = WpsConfig.getProperty(WpsConfig.AWS_BATCH_JOB_S3_KEY_PREFIX);
    private String statusS3Bucket = WpsConfig.getProperty(WpsConfig.OUTPUT_S3_BUCKET_CONFIG_KEY);
    private String requestFilename = WpsConfig.getProperty(WpsConfig.REQUEST_S3_FILENAME_CONFIG_KEY);

    protected Storage<S3Object> storage = new S3Storage();

    @Override
    public JobStatusResponse handleRequest(JobStatusRequest request, Context context) {

        JobStatusResponse response;
        JobStatusResponse.ResponseBuilder responseBuilder = new JobStatusResponse.ResponseBuilder();
        String responseBody = null;

        JobStatusRequestParameterParser parameterParser = new JobStatusRequestParameterParser(request);
        String jobId = parameterParser.getJobId();
        String format = parameterParser.getFormat();

        LOGGER.info("Parameters passed: JOBID [" + jobId + "], FORMAT [" + format + "]");


        //  Determine the format to send the response in
        JobStatusFormatEnum requestedStatusFormat = getRequestedStatusFormat(format);

        int httpStatus;
        //  Display the QUEUE view
        if (requestedStatusFormat.equals(JobStatusFormatEnum.QUEUE)) {

            AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();
            String queueName = WpsConfig.getProperty(WpsConfig.AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY);

            try {
                responseBody = generateQueueViewHTML(batchClient, queueName);
                httpStatus = HttpStatus.SC_OK;
            }
            catch(IOException | TemplateException ex)
            {
                String errorMessage = "Problem loading queue HTML template: " + ex.getMessage();
                //  Bad stuff - blow up!
                LOGGER.error(errorMessage, ex);
                responseBody = errorMessage;
                httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            }

        } else {

            if (requestedStatusFormat.equals(JobStatusFormatEnum.HTML) || requestedStatusFormat.equals(JobStatusFormatEnum.ADMIN)) {

                ExecuteResponse executeResponse = JobFileUtil.getExecuteResponse(storage, jobFileS3KeyPrefix, jobId, statusFilename, statusS3Bucket);
                String statusDescription = null;

                LOGGER.info("HTML output format requested.  Running transform.");
                boolean adminInfoRequested = false;
                if (requestedStatusFormat.equals(JobStatusFormatEnum.ADMIN)) {
                    adminInfoRequested = true;
                }

                if(executeResponse != null) {
                    StatusType currentStatus = executeResponse.getStatus();
                    //  Get a friendly description of the status
                    statusDescription = getWpsStatusDescription(currentStatus);
                } else {
                    statusDescription = "Unknown status. Error retrieving status. Job ID [" + jobId + "] not found.";
                }

                try {
                    responseBody = generateStatusHTML(executeResponse, statusDescription, jobId, adminInfoRequested);
                    httpStatus = HttpStatus.SC_OK;
                } catch (TemplateException | IOException ex) {
                    LOGGER.error("Exception while generating HTML output.", ex);
                    httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    responseBody = "Unable to generate HTML output";
                }
            } else {  //  Default format = XML

                String executeResponseString = JobFileUtil.getExecuteResponseString(storage, jobFileS3KeyPrefix, jobId, statusFilename, statusS3Bucket);
                if (executeResponseString != null) {
                    responseBody = executeResponseString;
                    LOGGER.info("Retrieved status file XML.");
                    httpStatus = HttpStatus.SC_OK;
                } else {
                    responseBody = "Unable to retrieve status file XML for Job ID [" + jobId + "]";
                    LOGGER.info(responseBody);
                    httpStatus = HttpStatus.SC_NOT_FOUND;
                }
            }


            /**  PARKED UNTIL WE CAN RELIABLY DETERMINE THE QUEUE POSITION OF
             *   THE JOB USING THE BATCH API.
             *   CURRENTLY THE AWS QUEUES SEEM TO BE NON-FIFO : SO IMPOSSIBLE TO
             *   DETERMINE THE ORDER IN WHICH QUEUED JOBS WILL EXECUTE!!
             *
             //  If the ExecuteResponse indicates that the job has been accepted but not
             //  started, completed or failed - then we will update the position indicator.


             if(AWSBatchUtil.isJobWaiting(currentStatus)) {
             AWSBatch batchClient = AWSBatchClientBuilder.defaultClient();

             LOGGER.info("Updating XML with progress description for jobId [" + jobId + "]");

             //  Perform a queue position lookup + insert the position information into the XML
             //  All we have to do is setProcessAccepted to a string that includes some queue
             //  position information.
             String jobProgressDescription = getProgressDescription(batchClient, jobId);

             LOGGER.info("Progress description: " + jobProgressDescription);

             if(jobProgressDescription != null) {
             currentStatus.setProcessAccepted(currentStatus.getProcessAccepted() + " " + jobProgressDescription);
             executeResponse.setStatus(currentStatus);
             }

             responseBody = JobFileUtil.createXmlDocument(executeResponse);
             } else {
             //  Return unaltered status XML
             responseBody = statusXMLString;
             }
             **/
        }

        //  Build the response
        responseBuilder.header("Content-Type", requestedStatusFormat.mimeType());
        responseBuilder.body(responseBody);
        responseBuilder.statusCode(httpStatus);
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
     * @return
     */
    private String getProgressDescription(String jobId) {

        String description = null;
        JobDetail jobDetail = AWSBatchUtil.getJobDetail(jobId);

        if (jobDetail != null && jobDetail.getJobId() != null) {

            //  Determine the queue that the job has been assigned to
            String queueName = jobDetail.getJobQueue();
            LOGGER.info("Queue name for jobId [" + jobDetail.getJobId() + "] = " + queueName);

            if (queueName != null) {
                //  Determine the position of the job in the queue
                QueuePosition queuePosition = AWSBatchUtil.getQueuePosition(jobDetail);

                if (queuePosition != null) {
                    description = "Queue position " + queuePosition.getPosition() + " of " + queuePosition.getNumberInQueue();
                }
            }
        }

        return description;
    }



    private String generateStatusHTML(ExecuteResponse response, String statusDescription, String jobId, boolean includeAdminDetails) throws TemplateException, IOException {

        Map<String, Object> params = new HashMap<>();

        params.put("jobId", jobId);
        params.put("statusDescription", statusDescription);
        params.put("executeResponse", response);


        //  If we can't determine the submission timestamp pass a -1.
        //  The javascript generated by the Freemarker template will recognise this as an
        // unknown timestamp.
        long unixTimestampSeconds = -1;

        try {
            //  Use the request.xml we write to S3 on accepting a job to determine the
            //  submission time of the job
            String requestFileS3Key = jobFileS3KeyPrefix + jobId + "/" + requestFilename;
            LOGGER.info("Request file bucket [" + statusS3Bucket + "], Key [" + requestFileS3Key + "]");
            S3Object requestS3Object = storage.getObject(statusS3Bucket, requestFileS3Key);

            if (requestS3Object != null) {
                long lastModifiedTimestamp = requestS3Object.getObjectMetadata().getLastModified().getTime();
                unixTimestampSeconds = lastModifiedTimestamp;
                params.put("submittedTime", unixTimestampSeconds + "");

                LOGGER.info("Request xml file timestamp = " + unixTimestampSeconds);

                //  Pass a couple of extra parameters (requestXML + logFileLink) to the template if the admin view is requested
                if(includeAdminDetails) {
                    //  Generate request summary details
                    LOGGER.info("Generating request summary HTML for request [" + jobId + "]");
                    String requestSummary = getRequestSummary(requestS3Object);
                    if(requestSummary != null) {
                        params.put("requestXML", "" + requestSummary);
                    }

                    String logFileLink = getBatchLogFileLink(jobId);
                    LOGGER.info("Adding log file link to status page: " + logFileLink);
                    if(logFileLink != null) {
                        params.put("logFileLink", logFileLink);
                    }
                }
            }

        } catch(Exception ex) {
            LOGGER.error("Unable to determine submission time for job [" + jobId + "]: " + ex.getMessage(), ex);
        }




        return runFreemarkerTemplate(JOB_STATUS_HTML_TEMPLATE, params);
    }



    private String getWpsStatusDescription(StatusType currentStatus) {

        if(currentStatus != null) {
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


    private String getRequestSummary(S3Object requestFileObject) {

        //  Currently just returns the content of the request file.
        //  Can be expanded to provide a richer HTML summary of the request for
        //  admin purposes (possible including links to logs etc).

        try (S3ObjectInputStream requestInputStream = requestFileObject.getObjectContent()) {

            if (requestFileObject != null) {

                StringBuilder requestStringBuilder = new StringBuilder();
                int chunkSizeBytes = 1024;
                byte[] inBytes = new byte[chunkSizeBytes];
                int bytesRead;
                while((bytesRead = requestInputStream.read(inBytes)) > 0) {
                    String inChunk = new String(inBytes, 0, bytesRead);
                    requestStringBuilder.append(inChunk);
                }

                return requestStringBuilder.toString();
            } else {
                LOGGER.error("Request S3 object null.");
            }

        } catch (IOException ioex) {
            LOGGER.error("Unable to read request file. Bucket [" + requestFileObject.getBucketName() + "]: " + ioex.getMessage(), ioex);
        }

        return null;
    }


    private String generateQueueViewHTML(AWSBatch batchClient, String queueName) throws IOException, TemplateException {

        LOGGER.info("Querying jobs for queue [" + queueName + "]");

        List<JobDetail> waitingJobDetails = AWSBatchUtil.getJobDetails(batchClient, queueName, AWSBatchUtil.waitingQueueStatuses);
        if(waitingJobDetails != null) {
            LOGGER.info("Waiting jobs: " + waitingJobDetails.size());
        }

        List<JobDetail> runningJobDetails = AWSBatchUtil.getJobDetails(batchClient, queueName, AWSBatchUtil.runningQueueStatuses);
        if(runningJobDetails != null) {
            LOGGER.info("Running jobs: " + runningJobDetails.size());
        }

        List<JobDetail> completedJobDetails = AWSBatchUtil.getJobDetails(batchClient, queueName, AWSBatchUtil.completedQueueStatuses);
        if(completedJobDetails != null) {
            LOGGER.info("Completed jobs: " + completedJobDetails.size());
        }


        //  Build parameters for passing to Freemarker template
        Map<String, Object> params = new HashMap<String, Object>();

        params.put("queueName", queueName);
        params.put("statusServiceBaseLink", WpsConfig.getBaseStatusServiceAdminLink());

        if (waitingJobDetails != null) {
            params.put("queuedJobsList", waitingJobDetails);
        }

        if (runningJobDetails != null) {
            //  Add a log link to the job detail for display on the queue HTML page
            ArrayList<ExtendedJobDetail> extendedJobDetailList = new ArrayList<>();

            for(JobDetail currentJobDetail : runningJobDetails) {
                ExtendedJobDetail extendedJobDetail = new ExtendedJobDetail();
                extendedJobDetail.setAwsBatchJobDetail(currentJobDetail);
                extendedJobDetail.setLogFileLink(getBatchLogFileLink(currentJobDetail));
                extendedJobDetailList.add(extendedJobDetail);
            }

            params.put("runningJobsList", extendedJobDetailList);
        }

        if (completedJobDetails != null) {
            //  Determine the full status of a completed job.
            //  This involves looking up the WPS status file for the job + adding that to the AWS batch status information.
            //  For completed jobs we'll add a WPS status description & a log link
            ArrayList<ExtendedJobDetail> extendedJobDetailList = new ArrayList<>();

            for(JobDetail currentJobDetail : completedJobDetails) {
                //  Get the jobs WPS status from the WPS status file.
                ExtendedJobDetail extendedJobDetails = new ExtendedJobDetail();
                extendedJobDetails.setAwsBatchJobDetail(currentJobDetail);
                ExecuteResponse wpsResponse = JobFileUtil.getExecuteResponse(storage, jobFileS3KeyPrefix, currentJobDetail.getJobId(), statusFilename, statusS3Bucket);
                if(wpsResponse != null && wpsResponse.getStatus() != null) {
                    extendedJobDetails.setWpsStatusDescription(getWpsStatusDescription(wpsResponse.getStatus()));
                } else {
                    extendedJobDetails.setWpsStatusDescription("Unknown - could not read status file.");
                }

                extendedJobDetails.setLogFileLink(getBatchLogFileLink(currentJobDetail));
                extendedJobDetailList.add(extendedJobDetails);
            }
            params.put("completedJobsList", extendedJobDetailList);
        }

        //  Run the template
        return runFreemarkerTemplate(JOB_QUEUE_HTML_TEMPLATE, params);
    }


    private String getBatchLogFileLink(String jobId) {
        JobDetail jobDetail = AWSBatchUtil.getJobDetail(jobId);
        return getBatchLogFileLink(jobDetail);
    }


    private String getBatchLogFileLink(JobDetail jobDetail) {
        //  Cloudwatch links are of this form:
        //  https://ap-southeast-2.console.aws.amazon.com/cloudwatch/home?region=ap-southeast-2#logEventViewer:group=/aws/batch/job;stream=JavaDuckSmall1-dev-cam/default/7714fa46-0b24-4e21-a4ff-45f1160d1ba0
        //  ie: https://<AWS_REGION>.console.aws.amazon.com/cloudwatch/home?region=<AWS_REGION>#logEventViewer:group=<LOG_GROUP_NAME>;stream=<LOG_STREAM_NAME>/default/<JOB_ID>
        String awsRegion = WpsConfig.getProperty(WpsConfig.AWS_REGION_CONFIG_KEY);
        String logGroup = WpsConfig.getProperty(WpsConfig.AWS_BATCH_LOG_GROUP_NAME_CONFIG_KEY);
        String logStream = jobDetail.getContainer().getLogStreamName();
        if(logStream != null) {
            String logUrl = "https://" + awsRegion + ".console.aws.amazon.com/cloudwatch/home?region=" + awsRegion + "#logEventViewer:group=" + logGroup + ";stream=" + logStream;
            return logUrl;
        }

        LOGGER.info("Unable to get log file link for job [" + jobDetail.getJobId() + "]. Region [" + awsRegion + "], LogGroup [" + logGroup + "], LogStream [" + logStream + "]");
        return null;
    }


    private String runFreemarkerTemplate(String templatePath, Map<String, Object> templateParams) throws TemplateException, IOException {

        //  Invoke freemarker template
        try (InputStream contentStream = this.getClass().getResourceAsStream(templatePath)) {

            //  read file to String
            String templateString = Utils.inputStreamToString(contentStream);

            StringTemplateLoader stringLoader = new StringTemplateLoader();
            stringLoader.putTemplate("StringTemplate", templateString);

            Configuration config = new Configuration();
            config.setClassForTemplateLoading(JobStatusServiceRequestHandler.class, "");
            config.setObjectWrapper(new DefaultObjectWrapper());
            config.setTemplateLoader(stringLoader);
            config.setDefaultEncoding("UTF-8");
            config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

            Map<String, Object> params = new HashMap<String, Object>();

            for(String key : templateParams.keySet()) {
                params.put(key, templateParams.get(key));
            }

            //  Run the freemarker template
            Template template = config.getTemplate("StringTemplate");

            LOGGER.info("Loaded template [" + templatePath + "]");
            StringWriter out = new StringWriter();

            template.process(params, out);

            LOGGER.info("Ran template.");

            return out.toString();

        } catch (IOException | TemplateException ex) {
            LOGGER.error("Exception running template:", ex);
            throw ex;
        }
    }


    private JobStatusFormatEnum getRequestedStatusFormat(String format) {

        JobStatusFormatEnum requestedFormat;

        if (format != null) {
            try {
                requestedFormat = JobStatusFormatEnum.valueOf(format.toUpperCase());
                LOGGER.info("Valid job status format requested : " + requestedFormat.name());
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
                requestedFormat = DEFAULT_FORMAT;
            }
        } else {
            LOGGER.info("No format parameter passed.  Defaulting to [" + DEFAULT_FORMAT.name() + "]");
            requestedFormat = DEFAULT_FORMAT;
        }

        return requestedFormat;
    }
}
