package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.EnumStatus;
import au.org.aodn.aws.wps.StatusCreator;
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
import net.opengis.wps._1_0.ExecuteResponse;
import net.opengis.wps._1_0.InputType;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ExecuteOperation implements Operation {

    //  Configuration key names
    private static final String STATUS_LOCATION_BASE_CONFIG_KEY = "STATUS_LOCATION";
    private static final String STATUS_FILE_FILENAME_CONFIG_KEY = "STATUS_FILENAME";
    private static final String JOB_NAME_CONFIG_KEY = "JOB_NAME";
    private static final String JOB_QUEUE_NAME_CONFIG_KEY = "JOB_QUEUE_NAME";
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
        String statusLocationBase = config.getProperty(STATUS_LOCATION_BASE_CONFIG_KEY);
        String statusFileName = config.getProperty(STATUS_FILE_FILENAME_CONFIG_KEY);
        String jobName = config.getProperty(JOB_NAME_CONFIG_KEY);
        String jobQueueName = config.getProperty(JOB_QUEUE_NAME_CONFIG_KEY);
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
        StatusCreator statusUpdater = new StatusCreator("http://" + statusLocationBase + "/" + jobId + "/" + statusFileName, jobId);
        ExecuteResponse response = statusUpdater.createResponseDocument(EnumStatus.ACCEPTED);

        String document = StatusCreator.createXmlDocument(response);

        AmazonS3 cl = AmazonS3ClientBuilder.defaultClient();

        try {
            cl.putObject(
                    new PutObjectRequest(statusLocationBase, jobId + "/" + statusFileName, new StringInputStream(document), new ObjectMetadata())
                            .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch (UnsupportedEncodingException e) {
            //TODO: if uploading status file fails should we return a failed status?
            e.printStackTrace();
        }

        return document;
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


}
