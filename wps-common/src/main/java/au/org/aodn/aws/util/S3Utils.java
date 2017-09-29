package au.org.aodn.aws.util;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class S3Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Utils.class);

    public static String readS3ObjectAsString(String s3Bucket, String s3Key, String s3Region) throws IOException
    {
        String objectString = null;

        //  Get from S3 bucket location
        AmazonS3Client s3Client = new AmazonS3Client();
        if(s3Region != null) {
            Region region = Region.getRegion(Regions.fromName(s3Region));
            s3Client.setRegion(region);
        }

        S3Object templateObject = s3Client.getObject(s3Bucket, s3Key);
        S3ObjectInputStream contentStream = templateObject.getObjectContent();

        //  read file to String
        try {
            objectString = Utils.inputStreamToString(contentStream);
        }
        catch(IOException ioex)
        {
            //  Bad stuff - blow up!
            LOGGER.error("Problem loading S3 object: ", ioex);
            throw ioex;
        }
        return objectString;
    }
}
