package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.wps.status.WpsConfig;
import net.opengis.wps.v_1_0_0.DataInputsType;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.InputType;
import java.util.List;

import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_JOB_DEFINITION_NAME_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_TEST_QUEUE_NAME_CONFIG_KEY;

public class JobMapper {

    public static String TEST_TRANSACTION_INPUT_IDENTIFIER = "TestMode";

    public static JobSettings getJobSettings(Execute executeRequest) throws OGCException {

        //  Do some validation on the jobDefinitionName
        if(executeRequest.getIdentifier() == null || executeRequest.getIdentifier().getValue() == null) {
            //  Throw an error
            throw new OGCException("ProcessError", "No process identifier was supplied.");
        }
        String processIdentifier = executeRequest.getIdentifier().getValue();
        JobSettings settings = new JobSettings();

        //  TODO:  Can map to different queues + job definitions here based on process identifiers
        //  Set job definition name
        if(processIdentifier.equalsIgnoreCase(WpsConfig.GOGODUCK_PROCESS_IDENTIFIER)) {
            settings.setJobDefinitionName(WpsConfig.getProperty(AWS_BATCH_JOB_DEFINITION_NAME_CONFIG_KEY));
        }

        //  Set the job queue name
        //  If the job contains an indicator that it is a test transaction (a specific process identifier) return the test queue
        if(processIdentifier.equalsIgnoreCase(WpsConfig.GOGODUCK_PROCESS_IDENTIFIER)) {
            if(isTestTransaction(executeRequest)) {
                settings.setJobQueueName(WpsConfig.getProperty(AWS_BATCH_TEST_QUEUE_NAME_CONFIG_KEY));
            } else {
                settings.setJobQueueName(WpsConfig.getProperty(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY));
            }
        }

        settings.setProcessIdentifier(processIdentifier);

        return settings;
    }

    /**
     * Check to see if this is a test transaction or not.  A test transaction is indicated by the presence of an Input in
     * the DataInputs section of the Execute request whose name is TestMode and whose literal value is 'true'.
     * eg:
     *  <wps:Input>
     *      <ows:Identifier>TestMode</ows:Identifier>
     *      <wps:Data>
     *          <wps:LiteralData>true</wps:LiteralData>
     *      </wps:Data>
     *  </wps:Input>
     *
     * @param executeRequest
     * @return
     */
    private static boolean isTestTransaction(Execute executeRequest) {

        if(executeRequest.getDataInputs() != null && executeRequest.getDataInputs().getInput() != null && executeRequest.getDataInputs().getInput().size() > 0)
        {
            DataInputsType dataInputs = executeRequest.getDataInputs();
            List<InputType> inputs = dataInputs.getInput();

            for(InputType input : inputs) {
                if(input.getIdentifier() != null) {
                    String inputName = input.getIdentifier().getValue();
                    if(inputName.equalsIgnoreCase(TEST_TRANSACTION_INPUT_IDENTIFIER)) {
                        if(input.getData() != null && input.getData().getLiteralData() != null) {
                            String value = input.getData().getLiteralData().getValue();
                            return Boolean.parseBoolean(value);
                        }
                    }
                }
            }
        }

        return false;
    }
}
