package au.org.aodn.aws.wps.status;

public class StatusHelper
{

    public static final String getStatusDocument(String s3Bucket, String statusFilename, EnumOperation operation, String jobId, EnumStatus jobStatus, String message, String messageCode)
    {
        String statusDocument = null;

        //  TODO: factory for status document builders
        if(operation.equals(EnumOperation.EXECUTE))
        {
            String statusLocation = "https://s3.amazonaws.com/" + s3Bucket + "/" + jobId + "/" + statusFilename;
            ExecuteStatusBuilder statusBuilder = new ExecuteStatusBuilder(statusLocation, jobId);
            statusDocument = statusBuilder.createResponseDocument(jobStatus, message, messageCode);
        }

        return statusDocument;
    }
}
