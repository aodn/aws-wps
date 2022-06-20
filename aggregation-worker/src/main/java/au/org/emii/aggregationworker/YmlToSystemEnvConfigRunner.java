package au.org.emii.aggregationworker;

import au.org.aodn.aws.wps.status.WpsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * The program run by assuming that system env is set before calling the AggregrationRunner, this is ok for DOCKER
 * env but not ok for development env. This class is used to make sure those system environments are set via the
 * yml configuration
 *
 * Without touching the current implementation, we set this value in this class
 */
@Profile({"dev"})
@Order(1)
@Component
public class YmlToSystemEnvConfigRunner implements CommandLineRunner {

    @Value("${aggregationWorker.WORKING_DIR}")
    protected Optional<String> workingDirectory;

    @Value("${aggregationWorker.DOWNLOAD_CONNECT_TIMEOUT}")
    protected Optional<String> downloadConnectTimeout;

    @Value("${aggregationWorker.DOWNLOAD_READ_TIMEOUT}")
    protected Optional<String> downloadReadTimeout;

    @Value("${aggregationWorker.BATCH_JOB_ID}")
    protected Optional<String> batchJobId;

    @Value("${aggregationWorker.JOB_S3_KEY}")
    protected Optional<String> jobS3keyPrefix;

    @Value("${aggregationWorker.STATUS_S3_FILENAME}")
    protected Optional<String> statusFileName;

    @Value("${aggregationWorker.REQUEST_S3_FILENAME}")
    protected Optional<String> requestFileName;

    @Value("${aggregationWorker.AWS_WPS_ENDPOINT_URL}")
    protected Optional<String> endPointUrl;

    @Value("${aggregationWorker.GEOSERVER_CATALOGUE_ENDPOINT_URL}")
    protected Optional<String> geoServerUrl;

    @Value("${aggregationWorker.OUTPUT_S3_BUCKET}")
    protected Optional<String> outputBucket;

    @Value("${aggregationWorker.administratorEmail}")
    protected Optional<String> administratorEmail;

    @Override
    public void run(String... args) {
        System.setProperty(WpsConfig.WORKING_DIR_CONFIG_KEY, workingDirectory.orElse(null));
        System.setProperty(WpsConfig.DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY, downloadConnectTimeout.orElse(null));
        System.setProperty(WpsConfig.DOWNLOAD_READ_TIMEOUT_CONFIG_KEY, downloadReadTimeout.orElse(null));
        System.setProperty(WpsConfig.AWS_BATCH_JOB_ID_CONFIG_KEY, batchJobId.orElse(null));
        System.setProperty(WpsConfig.AWS_BATCH_JOB_S3_KEY_PREFIX, jobS3keyPrefix.orElse(null));
        System.setProperty(WpsConfig.STATUS_S3_FILENAME_CONFIG_KEY, statusFileName.orElse(null));
        System.setProperty(WpsConfig.REQUEST_S3_FILENAME_CONFIG_KEY, requestFileName.orElse(null));
        System.setProperty(WpsConfig.WPS_ENDPOINT_URL_CONFIG_KEY, endPointUrl.orElse(null));
        System.setProperty(WpsConfig.GEONETWORK_CATALOGUE_URL_CONFIG_KEY, geoServerUrl.orElse(null));
        System.setProperty(WpsConfig.ADMINISTRATOR_EMAIL, administratorEmail.orElse(null));
        System.setProperty(WpsConfig.OUTPUT_S3_BUCKET_CONFIG_KEY, outputBucket.orElse(null));
    }
}
