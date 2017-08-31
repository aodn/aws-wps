package au.org.aodn.aws.wps.status;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.util.StringInputStream;

import java.io.UnsupportedEncodingException;

/**
 * Created by alexm12 on 24/08/2017.
 */
public class S3StatusUpdater {

    private String s3bucket;
    private final String uniqueId;
    static final private String statusFilename = "status.xml";

    public S3StatusUpdater(String s3bucket, String uniqueId) {

        this.s3bucket = s3bucket;
        this.uniqueId = uniqueId;
    }

    public void updateStatus (String statusDocument) throws UnsupportedEncodingException {
        System.out.println("Updating status in S3 : " + statusDocument);
        System.out.println("-  s3Bucket = " + s3bucket);
        System.out.println("-  uniqueId = " + uniqueId);
        StringInputStream inputStream = new StringInputStream(statusDocument);
        try {
            AmazonS3 cl = AmazonS3ClientBuilder.defaultClient();
            PutObjectRequest putRequest = new PutObjectRequest(s3bucket, uniqueId + "/" + statusFilename, inputStream, new ObjectMetadata());
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            PutObjectResult result = cl.putObject(putRequest);
            ObjectMetadata fileMetadata = result.getMetadata();
            System.out.println("File size : " + fileMetadata.getInstanceLength());
            System.out.println("Wrote status file.");
        }
        finally
        {
            try {
                inputStream.close();
            }
            catch(Exception e){}
        }

    }
}
