package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.exception.ValidationException;
import au.org.aodn.aws.wps.status.EnumOperation;
import au.org.aodn.aws.wps.status.EnumStatus;
import au.org.aodn.aws.wps.status.S3StatusUpdater;
import au.org.aodn.aws.wps.status.StatusHelper;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.ContainerOverrides;
import com.amazonaws.services.batch.model.KeyValuePair;
import com.amazonaws.services.batch.model.SubmitJobRequest;
import com.amazonaws.services.batch.model.SubmitJobResult;
import net.opengis.wps._1_0.DataInputsType;
import net.opengis.wps._1_0.DataType;
import net.opengis.wps._1_0.Execute;
import net.opengis.wps._1_0.InputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.UnsupportedEncodingException;
import java.util.*;

import static au.org.aodn.aws.wps.status.WpsConfig.*;

public class ExecuteOperation implements Operation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteOperation.class);


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
        String statusS3BucketName = config.getProperty(STATUS_S3_BUCKET_CONFIG_KEY);
        String statusFileName = config.getProperty(STATUS_S3_KEY_CONFIG_KEY);
        String jobName = config.getProperty(AWS_BATCH_JOB_NAME_CONFIG_KEY);
        String jobQueueName = config.getProperty(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY);
        String awsRegion = config.getProperty(AWS_REGION_CONFIG_KEY);
        String environmentName = config.getProperty(ENVIRONMENT_NAME_ENV_VARIABLE_NAME);

        LOGGER.info("Configuration: " + config.toString());

        String processIdentifier = executeRequest.getIdentifier().getValue();  // code spaces not supported for the moment
        Map<String, String> parameterMap = getJobParameters();

        LOGGER.info("Submitting job request...");
        SubmitJobRequest submitJobRequest = new SubmitJobRequest();

        //  TODO: at this point we will need to invoke the correct AWS batch processing job for the function that is specified in the Execute Operation
        //  This will probably involve selecting the appropriate queue, jobName (mainly for display in AWS console) & job definition based on the processIdentifier.
        submitJobRequest.setJobQueue(jobQueueName);  //TODO: config/jobqueue selection
        submitJobRequest.setJobName(jobName);
        submitJobRequest.setJobDefinition(processIdentifier);  //TODO: either map to correct job def or set vcpus/memory required appropriately
        submitJobRequest.setParameters(parameterMap);

        //  Add environment name to the submit job request as an environment
        //  variable.
        ContainerOverrides jobOverrides = new ContainerOverrides();
        Set<KeyValuePair> envVariables = new HashSet<KeyValuePair>();
        KeyValuePair envNameVariable = new KeyValuePair();
        //  Pass environment name to batch process using environment variable
        envNameVariable.setName(ENVIRONMENT_NAME_ENV_VARIABLE_NAME);
        envNameVariable.setValue(environmentName);
        envVariables.add(envNameVariable);
        jobOverrides.setEnvironment(envVariables);

        submitJobRequest.setContainerOverrides(jobOverrides);

        AWSBatchClientBuilder builder = AWSBatchClientBuilder.standard();
        builder.setRegion(awsRegion);

        AWSBatch client = builder.build();
        SubmitJobResult result = client.submitJob(submitJobRequest);

        String jobId = result.getJobId();

        LOGGER.info("Job submitted.  Job ID : " + jobId);

        String statusDocument = null;
        S3StatusUpdater statusUpdater = new S3StatusUpdater(statusS3BucketName, statusFileName);
        try
        {
            statusUpdater.updateStatus(EnumOperation.EXECUTE, jobId, EnumStatus.ACCEPTED, null, null);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            //  Form failed status document
            statusDocument = StatusHelper.getStatusDocument(statusS3BucketName, statusFileName, EnumOperation.EXECUTE, jobId, EnumStatus.FAILED, "Failed to create status file" + e.getMessage(), "StatusFileError");
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
