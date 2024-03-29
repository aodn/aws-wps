package au.org.emii.aggregationworker;


import au.org.aodn.aws.exception.EmailException;
import au.org.aodn.aws.util.EmailService;
import au.org.aodn.aws.util.S3Storage;
import au.org.aodn.aws.wps.Storage;
import au.org.aodn.aws.wps.request.ExecuteRequestHelper;
import au.org.aodn.aws.wps.request.XmlRequestParser;
import au.org.aodn.aws.wps.status.EnumStatus;
import au.org.aodn.aws.wps.status.ExecuteStatusBuilder;
import au.org.aodn.aws.wps.status.JobFileManager;
import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.aggregator.NetcdfAggregator;
import au.org.aodn.aws.geonetwork.CatalogueReader;
import au.org.emii.aggregator.converter.Converter;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.download.*;
import au.org.aodn.aws.geoserver.client.HttpIndexReader;
import au.org.aodn.aws.geoserver.client.SubsetParameters;
import au.org.aodn.aws.geoserver.client.TimeNotSupportedException;
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
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static au.org.aodn.aws.wps.status.WpsConfig.*;
import static au.org.emii.aggregator.config.AggregationOverridesReader.getAggregationOverrides;
import static au.org.emii.aggregator.config.DownloadConfigReader.getDownloadConfig;

public class AggregationWorker implements ExitCodeGenerator {

    public static final String SUMOLOGIC_LOG_APPENDER_NAME = "SumoAppender";
    private static final String PROVENANCE_TEMPLATE_GRIDDED = "provenance_template_gridded.ftl";
    private static final String METADATA_FILE_EXTENSION = ".xml";
    private static final String DEFAULT_METADATA_FILENAME = "metadata" + METADATA_FILE_EXTENSION;
    private static final String LITERAL_INPUT_IDENTIFIER_LAYER = "layer";
    private static final String LITERAL_INPUT_IDENTIFIER_SUBSET = "subset";
    private static final String LITERAL_INPUT_IDENTIFIER_FILENAME = "filename";
    private static final String LITERAL_INPUT_IDENTIFIER_AGGREGATION_MIME = "aggregationOutputMime";
    private static final String DEFAULT_OUTPUT_MIME = "application/x-netcdf";
    private static final String DOWNLOADS_DIRECTORY_NAME = "downloads";
    private static final String DEFAULT_OUTPUT_FILENAME = "IMOS_aggregation_";
    private static final String DEFAULT_OUTPUT_FILE_EXTENSION = ".zip";


    private static final Logger logger = LogManager.getRootLogger();
    private String statusS3Bucket = null;
    private String statusFilename = null;
    private String requestFilename = null;

    protected Storage<?> storage;

    @Value("${aggregationWorker.autoStart:true}")
    protected Boolean autoStart;

    @Autowired
    protected ApplicationContext applicationContext;

    protected Downloader downloader;

    protected int exitCode = 0;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AggregationWorker(Storage<?> storage, Downloader downloader) {
        this.storage = storage;
        this.downloader = downloader;
    }

    @PostConstruct
    public void init() {
        if(autoStart) {
            // We want to start and shutdown in production automatically but not good for unit testing.
            scheduler.schedule(() -> {
                // We want to give some time before work start so that spring completed the cycle of post construct
                start();
                scheduler.shutdown();
                System.exit(SpringApplication.exit(applicationContext));
            }, 3, TimeUnit.SECONDS);
        }
    }

    public void start() {

        //  Log environment variables
        logger.info("Environment Variables");
        for (String key : System.getenv().keySet()) {
            logger.info(String.format("%s = %s", key, System.getenv(key)));
        }

        checkLoggingConfiguration();

        DateTime startTime = new DateTime(DateTimeZone.UTC);

        JobFileManager statusFileManager = null;
        String batchJobId = null;
        String contactEmail = null;
        String administratorEmail = null;
        EmailService emailService = null;
        ExecuteStatusBuilder statusBuilder = null;
        Path downloadDirectory;
        SubsetParameters subsetParams = null;
        //  Try and determine the point of truth and the collection title
        String pointOfTruth = "";
        String collectionTitle = "";
        String metadataUuid = "";
        String resultUrl = null;

        try {
            //  Capture the AWS job specifics - they are passed to the docker runtime as
            //  environment variables.
            batchJobId = WpsConfig.getProperty(AWS_BATCH_JOB_ID_CONFIG_KEY);
            String awsBatchComputeEnvName = WpsConfig.getProperty(AWS_BATCH_CE_NAME_CONFIG_KEY);
            String awsBatchQueueName = WpsConfig.getProperty(AWS_BATCH_JQ_NAME_CONFIG_KEY);

            //  These values are passed as environment variables set in the AWS Batch job definition
            String outputBucketName = WpsConfig.getProperty(OUTPUT_S3_BUCKET_CONFIG_KEY);
            statusS3Bucket = WpsConfig.getProperty(OUTPUT_S3_BUCKET_CONFIG_KEY);

            String jobFileS3KeyPrefix = WpsConfig.getProperty(AWS_BATCH_JOB_S3_KEY_PREFIX);
            statusFilename = WpsConfig.getProperty(STATUS_S3_FILENAME_CONFIG_KEY);
            requestFilename = WpsConfig.getProperty(REQUEST_S3_FILENAME_CONFIG_KEY);

            Path workingDir = Paths.get(WpsConfig.getProperty(WORKING_DIR_CONFIG_KEY));
            Path jobDir = Files.createTempDirectory(workingDir, batchJobId);

            administratorEmail = WpsConfig.getProperty(WpsConfig.ADMINISTRATOR_EMAIL);
            String aggregatorTemplateFileURL = WpsConfig.getProperty(AGGREGATOR_TEMPLATE_FILE_URL_KEY);


            //  TODO:  null check and act on null configuration
            //  TODO : validate configuration

            statusBuilder = new ExecuteStatusBuilder(batchJobId, statusS3Bucket, statusFilename);

            //  Update status document to indicate job has started
            String statusDocument = statusBuilder.createResponseDocument(EnumStatus.STARTED, GOGODUCK_PROCESS_IDENTIFIER, null, null, null);
            statusFileManager = new JobFileManager(storage, statusS3Bucket, jobFileS3KeyPrefix, batchJobId);
            statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);


            logger.info("AWS BATCH JOB ID     : " + batchJobId);
            logger.info("AWS BATCH CE NAME    : " + awsBatchComputeEnvName);
            logger.info("AWS BATCH QUEUE NAME : " + awsBatchQueueName);
            logger.info("-----------------------------------------------------");


            //  Hold the content to be added to the output zip file
            List<File> zipContent = new ArrayList<>();

            // Get request details.  The request handler Lambda function writes the XML
            // request to S3.  We read it directly from there.
            String requestXML = statusFileManager.read(requestFilename);
            XmlRequestParser parser = new XmlRequestParser();
            Execute request = (Execute) parser.parse(requestXML);

            // Get inputs
            ExecuteRequestHelper requestHelper = new ExecuteRequestHelper(request);
            String layer = requestHelper.getLiteralInputValue(LITERAL_INPUT_IDENTIFIER_LAYER);
            String subset = requestHelper.getLiteralInputValue(LITERAL_INPUT_IDENTIFIER_SUBSET);
            String requestedOutputFilename = requestHelper.getLiteralInputValue(LITERAL_INPUT_IDENTIFIER_FILENAME);
            contactEmail = requestHelper.getEmail();

            //  Form a default filename if we weren't passed one
            if (requestedOutputFilename == null) {
                DateTimeFormatter isoFmt = ISODateTimeFormat.basicDateTimeNoMillis();
                logger.info("ISO date time format: " + isoFmt.print(startTime));
                String timestamp = isoFmt.print(startTime);
                requestedOutputFilename = DEFAULT_OUTPUT_FILENAME + timestamp;
            }

            // Determine required output mime type
            String resultMime = requestHelper.getRequestedMimeType("result") != null ? requestHelper.getRequestedMimeType("result") : DEFAULT_OUTPUT_MIME;
            logger.info("Requested mime type: " + resultMime);

            String aggregationOutputMime;

            //  If the client has requested a ZIP output for the result, we need to determine the mime type requested for
            //  the aggregation file.  The requested MIME will be passed as a literal input value called 'aggregationOutputMime'
            if(resultMime.equals("application/zip")) {
                aggregationOutputMime = requestHelper.getLiteralInputValue(LITERAL_INPUT_IDENTIFIER_AGGREGATION_MIME);
                if(aggregationOutputMime == null) {
                    aggregationOutputMime = DEFAULT_OUTPUT_MIME;
                }
            } else {
                aggregationOutputMime = resultMime;
            }

            //  If the client has requested a ZIP output - then write the request file
            //  to disk to be included in the ZIP
            if(resultMime.equals("application/zip")) {
                //  Write the request file locally to add to the zip file
                File localRequestFile = new File(jobDir.toFile(), requestFilename);
                try (FileWriter requestFileWriter = new FileWriter(localRequestFile)) {
                    requestFileWriter.write(requestXML);
                    requestFileWriter.flush();

                    //  Add to zip
                    zipContent.add(localRequestFile);
                }
            }

            //  Create a geonetwork index reader - this is used to lookup the list of files for the named layer & to
            //  work out the latest timestep (the latest file in the collection) for test transactions
            HttpIndexReader indexReader = new HttpIndexReader(WpsConfig.getProperty(WpsConfig.GEOSERVER_CATALOGUE_ENDPOINT_URL_CONFIG_KEY));

            //  Initialise email service
            emailService = new EmailService();

            //  Parse the subset parameters passed
            subsetParams = SubsetParameters.parse(subset);

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
            downloadDirectory = jobDir.resolve(DOWNLOADS_DIRECTORY_NAME);
            Files.createDirectory(downloadDirectory);

            //  Apply download configuration
            DownloadConfig downloadConfig = getDownloadConfig(downloadDirectory);

            //  Create a temp file as the destination for the aggregation
            Path outputFile = Files.createTempFile(jobDir, "agg", ".nc");
            Path convertedFile = null;

            long chunkSize = Long.valueOf(WpsConfig.getProperty(CHUNK_SIZE_KEY));

            //  Download and aggregate the files
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

                //  This map holds the WPS outputs to be returned in the ExecuteResponse
                HashMap<String, String> outputMap = new HashMap<>();

                //  Perform the conversion
                //  Instantiate the correct converter for the requested mimeType & do the conversion
                Converter converter = Converter.newInstance(aggregationOutputMime);
                convertedFile = jobDir.resolve("converted" + "." + converter.getExtension());

                logger.info("Converting aggregation output to Mime Type: " + aggregationOutputMime);

                converter.convert(outputFile, convertedFile);

                //  Create a file manager for uploading files to S3
                JobFileManager outputFileManager = new JobFileManager(storage, outputBucketName, jobFileS3KeyPrefix, batchJobId);

                //  Rename the converted file.
                convertedFile = Files.move(convertedFile, jobDir.resolve(requestedOutputFilename + "." + converter.getExtension()));

                if(resultMime.equals("application/zip")) {
                    //  Add the converted file to the zip file
                    zipContent.add(convertedFile.toFile());
                } else {
                    //  Add as an output to the WPS response
                    //  Upload to S3
                    String outputFilename = convertedFile.toFile().getName();
                    outputFileManager.upload(convertedFile.toFile(), outputFilename, aggregationOutputMime);

                    logger.info("Uploaded " + convertedFile.toFile().getAbsolutePath() + " to S3");

                    //  Put output URL in WPS response
                    resultUrl = WpsConfig.getExternalURL(outputBucketName, outputFileManager.getJobFileKey(outputFilename));
                    if (requestHelper.hasRequestedOutput("result")) {
                        outputMap.put("result", resultUrl);
                    }
                }

                //  URL for the status page for this job
                String statusUrl = WpsConfig.getStatusServiceHtmlEndpoint(batchJobId);


                //  Search for the metadata record for the layer by layer name
                String catalogueURL = WpsConfig.getProperty(GEONETWORK_CATALOGUE_URL_CONFIG_KEY);
                String layerSearchField = WpsConfig.getProperty(GEONETWORK_CATALOGUE_LAYER_FIELD_CONFIG_KEY);
                CatalogueReader catalogueReader = new CatalogueReader(catalogueURL, layerSearchField);

                //TODO: You need to report failure in the status report, otherwise the client keep waiting
                String metadataResponseXML = catalogueReader.getMetadataSummaryXML(layer);

                if (metadataResponseXML != null && metadataResponseXML.length() > 0) {

                    //  We only need the <metadata> tag and its contents
                    String metadataSummary = catalogueReader.getMetadataNodeContent(metadataResponseXML);

                    if (metadataSummary != null) {
                        pointOfTruth = catalogueReader.getMetadataPointOfTruthUrl(metadataSummary);
                        logger.info("Metadata Point Of Truth URL: " + pointOfTruth);

                        collectionTitle = catalogueReader.getCollectionTitle(metadataSummary);
                        logger.info("Metadata collection title: " + collectionTitle);

                        metadataUuid = catalogueReader.getUuid(metadataSummary);
                        logger.info("Metadata UUID: " + metadataUuid);

                        //  If the output requested is a ZIP - get the full metadata record for the collection to include in the
                        //  ZIP file.
                        if(resultMime.equals("application/zip")) {
                            if (metadataUuid != null) {

                                //  Get the full metadata record
                                //TODO: You need to report failure in the status report, otherwise the client keep waiting
                                String fullMetadataRecord = catalogueReader.getMetadataRecordXML(metadataUuid);

                                //  Write the metadata to a file

                                //  Form the metadata filename from the title of the collection
                                String metadataFilename = getMetadataFilename(collectionTitle) + ".xml";
                                File metadataFile = new File(jobDir.toFile(), metadataFilename);

                                try (FileWriter metadataFileWriter = new FileWriter(metadataFile)) {

                                    metadataFileWriter.write(fullMetadataRecord);
                                    metadataFileWriter.flush();

                                    logger.info("Metadata file size [" + metadataFile.length() + "] bytes");
                                    //  Add to the zip file
                                    zipContent.add(metadataFile);

                                    logger.info("Wrote metadata file to: " + metadataFile.getAbsolutePath() + ", Size: " + metadataFile.length());
                                } catch (IOException ioex) {
                                    logger.error("Unable to write metadata XML file: " + metadataFile.getAbsolutePath(), ioex);
                                }
                            }
                        }
                    } else {
                        logger.warn("Unable to retrieve metadata record for collection.  No metadata will be included in the zip file.");
                    }
                }


                //  Form the ZIP file and upload if requested
                if(resultMime.equals("application/zip")) {
                    String outputFilename = requestedOutputFilename + DEFAULT_OUTPUT_FILE_EXTENSION;

                    logger.info("Beginning zip and upload of output files to S3");
                    //  Form output ZIP file and upload it on the fly
                    S3Storage.uploadFilesToS3AsZip(zipContent, outputBucketName, outputFileManager.getJobFileKey(outputFilename));
                    logger.info("Uploaded " + batchJobId + ".zip to S3");

                    //  Put output URL in WPS response
                    resultUrl = WpsConfig.getExternalURL(outputBucketName, outputFileManager.getJobFileKey(outputFilename));
                    if (requestHelper.hasRequestedOutput("result")) {
                        outputMap.put("result", resultUrl);
                    }
                }



                //  If the requester has requested provenance output - form the record + save it to file
                //  We haven't included this output in the ZIP at this stage.  It is unlikely to be requested by a client.
                if (requestHelper.hasRequestedOutput("provenance")) {
                    logger.info("Provenance output requested.");

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
                    params.put("sourceMetadataUrl", pointOfTruth);
                    String provenanceDocument = ProvenanceWriter.write(PROVENANCE_TEMPLATE_GRIDDED, params);

                    File provenanceFile = new File(jobDir.toFile(), "provenance.xml");

                    FileWriter provenanceFileWriter = new FileWriter(provenanceFile);
                    provenanceFileWriter.write(provenanceDocument);
                    provenanceFileWriter.close();

                    //  Upload provenance document to S3
                    outputFileManager.upload(provenanceFile, "provenance.xml", PROVENANCE_FILE_MIME_TYPE);

                    String provenanceUrl = WpsConfig.getExternalURL(outputBucketName,
                            outputFileManager.getJobFileKey("provenance.xml"));

                    //  Add provenance output to WPS response
                    outputMap.put("provenance", provenanceUrl);
                }


                //  Calculate period elapsed during aggregation
                DateTime stopTime = new DateTime(DateTimeZone.UTC);
                Period elapsedPeriod = new Period(startTime, stopTime);
                String elapsedTimeString = formatPeriodString(elapsedPeriod);

                //  Log all job details including statistics on how long it took.
                logger.info("Aggregation completed successfully. JobID [" + batchJobId + "], Callback email [" + contactEmail + "], Size bytes [" + convertedFile.toFile().length() + "], Elapsed time (h:m:s) [" + elapsedTimeString + "]");
                statusDocument = statusBuilder.createResponseDocument(EnumStatus.SUCCEEDED, GOGODUCK_PROCESS_IDENTIFIER, null, null, outputMap);
                statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);


                //  Send email - if email address was provided
                if (contactEmail != null) {
                    try {
                        //  Send completed job email to user
                        emailService.sendCompletedJobEmail(
                                contactEmail,
                                batchJobId,
                                statusUrl,
                                storage.getExpirationinDays(outputBucketName),
                                subsetParams,
                                collectionTitle);
                    } catch (EmailException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }

            } finally {
                if (jobDir != null) {
                    FileUtils.deleteDirectory(jobDir.toFile());
                }
            }
        } catch(Throwable e) {
            if (e instanceof AmazonServiceException) {
                AmazonServiceException se = (AmazonServiceException) e;

                if (!se.getErrorType().equals(AmazonServiceException.ErrorType.Client)) {
                    // the exception was Amazon's fault, not ours, so the batch job should retry
                    String errorMessage = "An amazon service exception occurred processing job [" + batchJobId + "] : " +
                            "Message [" + se.getMessage() + "]" +
                            ", ErrorCode [" + se.getErrorCode() + "]" +
                            ", ErrorMessage [" + se.getErrorMessage() + "]" +
                            ", ErrorType [" + se.getErrorType().name() + "]";

                    logger.error(errorMessage, se);
                    exitCode = 1;
                }
            }

            logger.error("Failed aggregation. JobID [" + batchJobId + "], Callback email [" + contactEmail + "] : " + e.getMessage(), e);
            if (statusFileManager != null) {
                if (batchJobId != null) {
                    String statusDocument = null;
                    try {
                        statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, GOGODUCK_PROCESS_IDENTIFIER, "Exception occurred during aggregation : " + e.getMessage(), "AggregationError", null);
                        statusFileManager.write(statusDocument, statusFilename, STATUS_FILE_MIME_TYPE);
                    } catch (IOException ioe) {
                        logger.error("Unable to update status for job [" + batchJobId + "]. Status: " + statusDocument);
                        ioe.printStackTrace();
                    }
                }
            }

            //  Send failed job email to user
            if (contactEmail != null) {
                if (collectionTitle == "") {
                    // The metadata has not been retrieved yet
                    try {
                        collectionTitle = getCollectionTitle(statusFileManager);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        collectionTitle = "Could not retrieve collection metadata.";
                    }
                }
                try {
                    emailService.sendFailedJobEmail(contactEmail,
                            administratorEmail,
                            batchJobId,
                            subsetParams,
                            collectionTitle);
                } catch (EmailException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
        finally {
            //  Flush messages etc...
            LogManager.shutdown();

            //  Exit with a 'success' return code - will mean job will not retry
            // Autostart, auto exist. You do not want this behavior in test
            logger.info("Process completed successfully.. process will be shutdown in 3 secs");

        }
    }

    private String getCollectionTitle(JobFileManager jobFileManager) throws Exception {

        String collectionTitle = "";

        // Get the layer name from the request
        String requestXML = jobFileManager.read(requestFilename);
        XmlRequestParser parser = new XmlRequestParser();
        Execute request = (Execute) parser.parse(requestXML);
        ExecuteRequestHelper requestHelper = new ExecuteRequestHelper(request);
        String layer = requestHelper.getLiteralInputValue(LITERAL_INPUT_IDENTIFIER_LAYER);

        //  Search for the metadata record for the layer by layer name
        String catalogueURL = WpsConfig.getProperty(GEONETWORK_CATALOGUE_URL_CONFIG_KEY);
        String layerSearchField = WpsConfig.getProperty(GEONETWORK_CATALOGUE_LAYER_FIELD_CONFIG_KEY);
        CatalogueReader catalogueReader = new CatalogueReader(catalogueURL, layerSearchField);

        String metadataResponseXML = catalogueReader.getMetadataSummaryXML(layer);

        if (metadataResponseXML != null && metadataResponseXML.length() > 0) {

            //  We only need the <metadata> tag and its contents
            String metadataSummary = catalogueReader.getMetadataNodeContent(metadataResponseXML);

            if (metadataSummary != null) {
                collectionTitle = catalogueReader.getCollectionTitle(metadataSummary);
                logger.info("Metadata collection title: " + collectionTitle);
            }
        }

        return collectionTitle;
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


    private String getMetadataFilename(String collectionTitle) {
        if(collectionTitle == null || collectionTitle.trim().length() == 0) {
            //  Use a default filename
            return DEFAULT_METADATA_FILENAME;
        }

        //  Replace illegal characters with underscores
        collectionTitle = collectionTitle.replaceAll("[^\\w.-]", "_");

        return collectionTitle;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
