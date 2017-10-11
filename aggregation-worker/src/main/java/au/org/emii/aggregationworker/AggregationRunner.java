package au.org.emii.aggregationworker;


import au.org.aodn.aws.exception.EmailException;
import au.org.aodn.aws.util.EmailService;
import au.org.aodn.aws.util.S3Utils;
import au.org.aodn.aws.wps.status.EnumStatus;
import au.org.aodn.aws.wps.status.ExecuteStatusBuilder;
import au.org.aodn.aws.wps.status.S3StatusUpdater;
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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import thredds.crawlabledataset.s3.S3URI;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static au.org.aodn.aws.wps.status.WpsConfig.*;
import static au.org.emii.aggregator.au.org.emii.aggregator.config.AggregationOverridesReader.getAggregationOverrides;
import static au.org.emii.aggregator.au.org.emii.aggregator.config.DownloadConfigReader.getDownloadConfig;

@Component
public class AggregationRunner implements CommandLineRunner {

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 60000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 60000;

    private static final Logger logger = LoggerFactory.getLogger(au.org.emii.aggregationworker.AggregationRunner.class);

    private String statusS3Bucket = null;
    private String statusFilename = null;

    /**
     * Entry point for the aggregation.  Relies on command-line parameters.
     *
     * @param args
     */
    @Override
    public void run(String... args) {

        DateTime startTime = new DateTime(DateTimeZone.UTC);
        S3StatusUpdater statusUpdater = null;
        String batchJobId = null, email = null;
        EmailService emailService = null;
        ExecuteStatusBuilder statusBuilder = null;

        try {
            //  Capture the AWS job specifics - they are passed to the docker runtime as
            //  environment variables.
            batchJobId = WpsConfig.getConfig(AWS_BATCH_JOB_ID_CONFIG_KEY);
            String awsBatchComputeEnvName = WpsConfig.getConfig(AWS_BATCH_CE_NAME_CONFIG_KEY);
            String awsBatchQueueName = WpsConfig.getConfig(AWS_BATCH_JQ_NAME_CONFIG_KEY);
            String outputBucketName = WpsConfig.getConfig(OUTPUT_S3_BUCKET_CONFIG_KEY);
            String outputFilename = WpsConfig.getConfig(OUTPUT_S3_FILENAME_CONFIG_KEY);
            statusS3Bucket = WpsConfig.getConfig(STATUS_S3_BUCKET_CONFIG_KEY);
            statusFilename = WpsConfig.getConfig(STATUS_S3_FILENAME_CONFIG_KEY);

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
            String statusDocument = statusBuilder.createResponseDocument(EnumStatus.STARTED, null, null, null);

            //  Update status document to indicate job has started
            statusUpdater = new S3StatusUpdater(statusS3Bucket, statusFilename);
            statusUpdater.updateStatus(statusDocument, batchJobId);

            logger.info("AWS BATCH JOB ID     : " + batchJobId);
            logger.info("AWS BATCH CE NAME    : " + awsBatchComputeEnvName);
            logger.info("AWS BATCH QUEUE NAME : " + awsBatchQueueName);
            logger.info("-----------------------------------------------------");

            Options options = new Options();

            options.addOption("l", true, "The layer name");
            options.addOption("s", true, "Subset parameters");
            options.addOption("m", true, "The requested output mime type");
            options.addOption("e", true, "Callback email address");

            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);

            String layer = commandLine.getOptionValue('l');
            String subset = commandLine.getOptionValue('s');
            String resultMime = commandLine.getOptionValue('m');
            email = commandLine.getOptionValue('e');

            SubsetParameters subsetParams = SubsetParameters.parse(subset);

            if (email != null) {
                email = email.substring(email.indexOf("=") + 1);
                emailService = new EmailService();
            }

            logger.info("Command line parameters passed:");
            logger.info("Layer name             = " + layer);
            logger.info("Subset parameters      = " + subset);
            logger.info("Request MIME type      = " + resultMime);
            logger.info("Callback email address = " + email);


            //  TODO: Qa the parameters/settings passed?

            //  Instantiate the correct converter for the requested mimeType & do the conversion
            Converter converter = Converter.newInstance(resultMime);

            //  Query geoserver to get a list of files for the aggregation
            HttpIndexReader indexReader = new HttpIndexReader(WpsConfig.getConfig(WpsConfig.GEOSERVER_CATALOGUE_ENDPOINT_URL_CONFIG_KEY));
            Set<DownloadRequest> downloads = indexReader.getDownloadRequestList(layer, "time", "file_url", subsetParams);

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
            AggregationOverrides overrides = getAggregationOverrides(aggregatorConfigS3Bucket, aggregatorTemplateFileS3Key, null, layer);

            //  Apply download configuration
            DownloadConfig downloadConfig = getDownloadConfig();

            //  Apply connect/read timeouts
            Downloader downloader = new Downloader(downloadConnectTimeout, downloadReadTimeout);

            Path outputFile = Files.createTempFile("agg", ".nc");
            Path convertedFile = null;

            long chunkSize = Long.valueOf(WpsConfig.getConfig(CHUNK_SIZE_KEY));

            try (
                    ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
                    NetcdfAggregator netcdfAggregator = new NetcdfAggregator(outputFile, overrides, chunkSize, bbox, subsetParams.getVerticalRange(), subsetTimeRange)
            ) {
                for (Download download : downloadManager.download(downloads)) {
                    netcdfAggregator.add(download.getPath());
                    downloadManager.remove();
                }


                //  Convert to required output
                Path workingDir = downloadConfig.getDownloadDirectory();

                //  Perform the conversion
                convertedFile = workingDir.resolve("converted" + converter.getExtension());
                converter.convert(outputFile, convertedFile);

                //  Form output file location
                String jobPrefix = WpsConfig.getConfig(AWS_BATCH_JOB_S3_KEY);
                S3URI resultS3URI = new S3URI(outputBucketName, jobPrefix + batchJobId + "/" + outputFilename + "." + converter.getExtension());

                logger.info("Copying output file to : " + resultS3URI.toString());

                //  Upload to S3
                S3Utils.uploadToS3(resultS3URI, convertedFile.toFile());

                //  Lookup the metadata URL for the layer
                String catalogueURL = WpsConfig.getConfig(GEONETWORK_CATALOGUE_URL_CONFIG_KEY);
                String layerSearchField = WpsConfig.getConfig(GEONETWORK_CATALOGUE_LAYER_FIELD_CONFIG_KEY);
                CatalogueReader catalogueReader = new CatalogueReader(catalogueURL, layerSearchField);

                // Create provenance document
                Configuration config = new Configuration();
                config.setClassForTemplateLoading(ProvenanceWriter.class, "");

                Map<String, Object> params = new HashMap<>();
                params.put("jobId", batchJobId);
                params.put("downloadUrl", WpsConfig.getS3ExternalURL(resultS3URI.getBucket(), resultS3URI.getKey()));
                params.put("settingsPath", WpsConfig.getS3ExternalURL(aggregatorConfigS3Bucket, aggregatorTemplateFileS3Key));
                params.put("startTime", startTime);
                params.put("endTime", new DateTime(DateTimeZone.UTC));
                params.put("layer", layer);
                params.put("parameters", subsetParams);
                params.put("sourceMetadataUrl", catalogueReader.getMetadataUrl(layer));
                String provenanceDocument = ProvenanceWriter.write(aggregatorConfigS3Bucket, aggregatorProvenanceTemplateS3Key, params);

                //  Upload provenance document to S3
                //  TODO: configurable provenance filename?
                S3URI provenanceS3URI = new S3URI(outputBucketName, jobPrefix + batchJobId + "/provenance.xml");
                S3Utils.uploadToS3(provenanceS3URI, provenanceDocument);

                HashMap<String, String> outputMap = new HashMap<>();
                outputMap.put("result", WpsConfig.getS3ExternalURL(resultS3URI.getBucket(), resultS3URI.getKey()));
                outputMap.put("provenance", WpsConfig.getS3ExternalURL(provenanceS3URI.getBucket(), provenanceS3URI.getKey()));

                statusDocument = statusBuilder.createResponseDocument(EnumStatus.SUCCEEDED, null, null, outputMap);
                statusUpdater.updateStatus(statusDocument, batchJobId);
            } finally {
                if (convertedFile != null) {
                    Files.deleteIfExists(convertedFile);
                }
                if (outputFile != null) {
                    Files.deleteIfExists(outputFile);
                }
            }

            String jobPrefix = WpsConfig.getConfig(AWS_BATCH_JOB_S3_KEY);
            String outputFileLocation = WpsConfig.getS3ExternalURL(outputBucketName, jobPrefix + batchJobId + "/" + outputFilename + "." + converter.getExtension());
            emailService.sendCompletedJobEmail(email, batchJobId, outputFileLocation, WpsConfig.getJobExpiration());

        } catch (Throwable e) {
            e.printStackTrace();
            if (statusUpdater != null) {
                if (batchJobId != null) {
                    String statusDocument = null;
                    try {
                        statusDocument = statusBuilder.createResponseDocument(EnumStatus.FAILED, "Exception occurred during aggregation :" + e.getMessage(), "AggregationError", null);
                        statusUpdater.updateStatus(statusDocument, batchJobId);
                    } catch (UnsupportedEncodingException uex) {
                        logger.error("Unable to update status. Status: " + statusDocument);
                        uex.printStackTrace();
                    }
                }
            }

            if (emailService != null) {
                try {
                    emailService.sendFailedJobEmail(email, batchJobId, "Job Report URL (Service not yet implemented)");
                } catch (EmailException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            System.exit(1);
        }
    }
}
