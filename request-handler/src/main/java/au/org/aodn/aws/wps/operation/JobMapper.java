package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.wps.request.ExecuteRequestHelper;
import au.org.aodn.aws.wps.status.WpsConfig;
import net.opengis.wps.v_1_0_0.DataInputsType;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.InputType;
import java.util.List;

import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_JOB_DEFINITION_NAME_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_TEST_QUEUE_NAME_CONFIG_KEY;

public class JobMapper {

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
        if(processIdentifier.equalsIgnoreCase(WpsConfig.GOGODUCK_PROCESS_IDENTIFIER)) {
            //  If the job is a test transaction return the test queue name
            if(ExecuteRequestHelper.isTestTransaction(executeRequest)) {
                settings.setJobQueueName(WpsConfig.getProperty(AWS_BATCH_TEST_QUEUE_NAME_CONFIG_KEY));
            } else {
                settings.setJobQueueName(WpsConfig.getProperty(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY));
            }
        }

        settings.setProcessIdentifier(processIdentifier);

        return settings;
    }
}
