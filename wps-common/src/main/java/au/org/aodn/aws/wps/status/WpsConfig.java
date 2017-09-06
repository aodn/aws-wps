package au.org.aodn.aws.wps.status;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.util.Properties;

public class WpsConfig {

    public static final String REGION_NAME_ENV_VARIABLE_NAME = "S3_AWS_REGION";
    public static final String S3_BUCKET_ENV_VARIABLE_NAME = "CONFIG_PREFIX";
    public static final String CONFIG_FILENAME_ENV_VARIABLE_NAME = "CONFIG_FILENAME";

    //  Configuration key names
    public static final String AWS_BATCH_JOB_NAME_CONFIG_KEY = "AWS_BATCH_JOB_NAME";
    public static final String AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_JOB_QUEUE_NAME";
    public static final String AWS_REGION_CONFIG_KEY = "AWS_REGION";
    public static final String ENVIRONMENT_NAME_ENV_VARIABLE_NAME = "ENVIRONMENT_NAME";

    public static final String STATUS_S3_BUCKET_CONFIG_KEY = "STATUS_S3_BUCKET";
    public static final String STATUS_S3_FILENAME_CONFIG_KEY = "STATUS_S3_FILENAME";

    public static final String OUTPUT_S3_BUCKET_CONFIG_KEY = "OUTPUT_S3_BUCKET";
    public static final String OUTPUT_S3_FILENAME_CONFIG_KEY = "OUTPUT_S3_FILENAME";

    public static Properties getConfigProperties(String envName)
    {
        //  Read environment variables
        //  Identify the location of the configuration file to use
        String s3RegionName = getEnvironmentVariable(REGION_NAME_ENV_VARIABLE_NAME);
        String bucketName = getEnvironmentVariable(S3_BUCKET_ENV_VARIABLE_NAME);
        String configFilename = getEnvironmentVariable(CONFIG_FILENAME_ENV_VARIABLE_NAME);



        AmazonS3Client s3Client = new AmazonS3Client();
        Region region = Region.getRegion(Regions.fromName(s3RegionName));
        s3Client.setRegion(region);

        //  Append an environment name to the front of the config filename
        if(envName != null)
        {
            configFilename  = envName + "/" + configFilename;
        }

        System.out.println("S3 Config location: " + bucketName + "/" + configFilename);

        //  Note:  the Lambda function needs access to read from S3 bucket location
        //  that contains the configuration file.
        S3Object configFile = s3Client.getObject(bucketName, configFilename);
        S3ObjectInputStream contentStream = configFile.getObjectContent();
        Properties config = new Properties();
        try {
            //  Load configuration from S3 file
            config.load(contentStream);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unable to load configuration : " + ex.getMessage());
        }

        return config;
    }

    private static String getEnvironmentVariable(String keyName)
    {
        return System.getenv(keyName);
    }
}
