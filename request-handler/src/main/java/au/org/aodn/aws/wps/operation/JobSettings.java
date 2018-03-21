package au.org.aodn.aws.wps.operation;

public class JobSettings {

    private String jobQueueName;
    private String processIdentifier;
    private String jobDefinitionName;

    public String getJobQueueName() {
        return jobQueueName;
    }

    public void setJobQueueName(String jobQueueName) {
        this.jobQueueName = jobQueueName;
    }

    public String getProcessIdentifier() {
        return processIdentifier;
    }

    public void setProcessIdentifier(String jobIdentifier) {
        this.processIdentifier = jobIdentifier;
    }

    public String getJobDefinitionName() {
        return jobDefinitionName;
    }

    public void setJobDefinitionName(String jobDefinitionName) {
        this.jobDefinitionName = jobDefinitionName;
    }
}
