package au.org.emii.download;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static au.org.emii.util.IntegerHelper.suffix;

/**
 * Manages the parallel download of a list of files to local storage
 * limiting the amount of local storage used by only downloading up
 * to the configured storage limit and then waiting until previously
 * downloaded files are accessed and deleted before downloading
 * more files.
 *
 * Downloads are returned in the order provided, blocking
 * if necessary until the download has been completed
 *
 * Example usage:
 *
 *     DownloadConfig config = new DownloadConfig.ConfigBuilder()
 *                          .downloadDirectory(Paths.get("/tmp"))
 *                          .localStorageLimit(200 * 1024 * 1024)
 *                          .poolSize(8)
 *                          .build();
 *     Downloader downloader = new Downloader(60 * 1000, 60 * 1000);
 *
 *     try (ParallelDownloadManager downloadManager = new ParallelDownloadManager(config, downloader, pool) {
 *         for (Download download : downloadManager.download(requests)) {
 *             // do something with the download
 *             downloadManager.remove();  // removes current download freeing up space for more downloads
 *         }
 *     } catch (DownloadException e) {
 *         // handle download exception
 *     }
 */

public class ParallelDownloadManager implements Iterable<Download>, Iterator<Download>, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ParallelDownloadManager.class);
    private final DownloadConfig config;
    private final LinkedList<DownloadRequest> unactionedQueue;
    private final LinkedList<Future<Download>> inProgressQueue;
    private final Downloader downloader;
    private final ExecutorService pool;
    private Download previous = null;
    private long localStorageAllocated = 0L;

    public ParallelDownloadManager(DownloadConfig config, Downloader downloader) {
        this.config = config;
        this.unactionedQueue = new LinkedList<>();
        this.inProgressQueue = new LinkedList<>();
        this.downloader = downloader;
        this.pool = Executors.newFixedThreadPool(config.getPoolSize());
    }

    public Iterable<Download> download(Set<DownloadRequest> downloadRequests) {
        unactionedQueue.addAll(downloadRequests);
        downloadUpToStorageLimit();
        return this;
    }

    // Iterable methods

    @Override
    public Iterator<Download> iterator() {
        return this;
    }

    // Iterator methods

    @Override
    public boolean hasNext() {
        return unactionedQueue.size() > 0 || inProgressQueue.size() > 0;
    }

    @Override
    public Download next() {
        try {
            Future<Download> next = inProgressQueue.remove();
            Download result = next.get(); // blocks until the next download is complete
            previous = result;
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof DownloadException) {
                throw (DownloadException)ee.getCause();
            } else {
                throw new RuntimeException(ee.getCause());
            }
        }
    }

    @Override
    public void remove() {
        try {
            logger.debug(String.format("Deleting download from %s", previous.getURL()));
            Files.deleteIfExists(previous.getPath());
            localStorageAllocated -= previous.getSize();
            downloadUpToStorageLimit();
        } catch (IOException e) {
            throw new RuntimeException(
                String.format("Unexpected system error: unable to delete downloaded file %s", previous.getPath()), e);
        }
    }

    // AutoCloseable methods

    @Override
    public void close() {
        pool.shutdownNow();
    }

    private void downloadUpToStorageLimit() {
        DownloadRequest next = unactionedQueue.peek();

        while (next != null && (localStorageAllocated == 0 || wontExceedLocalStorageLimit(next))) {
            final DownloadRequest request = unactionedQueue.pop();
            logger.debug(String.format("Adding %s to download queue", request.getUrl()));
            inProgressQueue.add(pool.submit(new DownloadCallable(request)));
            localStorageAllocated += request.getSize();
            next = unactionedQueue.peek();
        }
    }

    private boolean wontExceedLocalStorageLimit(DownloadRequest next) {
        return localStorageAllocated + next.getSize() <= config.getLocalStorageLimit();
    }

    private class DownloadCallable implements Callable<Download> {
        private final DownloadRequest request;

        public DownloadCallable(DownloadRequest request) {
            this.request = request;
        }

        @Override
        public Download call() throws DownloadException {
            logger.debug(String.format("Downloading %s", request.getUrl()));
            int attempt = 1;
            Download result = download();
            while (result instanceof DownloadError && ++attempt <= config.getDownloadAttempts()) {
                sleep(config.getRetryInterval());
                logger.debug(
                    String.format("Downloading %s %s%s attempt", request.getUrl(), attempt, suffix(attempt)));
                result = download();
            }
            if (result instanceof DownloadError) {
                throw new DownloadException(String.format("Error downloading %s", request.getUrl()), ((DownloadError)result).getCause());
            } else {
                logger.info(String.format("Downloaded %s", request.getUrl()));
            }
            return result;
        }

        private Download download() {
            String fileName = FilenameUtils.getName(request.getUrl().getPath());
            Path path = config.getDownloadDirectory().resolve(UUID.randomUUID().toString() + fileName);

            try {
                downloader.download(request.getUrl(), path);
                return new Download(request.getUrl(), path, request.getSize());
            } catch (IOException e) {
                logger.warn(String.format("Could not download %s", request.getUrl()), e);
                return new DownloadError(request.getUrl(), path, request.getSize(), e);
            }
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Unexpected system error: interrupted retrying download", e);
            }
        }

    }

}
