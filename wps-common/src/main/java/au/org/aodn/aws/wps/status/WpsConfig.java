package au.org.aodn.aws.wps.status;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;


public class WpsConfig {

    public static final String STATUS_FILE_MIME_TYPE = "text/xml";
    public static final String PROVENANCE_FILE_MIME_TYPE = "application/xml";
    public static final String GOGODUCK_PROCESS_IDENTIFIER = "gs:GoGoDuck";

    //  Configuration key names
    public static final String AWS_BATCH_JOB_NAME_CONFIG_KEY = "AWS_BATCH_JOB_NAME";
    public static final String AWS_BATCH_JOB_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_JOB_QUEUE_NAME";
    public static final String AWS_BATCH_TEST_QUEUE_NAME_CONFIG_KEY = "AWS_BATCH_TEST_QUEUE_NAME";
    public static final String AWS_BATCH_JOB_DEFINITION_NAME_CONFIG_KEY = "AWS_BATCH_JOB_DEFINITION_NAME";
    public static final String AWS_BATCH_LOG_GROUP_NAME_CONFIG_KEY = "AWS_BATCH_LOG_GROUP_NAME";

    public static final String SUMOLOGIC_ENDPOINT_ENV_VARIABLE_NAME = "SUMO_ENDPOINT";
    public static final String AWS_REGION_CONFIG_KEY = "AWS_REGION";
    public static final String AWS_REGION_SES_CONFIG_KEY = "AWS_REGION_SES";
    public static final String STATUS_S3_FILENAME_CONFIG_KEY = "STATUS_S3_FILENAME";
    public static final String REQUEST_S3_FILENAME_CONFIG_KEY = "REQUEST_S3_FILENAME";
    public static final String OUTPUT_S3_BUCKET_CONFIG_KEY = "OUTPUT_S3_BUCKET";
    public static final String OUTPUT_S3_FILENAME_CONFIG_KEY = "OUTPUT_S3_FILENAME";

    public static final String CHUNK_SIZE_KEY = "CHUNK_SIZE";
    public static final String DOWNLOAD_ATTEMPTS_CONFIG_KEY = "DOWNLOAD_ATTEMPTS";
    public static final String WORKING_DIR_CONFIG_KEY = "WORKING_DIR";
    public static final String LOCAL_STORAGE_LIMIT_PROPERTY_KEY = "LOCAL_STORAGE_LIMIT_BYTES";
    public static final String POOL_SIZE_CONFIG_KEY = "POOL_SIZE";
    public static final String RETRY_INTERVAL_CONFIG_KEY = "RETRY_INTERVAL_MS";

    public static final String AWS_BATCH_JOB_ID_CONFIG_KEY = "AWS_BATCH_JOB_ID";
    public static final String AWS_BATCH_CE_NAME_CONFIG_KEY = "AWS_BATCH_CE_NAME";
    public static final String AWS_BATCH_JQ_NAME_CONFIG_KEY = "AWS_BATCH_JQ_NAME";
    public static final String AWS_BATCH_JOB_S3_KEY_PREFIX = "JOB_S3_KEY";

    public static final String WPS_ENDPOINT_URL_CONFIG_KEY = "AWS_WPS_ENDPOINT_URL";
    public static final String GEOSERVER_CATALOGUE_ENDPOINT_URL_CONFIG_KEY = "GEOSERVER_CATALOGUE_ENDPOINT_URL";
    public static final String WPS_ENDPOINT_TEMPLATE_KEY = "wpsEndpointURL";

    public static final String AGGREGATOR_TEMPLATE_FILE_URL_KEY = "AGGREGATOR_TEMPLATE_FILE_URL";

    public static final String DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY = "DOWNLOAD_CONNECT_TIMEOUT";
    public static final String DOWNLOAD_READ_TIMEOUT_CONFIG_KEY = "DOWNLOAD_READ_TIMEOUT";

    public static final String GEONETWORK_CATALOGUE_URL_CONFIG_KEY = "GEONETWORK_CATALOGUE_URL";
    public static final String GEONETWORK_CATALOGUE_LAYER_FIELD_CONFIG_KEY = "GEONETWORK_LAYER_SEARCH_FIELD";

    public static final String STATUS_SERVICE_JOB_ID_PARAMETER_NAME = "jobId";
    public static final String STATUS_SERVICE_FORMAT_PARAMETER_NAME = "format";
    public static final String STATUS_SERVICE_ENDPOINT_KEY = "STATUS_SERVICE_ENDPOINT_URL";

    // Email properties
    public static final String ADMINISTRATOR_EMAIL = "ADMINISTRATOR_EMAIL";
    public static final String EMAIL_TEMPLATES_LOCATION = "templates";
    public static final String COMPLETED_JOB_EMAIL_TEMPLATE_NAME = "jobComplete.vm";
    public static final String FAILED_JOB_EMAIL_TEMPLATE_NAME = "jobFailed.vm";
    public static final String REGISTERED_JOB_EMAIL_TEMPLATE_NAME = "jobRegistered.vm";
    public static final String REGISTERED_JOB_EMAIL_SUBJECT = "IMOS download request registered - ";
    public static final String COMPLETED_JOB_EMAIL_SUBJECT = "IMOS download available - ";
    public static final String FAILED_JOB_EMAIL_SUBJECT = "IMOS download error - ";
    public static final String JOB_EMAIL_CONTACT_ADDRESS = "info@aodn.org.au";
    public static final String JOB_EMAIL_FROM_ADDRESS = "administrator@aodn.org.au";
    public static final String SOURCE_ARN = "SOURCE_ARN";

    public static final String DATA_DOWNLOAD_URL_PREFIX_CONFIG_KEY = "DATA_DOWNLOAD_URL_PREFIX";

    //  Constants
    public static String TEST_TRANSACTION_INPUT_IDENTIFIER = "TestMode";
    public static final String DEFAULT_LANGUAGE = "en-US";

    public static String getProperty(String propertyName) {
        return System.getenv(propertyName);
    }

    public static String getStatusServiceHtmlEndpoint(String jobUuid) {
        return getStatusServiceEndpoint(jobUuid, JobStatusFormatEnum.HTML.toString());
    }

    public static String getStatusServiceXmlEndpoint(String jobUuid) {
        return getStatusServiceEndpoint(jobUuid, JobStatusFormatEnum.XML.toString());
    }

    public static String getStatusServiceEndpoint(String jobUuid, String format) {
        String statusServiceEndpoint = getProperty(STATUS_SERVICE_ENDPOINT_KEY);
        return String.format("%s?%s=%s&%s=%s", statusServiceEndpoint, STATUS_SERVICE_JOB_ID_PARAMETER_NAME, jobUuid, STATUS_SERVICE_FORMAT_PARAMETER_NAME, format);
    }

    public static String getBaseStatusServiceAdminLink() {
        String statusServiceEndpoint = getProperty(STATUS_SERVICE_ENDPOINT_KEY);
        return String.format("%s?%s=%s", statusServiceEndpoint, STATUS_SERVICE_FORMAT_PARAMETER_NAME, "ADMIN");
    }

    public static String getAwsWpsEndpointUrl() {
        return getProperty(WPS_ENDPOINT_URL_CONFIG_KEY);
    }


    public static String getS3ExternalURL(String s3Bucket, String s3Key) {
        String region = getProperty(WpsConfig.AWS_REGION_CONFIG_KEY);
        return String.format("https://s3-%s.amazonaws.com/%s/%s", region, s3Bucket, s3Key);
    }

    /**
     * Decrypt the value of a named environment variable.
     *
     * @param keyName
     * @return Decrypted value of the named environment variable.
     */
    private String getEncryptedEnvironmentVariable(String keyName) {
        return decryptKey(System.getenv(keyName));
    }


    /**
     * Decrypt an encrypted environment variable value.
     *
     * @param keyValue
     * @return Decrypted key value.
     */
    private String decryptKey(String keyValue) {
        if (keyValue != null) {
            byte[] encryptedKey = Base64.decode(keyValue);
            AWSKMS client = AWSKMSClientBuilder.defaultClient();
            DecryptRequest request = new DecryptRequest()
                    .withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
            ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
            return new String(plainTextKey.array(), Charset.forName("UTF-8"));
        }
        return null;
    }
}
