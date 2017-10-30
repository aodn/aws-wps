package au.org.aodn.aws.util;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.crawlabledataset.s3.S3URI;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class S3Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Utils.class);

    public static String readS3ObjectAsString(String s3Bucket, String s3Key) throws IOException {
        String objectString = null;
        S3ObjectInputStream contentStream = getS3ObjectStream(s3Bucket, s3Key);

        //  read file to String
        try {
            objectString = Utils.inputStreamToString(contentStream);
        } catch (IOException ioex) {
            //  Bad stuff - blow up!
            LOGGER.error("Problem loading S3 object: ", ioex);
            throw ioex;
        }
        return objectString;
    }

    public static S3ObjectInputStream getS3ObjectStream(String s3Bucket, String s3Key) throws IOException {
        S3Object templateObject = getS3Object(s3Bucket, s3Key);
        return templateObject.getObjectContent();
    }


    public static S3Object getS3Object(String s3Bucket, String s3Key) throws IOException {
        //  Get from S3 bucket
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        return s3Client.getObject(s3Bucket, s3Key);
    }

    public static void uploadToS3(File file, String s3bucket, String jobFileKey)
        throws InterruptedException {
        TransferManager tx = TransferManagerBuilder.defaultTransferManager();
        Upload myUpload = tx.upload(s3bucket, jobFileKey, file);
        myUpload.waitForCompletion();
        tx.shutdownNow();
    }

    public static void uploadToS3(String document, String bucket, String key) throws IOException {
        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            StringInputStream inputStream = new StringInputStream(document);
            PutObjectRequest putRequest = new PutObjectRequest(bucket, key, inputStream, new ObjectMetadata());
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            s3Client.putObject(putRequest);
        } catch (Exception ex) {
            LOGGER.error(String.format("Unable to write file %s to bucket %s at %s", document, bucket, key), ex);
            throw new IOException(ex);
        }
    }

}
