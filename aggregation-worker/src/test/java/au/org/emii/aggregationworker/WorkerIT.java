package au.org.emii.aggregationworker;

import au.org.aodn.aws.wps.LocalStorage;

import au.org.aodn.aws.wps.status.WpsConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles({"test"})
public class WorkerIT {

    protected Logger logger = LoggerFactory.getLogger(WorkerIT.class);

    @Autowired
    protected LocalStorage localStorage;

    @Autowired
    protected AggregationWorker runner;

    protected String getJobWorkPath() {
        return String.format("%s/%s/%s",
                WpsConfig.getProperty(WpsConfig.OUTPUT_S3_BUCKET_CONFIG_KEY),
                WpsConfig.getProperty(WpsConfig.AWS_BATCH_JOB_S3_KEY_PREFIX),
                WpsConfig.getProperty(WpsConfig.AWS_BATCH_JOB_ID_CONFIG_KEY));
    }

    protected String getStatusXml() throws IOException {
        return localStorage.readObjectAsString(getJobWorkPath(), "status.xml");
    }

    protected void writeRequestXml(File source) throws IOException {
        logger.info("Copy request1.xml to {}", getJobWorkPath());
        FileUtils.copyFile(source, new File( getJobWorkPath() + File.separator + "request.xml"));
    }

    @Test
    public void verifyRequest1Processed() throws IOException {
        // First copy the request file to target location
        String uuid = UUID.randomUUID().toString();
        System.setProperty(WpsConfig.AWS_BATCH_JOB_ID_CONFIG_KEY, uuid);

        File f = ResourceUtils.getFile("classpath:request1.xml");
        writeRequestXml(f);

        runner.start();

        assertDoesNotThrow(() -> {
            assertTrue("Expect job completed",
                    getStatusXml().contains("Job " + uuid + " has completed"));
        });
    }

    @Test
    public void verifyRequest2Processed() throws IOException {
        // First copy the request file to target location
        String uuid = UUID.randomUUID().toString();
        System.setProperty(WpsConfig.AWS_BATCH_JOB_ID_CONFIG_KEY, uuid);

        File f = ResourceUtils.getFile("classpath:request2.xml");
        writeRequestXml(f);

        runner.start();

        assertDoesNotThrow(() -> {
            assertTrue("Expect job completed",
                    getStatusXml().contains("Job " + uuid + " has completed"));
        });
    }

    @Test
    public void verifyNoRequestShowExceptionTextInStatus() {
        // Give a random jobid so it will not crash across test
        String uuid = UUID.randomUUID().toString();
        System.setProperty(WpsConfig.AWS_BATCH_JOB_ID_CONFIG_KEY, uuid);
        runner.start();

        assertDoesNotThrow(() -> {
            String x = getStatusXml();
            logger.info(x);

            assertTrue("Expect exception in processing",
                    x.contains("Exception occurred during aggregation : jakarta.xml.bind.UnmarshalException"));
        });
    }
}
