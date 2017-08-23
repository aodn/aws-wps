package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.exception.ValidationException;
import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.SubmitJobRequest;
import com.amazonaws.services.batch.model.SubmitJobResult;
import net.opengis.wps._1_0.DataInputsType;
import net.opengis.wps._1_0.DataType;
import net.opengis.wps._1_0.Execute;
import net.opengis.wps._1_0.ExecuteResponse;
import net.opengis.wps._1_0.InputType;
import net.opengis.wps._1_0.StatusType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ExecuteOperation implements Operation {

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
    public Object execute(Properties config) {

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

        System.out.println("Configuration: " + config.toString());

        String processIdentifier = executeRequest.getIdentifier().getValue();  // code spaces not supported for the moment
        Map<String, String> parameterMap = getJobParameters();

        System.out.println("Submitting job request...");
        SubmitJobRequest submitJobRequest = new SubmitJobRequest();
        submitJobRequest.setJobQueue(jobQueueName);  //TODO: config/jobqueue selection
        submitJobRequest.setJobName(jobName);
        submitJobRequest.setJobDefinition(processIdentifier);  //TODO: either map to correct job def or set vcpus/memory required appropriately
        submitJobRequest.setParameters(parameterMap);

        AWSBatchClientBuilder builder = AWSBatchClientBuilder.standard();
        builder.setRegion(awsRegion);
        AWSBatch client = builder.build();
        SubmitJobResult result = client.submitJob(submitJobRequest);

        String jobId = result.getJobId();

        System.out.println("Job submitted.  Job ID : " + jobId);
        //TODO: create job status document - status = submitted!

        String statusLocation = statusLocationBase + jobId + "/" + statusFileName;

        ExecuteResponse response = new ExecuteResponse();
        response.setStatusLocation(statusLocation);
        StatusType status = new StatusType();

        try {
            status.setCreationTime(DatatypeFactory.newInstance().newXMLGregorianCalendar());
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }

        status.setProcessAccepted("Accepted job " + jobId + " for processing");
        response.setStatus(status);
        return response;
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
