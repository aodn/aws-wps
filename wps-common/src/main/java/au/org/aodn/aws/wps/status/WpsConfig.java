package au.org.aodn.aws.wps.status;

import java.util.Properties;

public class WpsConfig {

    //  Configuration key names
    public static final String AWS_BATCH_JOB_NAME_CONFIG_KEY = "AWS_BATCH_JOB_NAME";
    public static final String AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_JOB_QUEUE_NAME";
    public static final String AWS_REGION_CONFIG_KEY = "AWS_REGION";
    public static final String DEFAULT_ENV_NAME = "$LATEST";
    public static final String ENVIRONMENT_NAME_CONFIG_KEY = "ENVIRONMENT_NAME";
    public static final String STATUS_S3_BUCKET_CONFIG_KEY = "STATUS_S3_BUCKET";
    public static final String STATUS_S3_FILENAME_CONFIG_KEY = "STATUS_S3_FILENAME";
    public static final String OUTPUT_S3_BUCKET_CONFIG_KEY = "OUTPUT_S3_BUCKET";
    public static final String OUTPUT_S3_FILENAME_CONFIG_KEY = "OUTPUT_S3_FILENAME";
    public static final String GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY = "GET_CAPABILITIES_TEMPLATE_S3_BUCKET";
    public static final String GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY = "GET_CAPABILITIES_TEMPLATE_S3_KEY";
    public static final String DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY = "DESCRIBE_PROCESS_S3_BUCKET";
    public static final String DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY = "DESCRIBE_PROCESS_S3_KEY_PREFIX";
    public static final String GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY = "GEOSERVER_WPS_ENDPOINT_URL";
    public static final String GEOSERVER_WPS_ENDPOINT_TEMPLATE_KEY = "geoserverWPSEndpointURL";
    public static final String S3_BASE_URL = "https://s3.amazonaws.com/";


    public static String getS3BaseUrl()
    {
        return S3_BASE_URL;
    }

    public static Properties getConfigProperties()
    {
        //  Read environment variables
        //  Identify the location of the configuration file to use
/*
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
*/

        Properties config = new Properties();

        if(getEnvironmentVariable(AWS_BATCH_JOB_NAME_CONFIG_KEY) != null) {
            config.put(AWS_BATCH_JOB_NAME_CONFIG_KEY, getEnvironmentVariable(AWS_BATCH_JOB_NAME_CONFIG_KEY));
        }
        if(getEnvironmentVariable(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY) != null) {
            config.put(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY, getEnvironmentVariable(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY));
        }
        if(getEnvironmentVariable(AWS_REGION_CONFIG_KEY) != null) {
            config.put(AWS_REGION_CONFIG_KEY, getEnvironmentVariable(AWS_REGION_CONFIG_KEY));
        }
        if(getEnvironmentVariable(STATUS_S3_BUCKET_CONFIG_KEY) != null) {
            config.put(STATUS_S3_BUCKET_CONFIG_KEY, getEnvironmentVariable(STATUS_S3_BUCKET_CONFIG_KEY));
        }
        if(getEnvironmentVariable(STATUS_S3_FILENAME_CONFIG_KEY) != null) {
            config.put(STATUS_S3_FILENAME_CONFIG_KEY, getEnvironmentVariable(STATUS_S3_FILENAME_CONFIG_KEY));
        }
        if(getEnvironmentVariable(OUTPUT_S3_BUCKET_CONFIG_KEY) != null) {
            config.put(OUTPUT_S3_BUCKET_CONFIG_KEY, getEnvironmentVariable(OUTPUT_S3_BUCKET_CONFIG_KEY));
        }
        if(getEnvironmentVariable(OUTPUT_S3_FILENAME_CONFIG_KEY) != null) {
            config.put(OUTPUT_S3_FILENAME_CONFIG_KEY, getEnvironmentVariable(OUTPUT_S3_FILENAME_CONFIG_KEY));
        }
        if(getEnvironmentVariable(GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY) != null) {
            config.put(GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY, getEnvironmentVariable(GET_CAPABILITIES_TEMPLATE_S3_BUCKET_CONFIG_KEY));
        }
        if(getEnvironmentVariable(GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY) != null) {
            config.put(GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY, getEnvironmentVariable(GET_CAPABILITIES_TEMPLATE_S3_KEY_CONFIG_KEY));
        }
        if(getEnvironmentVariable(DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY) != null) {
            config.put(DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY, getEnvironmentVariable(DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY));
        }
        if(getEnvironmentVariable(DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY) != null) {
            config.put(DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY, getEnvironmentVariable(DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY));
        }
        if(getEnvironmentVariable(GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY) != null) {
            config.put(GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY, getEnvironmentVariable(GEOSERVER_WPS_ENDPOINT_URL_CONFIG_KEY));
        }

        return config;
    }

    private static String getEnvironmentVariable(String keyName)
    {
        return System.getenv(keyName);
    }
}
