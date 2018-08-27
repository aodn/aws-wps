package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.geonetwork.CatalogueReader;
import au.org.aodn.aws.geoserver.client.SubsetParameters;
import au.org.aodn.aws.util.EmailService;
import au.org.aodn.aws.util.JobFileUtil;
import au.org.aodn.aws.wps.request.ExecuteRequestHelper;
import au.org.aodn.aws.wps.status.*;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.SubmitJobRequest;
import com.amazonaws.services.batch.model.SubmitJobResult;
import net.opengis.wps.v_1_0_0.Execute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static au.org.aodn.aws.wps.status.WpsConfig.*;

public class ExecuteOperation implements Operation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteOperation.class);
    private static final String LITERAL_INPUT_IDENTIFIER_LAYER = "layer";
    private static final String LITERAL_INPUT_IDENTIFIER_SUBSET = "subset";


    private final Execute executeRequest;

    public ExecuteOperation(Execute executeRequest) {
        this.executeRequest = executeRequest;
    }


    @Override
    public String execute() throws OGCException {

        //  Config items:
        //      queue names
        //      job name
        //      AWS region
        //      status filename
        //      status location
        String outputS3BucketName = WpsConfig.getProperty(OUTPUT_S3_BUCKET_CONFIG_KEY);
        String jobFileS3KeyPrefix = WpsConfig.getProperty(AWS_BATCH_JOB_S3_KEY_PREFIX);
        String statusFileName = WpsConfig.getProperty(STATUS_S3_FILENAME_CONFIG_KEY);
        String requestFileName = WpsConfig.getProperty(REQUEST_S3_FILENAME_CONFIG_KEY);
        String jobName = WpsConfig.getProperty(AWS_BATCH_JOB_NAME_CONFIG_KEY);
        String awsRegion = WpsConfig.getProperty(AWS_REGION_CONFIG_KEY);

        ExecuteRequestHelper helper = new ExecuteRequestHelper(executeRequest);
        String email = helper.getEmail();

        //  Determine the name of the batch job definition to run for the indicated process
        JobSettings jobSettings = JobMapper.getJobSettings(executeRequest);

        LOGGER.info("outputBucketName    : " + outputS3BucketName);
        LOGGER.info("statusFileName      : " + statusFileName);
        LOGGER.info("Request filename    : " + requestFileName);
        LOGGER.info("Job name            : " + jobName);
        LOGGER.info("Job queue name      : " + jobSettings.getJobQueueName());
        LOGGER.info("Job definition name : " + jobSettings.getJobDefinitionName());
        LOGGER.info("AWS region          : " + awsRegion);

        //  If a job definition name wasn't returned, then the process identifier must not be one we support.  Throw an error indicating that.
        if(jobSettings.getJobDefinitionName() == null) {
            //  Throw an error
            throw new OGCException("ProcessError", "Unknown process identifier supplied [" + jobSettings.getProcessIdentifier() + "]");
        }

        LOGGER.info("Execute operation requested. Identifier [" + jobSettings.getProcessIdentifier() + "], Email [" + email + "]");
        LOGGER.info("Submitting job request...");
        SubmitJobRequest submitJobRequest = new SubmitJobRequest();

        //  Invoke the correct AWS batch processing job for the function that is specified in the Execute Operation
        //  TODO: Select the appropriate queue, jobName (mainly for display in AWS console) based on the processIdentifier.
        //  TODO: we only have one job definition so far - but will need some mapping mechanism if/when we have more process types t support
        submitJobRequest.setJobQueue(jobSettings.getJobQueueName());
        submitJobRequest.setJobName(jobName);
        submitJobRequest.setJobDefinition(jobSettings.getJobDefinitionName());  //TODO: either map to correct job def or set vcpus/memory required appropriately

        AWSBatchClientBuilder builder = AWSBatchClientBuilder.standard();
        builder.setRegion(awsRegion);

        AWSBatch client = builder.build();
        SubmitJobResult result = client.submitJob(submitJobRequest);

        String jobId = result.getJobId();

        LOGGER.info("Batch job submitted. JobID [" + jobId + "], Identifier [" + jobSettings.getProcessIdentifier() + "], Email [" + email + "]");
        LOGGER.info("Writing job request file to S3");
        S3JobFileManager s3JobFileManager = new S3JobFileManager(outputS3BucketName, jobFileS3KeyPrefix, jobId);
        try {
            s3JobFileManager.write(JobFileUtil.createXmlDocument(executeRequest), requestFileName, STATUS_FILE_MIME_TYPE);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        String statusDocument;
        ExecuteStatusBuilder statusBuilder = new ExecuteStatusBuilder(jobId, outputS3BucketName, statusFileName);

        try {
            statusDocument = statusBuilder.createResponseDocument(EnumStatus.ACCEPTED, jobSettings.getProcessIdentifier(), null, null, null);
            s3JobFileManager.write(statusDocument, statusFileName, STATUS_FILE_MIME_TYPE);

            if (email != null) {
                ExecuteRequestHelper requestHelper = new ExecuteRequestHelper(executeRequest);
                String layer = requestHelper.getLiteralInputValue(LITERAL_INPUT_IDENTIFIER_LAYER);
                String subset = requestHelper.getLiteralInputValue(LITERAL_INPUT_IDENTIFIER_SUBSET);
                SubsetParameters subsetParams = SubsetParameters.parse(subset);

                String collectionTitle = layer;

                // Lookup collection metadata to get the collection title
                try {
                    //  Read the metadata record for the layer
                    String catalogueURL = WpsConfig.getProperty(GEONETWORK_CATALOGUE_URL_CONFIG_KEY);
                    String layerSearchField = WpsConfig.getProperty(GEONETWORK_CATALOGUE_LAYER_FIELD_CONFIG_KEY);
                    CatalogueReader catalogueReader = new CatalogueReader(catalogueURL, layerSearchField);
                    String metadataResponseXML = catalogueReader.getMetadataSummaryXML(layer);

                    //  Try and determine the collection title
                    if (metadataResponseXML != null && metadataResponseXML.length() > 0) {

                        //  We only need the <metadata> tag and its contents
                        String metadataRecord = catalogueReader.getMetadataNodeContent(metadataResponseXML);

                        if (metadataRecord != null) {

                            collectionTitle = catalogueReader.getCollectionTitle(metadataRecord);
                            LOGGER.info("Metadata collection title: " + collectionTitle);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("Unable to lookup catalogue for layer [" + layer + "]: " + ex.getMessage(), ex);
                }


                EmailService emailService = new EmailService();
                emailService.sendRegisteredJobEmail(email,
                                                    jobId,
                                                    subsetParams,
                                                    collectionTitle);
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            //  Form failed status document
            statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, jobSettings.getProcessIdentifier(),"Failed to create status file : " + e.getMessage(), "StatusFileError", null);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, jobSettings.getProcessIdentifier(), e.getMessage(), "EmailError", null);
        }

        return statusDocument;
    }

}
