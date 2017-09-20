package au.org.aodn.aws.wps.status;

import java.util.Properties;

public class WpsConfig {

    //  Configuration key names
    public static final String AWS_BATCH_JOB_NAME_CONFIG_KEY = "AWS_BATCH_JOB_NAME";
    public static final String AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_JOB_QUEUE_NAME";
    public static final String AWS_REGION_CONFIG_KEY = "AWS_REGION";
    public static final String STATUS_S3_BUCKET_CONFIG_KEY = "STATUS_S3_BUCKET";
    public static final String STATUS_S3_FILENAME_CONFIG_KEY = "STATUS_S3_FILENAME";
    public static final String OUTPUT_S3_BUCKET_CONFIG_KEY = "OUTPUT_S3_BUCKET";
    public static final String OUTPUT_S3_FILENAME_CONFIG_KEY = "OUTPUT_S3_FILENAME";

    public static final String AWS_BATCH_JOB_ID_CONFIG_KEY = "AWS_BATCH_JOB_ID";
    public static final String AWS_BATCH_CE_NAME_CONFIG_KEY = "AWS_BATCH_CE_NAME";
    public static final String AWS_BATCH_JQ_NAME_CONFIG_KEY = "AWS_BATCH_JQ_NAME";

    public static final String CONFIG_LOCATION_KEY = "CONFIG_LOCATION";

    public static final String S3_BASE_URL = "https://s3.amazonaws.com/";
    private static Properties properties = null;

    private static Properties getProperties() {
        if (properties == null) {
            // Load properties from config
            // TODO: Read properties from location CONFIG_LOCATION_CONFIG_KEY (S3 or local) and load it
            properties = new Properties();

            // Load properties from environment variables
            properties.setProperty(AWS_BATCH_JOB_NAME_CONFIG_KEY, System.getenv(AWS_BATCH_JOB_NAME_CONFIG_KEY));
            properties.setProperty(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY, System.getenv(AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY));
            properties.setProperty(AWS_REGION_CONFIG_KEY, System.getenv(AWS_REGION_CONFIG_KEY));
            properties.setProperty(STATUS_S3_BUCKET_CONFIG_KEY, System.getenv(STATUS_S3_BUCKET_CONFIG_KEY));
            properties.setProperty(STATUS_S3_FILENAME_CONFIG_KEY, System.getenv(STATUS_S3_FILENAME_CONFIG_KEY));
            properties.setProperty(OUTPUT_S3_BUCKET_CONFIG_KEY, System.getenv(OUTPUT_S3_BUCKET_CONFIG_KEY));
            properties.setProperty(OUTPUT_S3_FILENAME_CONFIG_KEY, System.getenv(OUTPUT_S3_FILENAME_CONFIG_KEY));

            properties.setProperty(AWS_BATCH_JOB_ID_CONFIG_KEY, System.getenv(AWS_BATCH_JOB_ID_CONFIG_KEY));
            properties.setProperty(AWS_BATCH_CE_NAME_CONFIG_KEY, System.getenv(AWS_BATCH_CE_NAME_CONFIG_KEY));
            properties.setProperty(AWS_BATCH_JQ_NAME_CONFIG_KEY, System.getenv(AWS_BATCH_JQ_NAME_CONFIG_KEY));
        }

        return properties;
    }

    public static String getConfig(String configName) {
        return getProperties().getProperty(configName);
    }

    public static String getS3ExternalURL(String s3Bucket, String s3Key) {
        return String.format("%s%s/%s", S3_BASE_URL, s3Bucket, s3Key);
    }
}
