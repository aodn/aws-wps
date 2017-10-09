package au.org.aodn.aws.util;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;
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

    public static String readS3ObjectAsString(String s3Bucket, String s3Key, String s3Region) throws IOException {
        String objectString = null;
        S3ObjectInputStream contentStream = getS3ObjectStream(s3Bucket, s3Key, s3Region);

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


    public static S3ObjectInputStream getS3ObjectStream(String s3Bucket, String s3Key, String s3Region) throws IOException {
        //  Get from S3 bucket location
        AmazonS3Client s3Client = new AmazonS3Client();
        if (s3Region != null) {
            Region region = Region.getRegion(Regions.fromName(s3Region));
            s3Client.setRegion(region);
        }

        S3Object templateObject = s3Client.getObject(s3Bucket, s3Key);
        return templateObject.getObjectContent();

    }


    public static void uploadToS3(S3URI s3URI, File file)
            throws InterruptedException {
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        TransferManager tx = new TransferManager(credentialProviderChain.getCredentials());
        Upload myUpload = tx.upload(s3URI.getBucket(), s3URI.getKey(), file);
        myUpload.waitForCompletion();
        tx.shutdownNow();
    }


    public static void uploadToS3(S3URI s3URI, String content)
            throws InterruptedException, IOException {
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        TransferManager tx = new TransferManager(credentialProviderChain.getCredentials());
        try {
            StringInputStream stringStream = new StringInputStream(content);
            Upload myUpload = tx.upload(s3URI.getBucket(), s3URI.getKey(), stringStream, null);
            myUpload.waitForCompletion();
            tx.shutdownNow();
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Unable to upload content to S3 : " + ex.getMessage(), ex);
            throw new IOException("Unable to upload content to S3 : " + ex.getMessage(), ex);
        }
    }
}
