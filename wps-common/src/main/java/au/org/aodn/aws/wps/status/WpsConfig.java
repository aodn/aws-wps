package au.org.aodn.aws.wps.status;

public class WpsConfig {

    //  Configuration key names
    public static final String AWS_BATCH_JOB_NAME_CONFIG_KEY = "AWS_BATCH_JOB_NAME";
    public static final String AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_JOB_QUEUE_NAME";
    public static final String AWS_REGION_CONFIG_KEY = "AWS_REGION";
    public static final String STATUS_S3_BUCKET_CONFIG_KEY = "STATUS_S3_BUCKET";
    public static final String STATUS_S3_FILENAME_CONFIG_KEY = "STATUS_S3_FILENAME";
    public static final String OUTPUT_S3_BUCKET_CONFIG_KEY = "OUTPUT_S3_BUCKET";
    public static final String OUTPUT_S3_FILENAME_CONFIG_KEY = "OUTPUT_S3_FILENAME";

    public static final String S3_BASE_URL = "https://s3.amazonaws.com/";

    public static String getS3ExternalURL(String s3Bucket, String s3Key) {
        return String.format("%s%s/%s", S3_BASE_URL, s3Bucket, s3Key);
    }
}
