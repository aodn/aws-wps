package au.org.emii.download;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration setting for the ParallelDownloadManager
 */
public class DownloadConfig {
    private final Path downloadDirectory;
    private final long localStorageLimit;
    private final int downloadAttempts;
    private final int retryInterval;
    private final int poolSize;

    public DownloadConfig(ConfigBuilder builder) {
        downloadDirectory = builder.downloadDirectory;
        localStorageLimit = builder.localStorageLimit;
        downloadAttempts = builder.downloadAttempts;
        retryInterval = builder.retryInterval;
        poolSize = builder.poolSize;
    }

    public Path getDownloadDirectory() {
        return downloadDirectory;
    }

    public long getLocalStorageLimit() {
        return localStorageLimit;
    }

    public int getDownloadAttempts() {
        return downloadAttempts;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public static class ConfigBuilder {
        private Path downloadDirectory;
        private long localStorageLimit;
        private int downloadAttempts;
        private int retryInterval;
        private int poolSize;

        public ConfigBuilder() {
            // defaults
            downloadDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
            localStorageLimit = 100 * 1024 * 1024; // 100MiB
            downloadAttempts = 3;
            retryInterval = 60 * 1000; // 60 seconds
            poolSize = 8;
        }

        public ConfigBuilder downloadDirectory(Path downloadDirectory) {
            this.downloadDirectory = downloadDirectory;
            return this;
        }

        public ConfigBuilder localStorageLimit(long localStorageLimit) {
            this.localStorageLimit = localStorageLimit;
            return this;
        }

        public ConfigBuilder downloadAttempts(int downloadAttempts) {
            this.downloadAttempts = downloadAttempts;
            return this;
        }

        public ConfigBuilder retryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public ConfigBuilder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public DownloadConfig build() {
            return new DownloadConfig(this);
        }

    }
}
