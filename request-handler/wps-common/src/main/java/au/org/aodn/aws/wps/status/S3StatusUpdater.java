package au.org.aodn.aws.wps.status;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
        AmazonS3 cl = AmazonS3ClientBuilder.defaultClient();
        cl.putObject(
                new PutObjectRequest(s3bucket, uniqueId + "/" + statusFilename, new StringInputStream(statusDocument), new ObjectMetadata())
                        .withCannedAcl(CannedAccessControlList.PublicRead));

    }
}
