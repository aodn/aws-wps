package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.exception.ValidationException;
import au.org.aodn.aws.wps.status.EnumStatus;
import au.org.aodn.aws.wps.status.ExecuteStatusBuilder;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.SubmitJobRequest;
import com.amazonaws.services.batch.model.SubmitJobResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.StringInputStream;
import net.opengis.wps._1_0.DataInputsType;
import net.opengis.wps._1_0.DataType;
import net.opengis.wps._1_0.Execute;
import net.opengis.wps._1_0.InputType;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ExecuteOperation implements Operation {

    private static final Logger LOGGER = Logger.getLogger(ExecuteOperation.class);

    //  Configuration key names
    private static final String STATUS_S3_BUCKET_CONFIG_KEY = "STATUS_S3_BUCKET";
    private static final String STATUS_S3_KEY_CONFIG_KEY = "STATUS_S3_FILENAME";
    private static final String AWS_BATCH_JOB_NAME_CONFIG_KEY = "AWS_BATCH_JOB_NAME";
    private static final String AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_JOB_QUEUE_NAME";
    private static final String AWS_REGION_CONFIG_KEY = "AWS_REGION";


    private final Execute executeRequest;

    public ExecuteOperation(Execute executeRequest) {
        this.executeRequest = executeRequest;
    }

    @Override
    public String execute(Properties config) {

        //  Config items:
        //      queue names
        //      job name
        //      AWS region
        //      status filename
        //      status location
        String statusLocationBase = config.getProperty(STATUS_S3_BUCKET_CONFIG_KEY);
        String statusFileName = config.getProperty(STATUS_S3_KEY_CONFIG_KEY);
        String jobName = config.getProperty(AWS_BATCH_JOB_NAME_CONFIG_KEY);
        String jobQueueName = config.getProperty(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY);
        String awsRegion = config.getProperty(AWS_REGION_CONFIG_KEY);

        LOGGER.debug("Configuration: " + config.toString());

        String processIdentifier = executeRequest.getIdentifier().getValue();  // code spaces not supported for the moment
        Map<String, String> parameterMap = getJobParameters();

        LOGGER.debug("Submitting job request...");
        SubmitJobRequest submitJobRequest = new SubmitJobRequest();

        //  TODO: at this point we will need to invoke the correct AWS batch processing job for the function that is specified in the Execute Operation
        //  This will probably involve selecting the appropriate queue, jobName (mainly for display in AWS console) & job definition based on the processIdentifier.
        submitJobRequest.setJobQueue(jobQueueName);  //TODO: config/jobqueue selection
        submitJobRequest.setJobName(jobName);
        submitJobRequest.setJobDefinition(processIdentifier);  //TODO: either map to correct job def or set vcpus/memory required appropriately
        submitJobRequest.setParameters(parameterMap);

        AWSBatchClientBuilder builder = AWSBatchClientBuilder.standard();
        builder.setRegion(awsRegion);
        AWSBatch client = builder.build();
        SubmitJobResult result = client.submitJob(submitJobRequest);

        String jobId = result.getJobId();

        LOGGER.debug("Job submitted.  Job ID : " + jobId);

        String statusLocation = statusLocationBase + jobId + "/" + statusFileName;
        ExecuteStatusBuilder statusBuilder = new ExecuteStatusBuilder(statusLocation,jobId);
        String statusDocument = statusBuilder.createResponseDocument(EnumStatus.ACCEPTED);

        AmazonS3 cl = AmazonS3ClientBuilder.defaultClient();
        try {
            cl.putObject(
                    new PutObjectRequest(statusLocationBase, jobId + "/" + statusFileName, new StringInputStream(statusDocument), new ObjectMetadata())
                            .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch (UnsupportedEncodingException e) {
            statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, "Failed to create status file" + e.getMessage(), "StatusFileError");
            e.printStackTrace();
        }

        return statusDocument;
    }

    private Map<String, String> getJobParameters() {
        Map<String, String> result = new LinkedHashMap<>();

        DataInputsType dataInputs = executeRequest.getDataInputs();

        if (dataInputs == null) {
            return result;
        }

        for (InputType inputType : dataInputs.getInput()) {
            if (inputType.getReference() != null) {
                throw new UnsupportedOperationException("Input by reference not supported");
            }

            String identifier = inputType.getIdentifier().getValue();  // codespaces not supported for the moment
            DataType data = inputType.getData();
            String literalValue = data.getLiteralData().getValue();   //uom and datatype not supported for the moment
            result.put(identifier, literalValue);
        }

        return result;
    }

    @Override
    public void validate(Properties config) throws ValidationException
    {
        //  Validate execute operation
    }

}
