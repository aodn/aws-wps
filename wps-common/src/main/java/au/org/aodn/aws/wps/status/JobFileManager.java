package au.org.aodn.aws.wps.status;

import au.org.aodn.aws.util.S3Storage;
import au.org.aodn.aws.wps.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class JobFileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobFileManager.class);

    private final String s3bucket;
    private final String s3JobPrefix;
    private final String jobId;
    private final Storage<?> storage;

    public JobFileManager(Storage<?> storage, String s3bucket, String s3JobPrefix, String jobId) {
        this.storage = storage;
        this.s3bucket = s3bucket;
        this.s3JobPrefix = s3JobPrefix;
        this.jobId = jobId;
    }

    public String read(String fileName) throws IOException {
        String jobFileKey = getJobFileKey(fileName);
        LOGGER.info(String.format("Reading from bucket %s key %s", s3bucket, jobFileKey));
        return storage.readObjectAsString(s3bucket, jobFileKey);
    }

    public void write(String content, String fileName, String contentType) throws IOException {
        String jobFileKey = getJobFileKey(fileName);
        LOGGER.info(String.format("Writing to bucket %s key %s", s3bucket, jobFileKey));
        storage.uploadToTarget(content, s3bucket, jobFileKey, contentType);
    }

    public void upload(File file, String filename, String contentType) throws InterruptedException, IOException {
        String jobFileKey = getJobFileKey(filename);
        LOGGER.info(String.format("Uploading %s to bucket %s key %s", file.toString(), s3bucket, jobFileKey));
        storage.uploadToTarget(file, s3bucket, jobFileKey, contentType);
    }

    public String getJobFileKey(String fileName) {
        return String.format("%s%s/%s", s3JobPrefix, jobId, fileName);
    }

}
