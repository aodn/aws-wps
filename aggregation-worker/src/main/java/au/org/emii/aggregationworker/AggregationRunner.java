package au.org.emii.aggregationworker;


import au.org.aodn.aws.exception.EmailException;
import au.org.aodn.aws.util.EmailService;
import au.org.aodn.aws.util.S3Utils;
import au.org.aodn.aws.wps.request.ExecuteRequestHelper;
import au.org.aodn.aws.wps.request.XmlRequestParser;
import au.org.aodn.aws.wps.status.EnumStatus;
import au.org.aodn.aws.wps.status.ExecuteStatusBuilder;
import au.org.aodn.aws.wps.status.S3JobFileManager;
import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.aggregator.NetcdfAggregator;
import au.org.emii.aggregator.catalogue.CatalogueReader;
import au.org.emii.aggregator.converter.Converter;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.download.*;
import au.org.emii.geoserver.client.HttpIndexReader;
import au.org.emii.geoserver.client.SubsetParameters;
import au.org.emii.geoserver.client.TimeNotSupportedException;
import au.org.emii.util.IntegerHelper;
import au.org.emii.util.NumberRange;
import au.org.emii.util.ProvenanceWriter;
import com.amazonaws.AmazonServiceException;
import freemarker.template.Configuration;
import net.opengis.wps.v_1_0_0.Execute;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
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

    public static final String SUMOLOGIC_LOG_APPENDER_NAME = "SumoAppender";
    private static final String PROVENANCE_TEMPLATE_GRIDDED = "provenance_template_gridded.ftl";

    private static final Logger logger = LogManager.getRootLogger();

    private String statusS3Bucket = null, statusFilename = null, requestFilename = null;

    /**
     * Entry point for the aggregation.  Relies on command-line parameters.
     *
     * @param args
     */
    @Override
    public void run(String... args) {

        //  Log environment variables
        logger.info("Environment Variables");
        for (String key : System.getenv().keySet()) {
            logger.info(String.format("%s = %s", key, System.getenv(key)));
        }

        checkLoggingConfiguration();

        DateTime startTime = new DateTime(DateTimeZone.UTC);

        S3JobFileManager statusFileManager = null;
        String batchJobId = null;
        String contactEmail = null;
        String administratorEmail = null;
        EmailService emailService = null;
        ExecuteStatusBuilder statusBuilder = null;
        Path downloadDirectory;

        try {
            //  Capture the AWS job specifics - they are passed to the docker runtime as
            //  environment variables.
            batchJobId = WpsConfig.getProperty(AWS_BATCH_JOB_ID_CONFIG_KEY);
            String awsBatchComputeEnvName = WpsConfig.getProperty(AWS_BATCH_CE_NAME_CONFIG_KEY);
            String awsBatchQueueName = WpsConfig.getProperty(AWS_BATCH_JQ_NAME_CONFIG_KEY);

            //  These values are passed as environment variables set in the AWS Batch job definition
            String outputBucketName = WpsConfig.getProperty(OUTPUT_S3_BUCKET_CONFIG_KEY);
            String outputFilename = WpsConfig.getProperty(OUTPUT_S3_FILENAME_CONFIG_KEY);
            statusS3Bucket = WpsConfig.getProperty(OUTPUT_S3_BUCKET_CONFIG_KEY);
            String jobFileS3KeyPrefix = WpsConfig.getProperty(AWS_BATCH_JOB_S3_KEY_PREFIX);
            statusFilename = WpsConfig.getProperty(STATUS_S3_FILENAME_CONFIG_KEY);
            requestFilename = WpsConfig.getProperty(REQUEST_S3_FILENAME_CONFIG_KEY);
            Path workingDir = Paths.get(WpsConfig.getProperty(WORKING_DIR_CONFIG_KEY));
            Path jobDir = Files.createTempDirectory(workingDir, batchJobId);
            administratorEmail = WpsConfig.getProperty(WpsConfig.ADMINISTRATOR_EMAIL);
            String aggregatorTemplateFileURL = WpsConfig.getProperty(AGGREGATOR_TEMPLATE_FILE_URL_KEY);

            //  Parse connect timeout
            String downloadConnectTimeoutString = WpsConfig.getProperty(DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY);
            int downloadConnectTimeout;
            if (downloadConnectTimeoutString != null && IntegerHelper.isInteger(downloadConnectTimeoutString)) {
                downloadConnectTimeout = Integer.parseInt(downloadConnectTimeoutString);
            } else {
                downloadConnectTimeout = DEFAULT_CONNECT_TIMEOUT_MS;
            }

            // Parse read timeout
            String downloadReadTimeoutString = WpsConfig.getProperty(DOWNLOAD_READ_TIMEOUT_CONFIG_KEY);
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
            contactEmail = requestHelper.getEmail();

            // Determine required output mime type
            String requestedMimeType = requestHelper.getRequestedMimeType("result");
            String resultMime = requestedMimeType != null ? requestedMimeType : "application/x-netcdf";

            HttpIndexReader indexReader = new HttpIndexReader(WpsConfig.getProperty(WpsConfig.GEOSERVER_CATALOGUE_ENDPOINT_URL_CONFIG_KEY));

            SubsetParameters subsetParams = SubsetParameters.parse(subset);

            //  Initialise email service
            emailService = new EmailService();

            logger.info("Running aggregation job. JobID [" + batchJobId + "]. Layer [" + layer + "], Subset [" + subset + "], Result MIME [" + resultMime + "], Callback email [" + contactEmail + "]");

            //  TODO: Qa the parameters/settings passed?

            //  Apply subset parameters
            LatLonRect bbox = subsetParams.getBbox();
            if (bbox != null) {
                logger.info("Bounding box: LAT [" + bbox.getLatMin() + ", " + bbox.getLatMax() + "], LON [" + bbox.getLonMin() + ", " + bbox.getLonMax() + "]");
            }

            //  Use the supplied time extent (unless it is a test transaction)
            CalendarDateRange subsetTimeRange  = subsetParams.getTimeRange();

            //  If the transaction is a test transaction - we want to limit the temporal extent to a small (one time step) extent.
            //  We'll just limit the test transaction to the latest timestep.
            if(ExecuteRequestHelper.isTestTransaction(request)) {
                logger.info("TEST TRANSACTION.  Adjusting temporal extent to a single timestep (latest)");

                String timestamp = null;
                try {
                    timestamp = indexReader.getLatestTimeStep(layer, "time");
                    if (timestamp != null) {
                        logger.info("Last timestamp for layer [" + layer + "] = " + timestamp);
                        DateTime dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(timestamp);
                        subsetTimeRange = CalendarDateRange.of(dateTime.toDate(), dateTime.toDate());
                    } else {
                        logger.error("Unable to determine latest timestep for layer [" + layer + "].");
                        throw new AggregationException("Unable to determine latest timestep for test transaction. Layer [" + layer + "]");
                    }
                } catch(TimeNotSupportedException ex) {
                    logger.info("Time parameter does not appear to be supported for this layer.");
                }
            }

            if (subsetTimeRange != null) {
                logger.info("Time range specified for aggregation: START [" + subsetTimeRange.getStart() + "], END [" + subsetTimeRange.getEnd() + "]");
            }

            NumberRange depthRange = subsetParams.getVerticalRange();
            if (depthRange != null) {
                logger.info("Z range specified for aggregation: START [" + depthRange.getMin() + "], END [" + depthRange.getMax() + "]");
            }

            //  Query geoserver to get a list of files for the aggregation
            List<DownloadRequest> downloads = indexReader.getDownloadRequestList(layer, "time", "file_url", subsetTimeRange);

            //  Apply overrides (if provided)
            AggregationOverrides overrides = getAggregationOverrides(aggregatorTemplateFileURL, layer);

            // Create a directory for downloads in working directory
            downloadDirectory = jobDir.resolve("downloads");
            Files.createDirectory(downloadDirectory);

            //  Apply download configuration
            DownloadConfig downloadConfig = getDownloadConfig(downloadDirectory);

            //  Apply connect/read timeouts
            Downloader downloader = new Downloader(downloadConnectTimeout, downloadReadTimeout);

            Path outputFile = Files.createTempFile(jobDir, "agg", ".nc");
            Path convertedFile = null;

            long chunkSize = Long.valueOf(WpsConfig.getProperty(CHUNK_SIZE_KEY));

            try (
                    ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
                    NetcdfAggregator netcdfAggregator = new NetcdfAggregator(outputFile, overrides, chunkSize, bbox, depthRange, subsetTimeRange)
            ) {
                logger.info("Commencing download of [" + downloads.size() + "] files.");

                for (Download download : downloadManager.download(new LinkedHashSet<>(downloads))) {
                    netcdfAggregator.add(download.getPath());
                    downloadManager.remove();
                }

                logger.info("Raw aggregated file size [" + outputFile.toFile().length() + " bytes]");

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

                //  URL for the status page for this job
                String statusUrl = WpsConfig.getStatusServiceHtmlEndpoint(batchJobId);

                if (requestHelper.hasRequestedOutput("result")) {
                    outputMap.put("result", resultUrl);
                }

                if (requestHelper.hasRequestedOutput("provenance")) {
                    logger.info("Provenance output requested.");

                    //  Lookup the metadata URL for the layer
                    String catalogueURL = WpsConfig.getProperty(GEONETWORK_CATALOGUE_URL_CONFIG_KEY);
                    String layerSearchField = WpsConfig.getProperty(GEONETWORK_CATALOGUE_LAYER_FIELD_CONFIG_KEY);
                    CatalogueReader catalogueReader = new CatalogueReader(catalogueURL, layerSearchField);

                    // Create provenance document
                    Configuration config = new Configuration();
                    config.setClassForTemplateLoading(ProvenanceWriter.class, "");

                    Map<String, Object> params = new HashMap<>();
                    params.put("jobId", batchJobId);
                    params.put("downloadUrl", resultUrl);
                    params.put("settingsPath", aggregatorTemplateFileURL);
                    params.put("startTime", startTime);
                    params.put("endTime", new DateTime(DateTimeZone.UTC));
                    params.put("layer", layer);
                    params.put("parameters", subsetParams);
                    params.put("sourceMetadataUrl", catalogueReader.getMetadataUrl(layer));
                    String provenanceDocument = ProvenanceWriter.write(PROVENANCE_TEMPLATE_GRIDDED, params);

                    //  Upload provenance document to S3
                    outputFileManager.write(provenanceDocument, "provenance.xml", PROVENANCE_FILE_MIME_TYPE);

                    String provenanceUrl = WpsConfig.getS3ExternalURL(outputBucketName,
                            outputFileManager.getJobFileKey("provenance.xml"));

                    outputMap.put("provenance", provenanceUrl);
                }


                //  Calculate period elapsed during aggregation
                DateTime stopTime = new DateTime(DateTimeZone.UTC);
                Period elapsedPeriod = new Period(startTime, stopTime);
                String elapsedTimeString = formatPeriodString(elapsedPeriod);

                logger.info("Aggregation completed successfully. JobID [" + batchJobId + "], Callback email [" + contactEmail + "], Size bytes [" + convertedFile.toFile().length() + "], Elapsed time (h:m:s) [" + elapsedTimeString + "]");
                statusDocument = statusBuilder.createResponseDocument(EnumStatus.SUCCEEDED, GOGODUCK_PROCESS_IDENTIFIER, null, null, outputMap);
                statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);

                //  Send email - if email address was provided
                if (contactEmail != null) {
                    try {
                        //  Send completed job email to user
                        emailService.sendCompletedJobEmail(contactEmail, batchJobId, statusUrl, S3Utils.getExpirationinDays(outputBucketName));
                    } catch (EmailException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }

            } finally {
                if (jobDir != null) {
                    FileUtils.deleteDirectory(jobDir.toFile());
                }
            }
        } catch(AmazonServiceException se) {
            String errorMessage = "An amazon service exception occurred processing job [" + batchJobId + "] : " +
                                  "Message [" + se.getMessage() + "]" +
                                  ", ErrorCode [" + se.getErrorCode() + "]" +
                                  ", ErrorMessage [" + se.getErrorMessage() + "]" +
                                  ", ErrorType [" + se.getErrorType().name() + "]";

            logger.error(errorMessage, se);

            //  Flush messages etc...
            LogManager.shutdown();

            //  Exit with a failed return code - means batch job will retry (unless max retries reached)
            System.exit(1);

        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("Failed aggregation. JobID [" + batchJobId + "], Callback email [" + contactEmail + "] : " + e.getMessage(), e);
            if (statusFileManager != null) {
                if (batchJobId != null) {
                    String statusDocument = null;
                    try {
                        statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, GOGODUCK_PROCESS_IDENTIFIER,"Exception occurred during aggregation : " + e.getMessage(), "AggregationError", null);
                        statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);
                    } catch (IOException ioe) {
                        logger.error("Unable to update status for job [" + batchJobId + "]. Status: " + statusDocument);
                        ioe.printStackTrace();
                    }
                }
            }

            //  Send failed job email to user
            if (contactEmail != null) {
                try {
                    emailService.sendFailedJobEmail(contactEmail, administratorEmail, batchJobId);
                } catch (EmailException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            //  Flush messages etc...
            LogManager.shutdown();

            //  Exit with a 'success' return code - will mean job will not retry
            System.exit(0);
        }
    }


    private void checkLoggingConfiguration() {
        //  If we don't have a sumo endpoint defined - but we do have a Sumo log appender configured
        //  we should remove the appender - because it isn't configured properly anyway.
        if(WpsConfig.getProperty(WpsConfig.SUMOLOGIC_ENDPOINT_ENV_VARIABLE_NAME) == null) {
            org.apache.logging.log4j.core.config.Configuration logConfig = ((LoggerContext) LogManager.getContext()).getConfiguration();
            Map<String, Appender> appenderMap = logConfig.getAppenders();
            Collection<Appender> appenders = appenderMap.values();
            for(Appender currentAppender : appenders) {
                if(currentAppender.getName().equalsIgnoreCase(SUMOLOGIC_LOG_APPENDER_NAME)) {
                    LoggerContext context = (LoggerContext) LogManager.getContext();
                    logConfig.getRootLogger().removeAppender(SUMOLOGIC_LOG_APPENDER_NAME);
                    context.updateLoggers();

                    logger.info("Removed SumoLogic Log Appender [" + SUMOLOGIC_LOG_APPENDER_NAME + "] - no endpoint set.");
                }
            }
        }
    }


    private String formatPeriodString(Period period) {
        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(2)
                .appendHours().appendSeparator(":")
                .minimumPrintedDigits(2)
                .appendMinutes().appendSeparator(":")
                .minimumPrintedDigits(2)
                .appendSeconds()
                .toFormatter();

        return formatter.print(period);
    }
}
