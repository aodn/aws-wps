package au.org.emii.aggregator.au.org.emii.aggregator.config;

import au.org.aodn.aws.util.S3Utils;
import au.org.emii.download.DownloadConfig;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DownloadConfigReader {

    private static final Logger logger = LoggerFactory.getLogger(DownloadConfigReader.class);

    public static final String DOWNLOAD_ATTEMPTS_CONFIG_KEY = "DOWNLOAD_ATTEMPTS";
    public static final String DOWNLOAD_DIRECTORY_PROPERTY_KEY = "DOWNLOAD_DIRECTORY";
    public static final String LOCAL_STORAGE_LIMIT_PROPERTY_KEY = "LOCAL_STORAGE_LIMIT_BYTES";
    public static final String POOL_SIZE_CONFIG_KEY = "POOL_SIZE";
    public static final String RETRY_INTERVAL_CONFIG_KEY = "RETRY_INTERVAL_MS";

    public static DownloadConfig getDownloadConfig(String s3Bucket, String s3Key)
            throws IOException
    {
        DownloadConfig.ConfigBuilder builder = new DownloadConfig.ConfigBuilder();

        //  Read config file from S3
        S3ObjectInputStream downloadConfigStream = null;
        try {
            downloadConfigStream = S3Utils.getS3ObjectStream(s3Bucket, s3Key, null);
        }
        catch(IOException ioex)
        {
            logger.error("Unable to read download config properties file: S3Bucket [" + s3Bucket + "] , Key [" + s3Key + "]", ioex);
            throw ioex;
        }

        InputStreamReader reader = new InputStreamReader(downloadConfigStream);
        Properties downloadConfig = new Properties();
        downloadConfig.load(reader);

        //  Populate builder with values from config file
        int downloadAttempts = Integer.parseInt(downloadConfig.getProperty(DOWNLOAD_ATTEMPTS_CONFIG_KEY));
        String downloadDirectory = downloadConfig.getProperty(DOWNLOAD_DIRECTORY_PROPERTY_KEY);
        long localStorageLimit = Long.parseLong(downloadConfig.getProperty(LOCAL_STORAGE_LIMIT_PROPERTY_KEY));
        int poolSize = Integer.parseInt(downloadConfig.getProperty(POOL_SIZE_CONFIG_KEY));
        int retryInterval = Integer.parseInt(downloadConfig.getProperty(RETRY_INTERVAL_CONFIG_KEY));
        Path downloadPath = Paths.get(downloadDirectory);

        builder.downloadAttempts(downloadAttempts);
        builder.downloadDirectory(downloadPath);
        builder.localStorageLimit(localStorageLimit);
        builder.poolSize(poolSize);
        builder.retryInterval(retryInterval);


        DownloadConfig config = new DownloadConfig(builder);

        return config;
    }

}
