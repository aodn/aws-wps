package au.org.aodn.aws.wps.operation;

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

public class ExecuteOperation implements Operation {
    //TODO:  need to get from config
    private static final String STATUS_LOCATION = "https://bucket/prefix/";

    private final Execute executeRequest;

    public ExecuteOperation(Execute executeRequest) {
        this.executeRequest = executeRequest;
    }

    @Override
    public Object execute() {
        //TODO: Read config

        String processIdentifier = executeRequest.getIdentifier().getValue();  // code spaces not supported for the moment
        Map<String, String> parameterMap = getJobParameters();

        SubmitJobRequest submitJobRequest = new SubmitJobRequest();
        submitJobRequest.setJobQueue("javaduck-small-in");  //TODO: config/jobqueue selection
        submitJobRequest.setJobName("javaduck");
        submitJobRequest.setJobDefinition(processIdentifier);  //TODO: either map to correct job def or set vcpus/memory required appropriately
        submitJobRequest.setParameters(parameterMap);

        AWSBatchClientBuilder builder = AWSBatchClientBuilder.standard();
        builder.setRegion("us-east-1");  // TODO: get from config
        AWSBatch client = builder.build();
        SubmitJobResult result = client.submitJob(submitJobRequest);

        String jobId = result.getJobId();

        //TODO: create job status document - status = submitted!

        String statusLocation = STATUS_LOCATION + jobId + "/status.xml";

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


}
