package au.org.aodn.aws.wps.status;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_JOB_S3_KEY;

public class S3JobFileUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3JobFileUpdater.class);

    private final String s3bucket;
    private final String statusFilename;
    private final String requestFilename;
    private AmazonS3 client;

    public S3JobFileUpdater(String s3bucket, String statusFilename, String requestFilename) {
        this.s3bucket = s3bucket;
        this.statusFilename = statusFilename;
        this.requestFilename = requestFilename;
        this.client = AmazonS3ClientBuilder.defaultClient();
    }

    public void updateStatus (String statusDocument, String jobId) throws UnsupportedEncodingException {
        String location = getJobFilePrefix(jobId, statusFilename);
        writePublicFileToS3(statusDocument, location);
    }

    public void writeRequest (String requestDocument, String jobId) throws UnsupportedEncodingException {
        String location = getJobFilePrefix(jobId, requestFilename);
        writePublicFileToS3(requestDocument, location);
    }

    private String getJobFilePrefix(String jobId, String fileName) {
        String jobPrefix = WpsConfig.getConfig(AWS_BATCH_JOB_S3_KEY);
        return  String.format("%s%s/%s", jobPrefix, jobId, fileName);
    }

    public void writePublicFileToS3(String document, String location) throws UnsupportedEncodingException {
        StringInputStream inputStream = null;
        LOGGER.info(String.format("Writing request to S3 %s at %s ", s3bucket, location));

        try {
            inputStream = new StringInputStream(document);
            PutObjectRequest putRequest = new PutObjectRequest(s3bucket, location, inputStream, new ObjectMetadata());
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            client.putObject(putRequest);
        } catch (Exception ex) {
            LOGGER.error(String.format("Unable to write file %s to bucket %s at %s", document, s3bucket, location), ex);
            throw ex;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
