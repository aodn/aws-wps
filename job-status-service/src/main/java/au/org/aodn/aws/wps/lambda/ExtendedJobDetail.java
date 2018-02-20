package au.org.aodn.aws.wps.lambda;

import au.org.aodn.aws.wps.status.EnumStatus;
import com.amazonaws.services.batch.model.JobDetail;

public class ExtendedJobDetail {

    private JobDetail awsBatchJobDetail;
    private String wpsStatusDescription;
    private String logFileLink;

    public ExtendedJobDetail() {}

    public ExtendedJobDetail(JobDetail awsBatchJobDetail, String wpsStatusDescription, String logFileLink) {
        this.awsBatchJobDetail = awsBatchJobDetail;
        this.wpsStatusDescription = wpsStatusDescription;
        this.logFileLink = logFileLink;
    }

    public String getWpsStatusDescription() {
        return wpsStatusDescription;
    }

    public void setWpsStatusDescription(String statusDescription) {
        wpsStatusDescription = statusDescription;
    }

    public JobDetail getAwsBatchJobDetail() {
        return awsBatchJobDetail;
    }

    public void setAwsBatchJobDetail(JobDetail awsbatchJobDetail) {
        this.awsBatchJobDetail = awsbatchJobDetail;
    }

    public String getLogFileLink() {
        return logFileLink;
    }

    public void setLogFileLink(String logFileLink) {
        this.logFileLink = logFileLink;
    }
}
