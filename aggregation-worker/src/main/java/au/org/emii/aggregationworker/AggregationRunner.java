package au.org.emii.aggregationworker;


import au.org.aodn.aws.exception.EmailException;
import au.org.aodn.aws.util.EmailService;
import au.org.aodn.aws.wps.request.ExecuteRequestHelper;
import au.org.aodn.aws.wps.request.XmlRequestParser;
import au.org.aodn.aws.wps.status.EnumStatus;
import au.org.aodn.aws.wps.status.ExecuteStatusBuilder;
import au.org.aodn.aws.wps.status.S3JobFileManager;
import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.aggregator.NetcdfAggregator;
import au.org.emii.aggregator.catalogue.CatalogueReader;
import au.org.emii.aggregator.converter.Converter;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.download.*;
import au.org.emii.geoserver.client.HttpIndexReader;
import au.org.emii.geoserver.client.SubsetParameters;
import au.org.emii.util.IntegerHelper;
import au.org.emii.util.ProvenanceWriter;
import freemarker.template.Configuration;
import net.opengis.wps.v_1_0_0.Execute;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static au.org.aodn.aws.wps.status.WpsConfig.*;
import static au.org.emii.aggregator.au.org.emii.aggregator.config.AggregationOverridesReader.getAggregationOverrides;
import static au.org.emii.aggregator.au.org.emii.aggregator.config.DownloadConfigReader.getDownloadConfig;

@Component
public class AggregationRunner implements CommandLineRunner {

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 60000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 60000;

    private static final Logger logger = LoggerFactory.getLogger(au.org.emii.aggregationworker.AggregationRunner.class);

    private String statusS3Bucket = null, statusFilename = null, requestFilename = null;

    /**
     * Entry point for the aggregation.  Relies on command-line parameters.
     *
     * @param args
     */
    @Override
    public void run(String... args) {

        DateTime startTime = new DateTime(DateTimeZone.UTC);
        S3JobFileManager statusFileManager = null;
        String batchJobId = null, email = null;
        EmailService emailService = null;
        ExecuteStatusBuilder statusBuilder = null;
        Path downloadDirectory = null;

        try {
            //  Capture the AWS job specifics - they are passed to the docker runtime as
            //  environment variables.
            batchJobId = WpsConfig.getConfig(AWS_BATCH_JOB_ID_CONFIG_KEY);
            String awsBatchComputeEnvName = WpsConfig.getConfig(AWS_BATCH_CE_NAME_CONFIG_KEY);
            String awsBatchQueueName = WpsConfig.getConfig(AWS_BATCH_JQ_NAME_CONFIG_KEY);

            //  These values are passed as environment variables set in the AWS Batch job definition
            String outputBucketName = WpsConfig.getConfig(OUTPUT_S3_BUCKET_CONFIG_KEY);
            String outputFilename = WpsConfig.getConfig(OUTPUT_S3_FILENAME_CONFIG_KEY);
            statusS3Bucket = WpsConfig.getConfig(STATUS_S3_BUCKET_CONFIG_KEY);
            String jobFileS3KeyPrefix = WpsConfig.getConfig(AWS_BATCH_JOB_S3_KEY_PREFIX);
            statusFilename = WpsConfig.getConfig(STATUS_S3_FILENAME_CONFIG_KEY);
            requestFilename = WpsConfig.getConfig(REQUEST_S3_FILENAME_CONFIG_KEY);
            Path workingDir = Paths.get(WpsConfig.getConfig(WORKING_DIR_CONFIG_KEY));
            Path jobDir = Files.createTempDirectory(workingDir, batchJobId);

            String aggregatorConfigS3Bucket = WpsConfig.getConfig(AGGREGATOR_CONFIG_S3_BUCKET_CONFIG_KEY);
            String aggregatorTemplateFileS3Key = WpsConfig.getConfig(AGGREGATOR_TEMPLATE_FILE_S3_KEY_CONFIG_KEY);
            String aggregatorProvenanceTemplateS3Key = WpsConfig.getConfig(PROVENANCE_TEMPLATE_S3_KEY_CONFIG_KEY);

            //  Parse connect timeout
            String downloadConnectTimeoutString = WpsConfig.getConfig(DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY);
            int downloadConnectTimeout;
            if (downloadConnectTimeoutString != null && IntegerHelper.isInteger(downloadConnectTimeoutString)) {
                downloadConnectTimeout = Integer.parseInt(downloadConnectTimeoutString);
            } else {
                downloadConnectTimeout = DEFAULT_CONNECT_TIMEOUT_MS;
            }

            // Parse read timeout
            String downloadReadTimeoutString = WpsConfig.getConfig(DOWNLOAD_READ_TIMEOUT_CONFIG_KEY);
            int downloadReadTimeout;
            if (downloadReadTimeoutString != null && IntegerHelper.isInteger(downloadReadTimeoutString)) {
                downloadReadTimeout = Integer.parseInt(downloadConnectTimeoutString);
            } else {
                downloadReadTimeout = DEFAULT_READ_TIMEOUT_MS;
            }

            //  TODO:  null check and act on null configuration
            //  TODO : validate configuration

            statusBuilder = new ExecuteStatusBuilder(batchJobId, statusS3Bucket, statusFilename);
            String statusDocument = statusBuilder.createResponseDocument(EnumStatus.STARTED, GOGODUCK_PROCESS_IDENTIFIER, null, null, null);

            //  Update status document to indicate job has started
            statusFileManager = new S3JobFileManager(statusS3Bucket, jobFileS3KeyPrefix, batchJobId);
            statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);

            logger.info("AWS BATCH JOB ID     : " + batchJobId);
            logger.info("AWS BATCH CE NAME    : " + awsBatchComputeEnvName);
            logger.info("AWS BATCH QUEUE NAME : " + awsBatchQueueName);
            logger.info("-----------------------------------------------------");

            // Get request details
            String requestFileContent = statusFileManager.read(requestFilename);
            XmlRequestParser parser = new XmlRequestParser();
            Execute request = (Execute) parser.parse(requestFileContent);

            // Get inputs
            ExecuteRequestHelper requestHelper = new ExecuteRequestHelper(request);
            String layer = requestHelper.getLiteralInputValue("layer");
            String subset = requestHelper.getLiteralInputValue("subset");
            email = requestHelper.getEmail();

            // Determine required output mime type
            String requestedMimeType = requestHelper.getRequestedMimeType("result");
            String resultMime = requestedMimeType != null ? requestedMimeType : "application/x-netcdf";

            SubsetParameters subsetParams = SubsetParameters.parse(subset);

            if (email != null) {
                emailService = new EmailService();
            }

            logger.info("Command line parameters passed:");
            logger.info("Layer name             = " + layer);
            logger.info("Subset parameters      = " + subset);
            logger.info("Request MIME type      = " + resultMime);
            logger.info("Callback email address = " + email);

            //  TODO: Qa the parameters/settings passed?

            //  Query geoserver to get a list of files for the aggregation
            HttpIndexReader indexReader = new HttpIndexReader(WpsConfig.getConfig(WpsConfig.GEOSERVER_CATALOGUE_ENDPOINT_URL_CONFIG_KEY));
            List<DownloadRequest> downloads = indexReader.getDownloadRequestList(layer, "time", "file_url", subsetParams);

            //  Apply subset parameters
            LatLonRect bbox = subsetParams.getBbox();
            if (bbox != null) {
                logger.info("Bounding box: LAT [" + bbox.getLatMin() + ", " + bbox.getLatMax() + "], LON [" + bbox.getLonMin() + ", " + bbox.getLonMax() + "]");
            }

            CalendarDateRange subsetTimeRange = subsetParams.getTimeRange();
            if (subsetTimeRange != null) {
                logger.info("Time range specified for aggregation: START [" + subsetTimeRange.getStart() + "], END [" + subsetTimeRange.getEnd() + "]");
            }

            //  Apply overrides (if provided)
            AggregationOverrides overrides = getAggregationOverrides(aggregatorConfigS3Bucket, aggregatorTemplateFileS3Key, layer);

            // Create a directory for downloads in working directory
            downloadDirectory = jobDir.resolve("downloads");
            Files.createDirectory(downloadDirectory);

            //  Apply download configuration
            DownloadConfig downloadConfig = getDownloadConfig(downloadDirectory);

            //  Apply connect/read timeouts
            Downloader downloader = new Downloader(downloadConnectTimeout, downloadReadTimeout);

            Path outputFile = Files.createTempFile(jobDir, "agg", ".nc");
            Path convertedFile = null;

            long chunkSize = Long.valueOf(WpsConfig.getConfig(CHUNK_SIZE_KEY));

            try (
                    ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
                    NetcdfAggregator netcdfAggregator = new NetcdfAggregator(outputFile, overrides, chunkSize, bbox, subsetParams.getVerticalRange(), subsetTimeRange)
            ) {
                for (Download download : downloadManager.download(new LinkedHashSet<>(downloads))) {
                    netcdfAggregator.add(download.getPath());
                    downloadManager.remove();
                }

                HashMap<String, String> outputMap = new HashMap<>();

                //  Perform the conversion
                //  Instantiate the correct converter for the requested mimeType & do the conversion
                Converter converter = Converter.newInstance(resultMime);

                convertedFile = jobDir.resolve("converted" + converter.getExtension());
                converter.convert(outputFile, convertedFile);

                S3JobFileManager outputFileManager = new S3JobFileManager(outputBucketName, jobFileS3KeyPrefix, batchJobId);
                String fullOutputFilename = outputFilename + "." + converter.getExtension();
                outputFileManager.upload(convertedFile.toFile(), fullOutputFilename, resultMime);

                String resultUrl = WpsConfig.getS3ExternalURL(outputBucketName,
                    outputFileManager.getJobFileKey(fullOutputFilename));

                if (requestHelper.hasRequestedOutput("result")) {
                    outputMap.put("result", resultUrl);
                }

                if (requestHelper.hasRequestedOutput("provenance")) {
                    //  Lookup the metadata URL for the layer
                    String catalogueURL = WpsConfig.getConfig(GEONETWORK_CATALOGUE_URL_CONFIG_KEY);
                    String layerSearchField = WpsConfig.getConfig(GEONETWORK_CATALOGUE_LAYER_FIELD_CONFIG_KEY);
                    CatalogueReader catalogueReader = new CatalogueReader(catalogueURL, layerSearchField);

                    // Create provenance document
                    Configuration config = new Configuration();
                    config.setClassForTemplateLoading(ProvenanceWriter.class, "");

                    Map<String, Object> params = new HashMap<>();
                    params.put("jobId", batchJobId);
                    params.put("downloadUrl", resultUrl);
                    params.put("settingsPath", WpsConfig.getS3ExternalURL(aggregatorConfigS3Bucket, aggregatorTemplateFileS3Key));
                    params.put("startTime", startTime);
                    params.put("endTime", new DateTime(DateTimeZone.UTC));
                    params.put("layer", layer);
                    params.put("parameters", subsetParams);
                    params.put("sourceMetadataUrl", catalogueReader.getMetadataUrl(layer));
                    String provenanceDocument = ProvenanceWriter.write(aggregatorConfigS3Bucket, aggregatorProvenanceTemplateS3Key, params);

                    //  Upload provenance document to S3
                    //  TODO: configurable provenance filename?
                    outputFileManager.write(provenanceDocument, "provenance.xml", PROVENANCE_FILE_MIME_TYPE);

                    String provenanceUrl = WpsConfig.getS3ExternalURL(outputBucketName,
                        outputFileManager.getJobFileKey("provenance.xml"));

                    outputMap.put("provenance", provenanceUrl);
                }

                statusDocument = statusBuilder.createResponseDocument(EnumStatus.SUCCEEDED, GOGODUCK_PROCESS_IDENTIFIER, null, null, outputMap);
                statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);

                if (email != null) {
                    emailService.sendCompletedJobEmail(email, batchJobId, resultUrl, WpsConfig.getJobExpiration());
                }
            } finally {
                if (jobDir != null) {
                    FileUtils.deleteDirectory(jobDir.toFile());
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            if (statusFileManager != null) {
                if (batchJobId != null) {
                    String statusDocument = null;
                    try {
                        statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, GOGODUCK_PROCESS_IDENTIFIER,"Exception occurred during aggregation :" + e.getMessage(), "AggregationError", null);
                        statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);
                    } catch (IOException ioe) {
                        logger.error("Unable to update status. Status: " + statusDocument);
                        ioe.printStackTrace();
                    }
                }
            }

            if (email != null) {
                try {
                    emailService.sendFailedJobEmail(email, batchJobId);
                } catch (EmailException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            System.exit(1);
        }
    }
}
