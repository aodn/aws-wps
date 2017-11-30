package au.org.emii.aggregator.au.org.emii.aggregator.config;

import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.download.DownloadConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static au.org.aodn.aws.wps.status.WpsConfig.*;

public class DownloadConfigReader {

    private static DownloadConfig config = null;

    public static DownloadConfig getDownloadConfig(Path downloadDirectory)
            throws IOException {
        if (config == null) {
            DownloadConfig.ConfigBuilder builder = new DownloadConfig.ConfigBuilder();

            int downloadAttempts = Integer.valueOf(WpsConfig.getConfig(DOWNLOAD_ATTEMPTS_CONFIG_KEY));
            String workingDir = WpsConfig.getConfig(WORKING_DIR_CONFIG_KEY);
            long localStorageLimit = Long.valueOf(WpsConfig.getConfig(LOCAL_STORAGE_LIMIT_PROPERTY_KEY));
            int poolSize = Integer.valueOf(WpsConfig.getConfig(POOL_SIZE_CONFIG_KEY));
            int retryInterval = Integer.valueOf(WpsConfig.getConfig(RETRY_INTERVAL_CONFIG_KEY));
            builder.downloadAttempts(downloadAttempts);
            builder.downloadDirectory(downloadDirectory);
            builder.localStorageLimit(localStorageLimit);
            builder.poolSize(poolSize);
            builder.retryInterval(retryInterval);

            config = new DownloadConfig(builder);
        }
        return config;
    }
}
