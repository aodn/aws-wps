package au.org.aodn.aws.util;

import com.amazonaws.services.s3.AmazonS3;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class S3Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Utils.class);

    public static String readS3ObjectAsString(String s3Bucket, String s3Key) throws IOException {

        String objectString;

        //  read file to String
        try(S3ObjectInputStream contentStream = getS3ObjectStream(s3Bucket, s3Key)) {
            objectString = Utils.inputStreamToString(contentStream);
        } catch (IOException ioex) {
            //  Bad stuff - blow up!
            LOGGER.error("Problem reading S3 object [" + s3Bucket + "/" + s3Key +"]: ", ioex);
            throw ioex;
        }

        return objectString;
    }

    public static S3ObjectInputStream getS3ObjectStream(String s3Bucket, String s3Key) throws IOException {
        S3Object s3Object = getS3Object(s3Bucket, s3Key);
        return s3Object.getObjectContent();
    }


    public static S3Object getS3Object(String s3Bucket, String s3Key) throws IOException {
        //  Get from S3 bucket
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        return s3Client.getObject(s3Bucket, s3Key);
    }

    public static void uploadToS3(File file, String s3bucket, String fileKey, String contentType)
        throws InterruptedException, IOException {
        TransferManager tx = TransferManagerBuilder.defaultTransferManager();
        FileInputStream fileStream = null;
        try {
            Upload myUpload;

            LOGGER.info("Uploading file [" + file.getAbsolutePath() + "] to S3 Bucket [" + s3bucket + "], Key [" + fileKey + "]");
            if(contentType != null) {
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentType(contentType);
                //  Need to set content length - otherwise S3 TransferManager tries
                //  to read the entire file into memory.  May be rectified at some point
                //  in the future. See: https://github.com/aws/aws-sdk-java/issues/474#issuecomment-126413580
                meta.setContentLength(file.length());
                LOGGER.info("Setting contentType [" + contentType + "]. Content length [" + file.length() +" bytes]");
                fileStream = new FileInputStream(file);
                myUpload = tx.upload(s3bucket, fileKey, fileStream, meta);
            } else {
                myUpload = tx.upload(s3bucket, fileKey, file);
            }

            myUpload.waitForCompletion();
        } catch(FileNotFoundException fnfe) {
            LOGGER.error("File not found: " + fnfe, fnfe);
            throw fnfe;
        } finally {

            if(fileStream != null) {
                try {
                    fileStream.close();
                } catch(IOException ioex) {
                    LOGGER.error("Exception closing file stream: " + ioex.getMessage(), ioex);
                }
            }
            tx.shutdownNow();
        }
    }

    public static void uploadToS3(String document, String bucket, String key, String contentType) throws IOException {

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

        try (StringInputStream inputStream = new StringInputStream(document)) {
            PutObjectRequest putRequest = new PutObjectRequest(bucket, key, inputStream, new ObjectMetadata());
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);

            ObjectMetadata meta = new ObjectMetadata();

            //  Add content (mime) type metadata if provided
            if(contentType != null) {
                meta.setContentType(contentType);
                LOGGER.info("Setting contentType [" + contentType + "]");
            }

            LOGGER.info("Setting contentLength [" + inputStream.available() + " bytes]");
            meta.setContentLength(inputStream.available());
            putRequest.setMetadata(meta);

            s3Client.putObject(putRequest);
        } catch (Exception ex) {
            LOGGER.error(String.format("Unable to write content [%s] to S3. Bucket [%s], Key [%s]", document, bucket, key), ex);
            throw new IOException(ex);
        }
    }

    public static int getExpirationinDays(String bucket) {
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        return s3Client.getBucketLifecycleConfiguration(bucket).getRules().get(0).getExpirationInDays();
    }

}
