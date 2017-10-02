package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.status.WpsConfig;

import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_JOB_DEFINITION_NAME_CONFIG_KEY;

public class JobMapper {
    private String processIdentifier;

    public JobMapper(String processIdentifier) {
        this.processIdentifier = processIdentifier;
    }

    public String getJobDefinitionName() {
        if(processIdentifier.equalsIgnoreCase("gs:gogoduck")) {
            return WpsConfig.getConfig(AWS_BATCH_JOB_DEFINITION_NAME_CONFIG_KEY);
        } else {
            return null;
        }
    }

    public String getProcessIdentifier() {
        return processIdentifier;
    }

    public void setProcessIdentifier(String processIdentifier) {
        this.processIdentifier = processIdentifier;
    }
}
