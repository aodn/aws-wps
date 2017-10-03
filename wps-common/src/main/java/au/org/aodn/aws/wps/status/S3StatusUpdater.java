package au.org.aodn.aws.wps.status;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Created by alexm12 on 24/08/2017.
 */
public class S3StatusUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3StatusUpdater.class);

    private final String s3bucket;
    private final String statusFilename;

    public S3StatusUpdater(String s3bucket, String statusFilename) {

        this.s3bucket = s3bucket;
        this.statusFilename = statusFilename;
    }

    public void updateStatus (String statusDocument, String jobId) throws UnsupportedEncodingException {
        LOGGER.info("Updating status in S3 : " + statusDocument);
        LOGGER.info("-  s3Bucket = " + s3bucket);
        LOGGER.info("-  jobId    = " + jobId);
        StringInputStream inputStream = new StringInputStream(statusDocument);
        try {
            AmazonS3 client = AmazonS3ClientBuilder.defaultClient();
            PutObjectRequest putRequest = new PutObjectRequest(s3bucket, jobId + "/" + statusFilename, inputStream, new ObjectMetadata());
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            client.putObject(putRequest);
        }
        catch(Exception ex)
        {
            LOGGER.error("Unable to write status file: S3Bucket [" + s3bucket + "], Key [" + jobId + "/" + statusFilename + "]", ex);
        }
        finally
        {
            try { inputStream.close(); } catch(Exception e){}
        }
    }
}
