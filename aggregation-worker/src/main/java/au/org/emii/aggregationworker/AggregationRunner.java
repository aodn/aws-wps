package au.org.emii.aggregationworker;

import au.org.aodn.aggregator.configuration.Attribute;
import au.org.aodn.aggregator.configuration.Template;
import au.org.aodn.aggregator.configuration.Templates;
import au.org.aodn.aggregator.configuration.Variable;
import au.org.aodn.aws.util.S3Utils;
import au.org.aodn.aws.wps.status.EnumStatus;
import au.org.aodn.aws.wps.status.ExecuteStatusBuilder;
import au.org.aodn.aws.wps.status.S3StatusUpdater;
import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.aggregator.NetcdfAggregator;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.xstream.AggregationOverridesReader;
import au.org.emii.download.Download;
import au.org.emii.download.DownloadConfig;
import au.org.emii.download.DownloadRequest;
import au.org.emii.download.Downloader;
import au.org.emii.download.ParallelDownloadManager;
import au.org.emii.geoserver.client.HttpIndexReader;
import au.org.emii.geoserver.client.SubsetParameters;
import au.org.emii.util.EmailService;
import au.org.emii.util.IntegerHelper;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.StringInputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import thredds.crawlabledataset.s3.S3URI;
import ucar.ma2.DataType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImmutable;
import ucar.unidata.geoloc.LatLonRect;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static au.org.aodn.aws.wps.status.WpsConfig.*;

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

        S3StatusUpdater statusUpdater = null;
        String batchJobId = null, email = null;
        EmailService emailService = null;

        String jobReportUrl = "jobReportUrl"; // Needed to be replaced
        String expirationPeriod = "expirationPeriod"; // Needed to be replaced

        try {

            emailService = new EmailService();

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
            String downloadConfigS3Key = WpsConfig.getConfig(DOWNLOAD_CONFIG_S3_KEY_CONFIG_KEY);

            //  Parse connect timeout
            String downloadConnectTimeoutString = WpsConfig.getConfig(DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY);
            int downloadConnectTimeout;
            if(downloadConnectTimeoutString != null && IntegerHelper.isInteger(downloadConnectTimeoutString))
            {
                downloadConnectTimeout = Integer.parseInt(downloadConnectTimeoutString);
            }
            else
            {
                downloadConnectTimeout = DEFAULT_CONNECT_TIMEOUT_MS;
            }

            // Parse read timeout
            String downloadReadTimeoutString = WpsConfig.getConfig(DOWNLOAD_READ_TIMEOUT_CONFIG_KEY);
            int downloadReadTimeout;
            if(downloadReadTimeoutString != null && IntegerHelper.isInteger(downloadReadTimeoutString))
            {
                downloadReadTimeout = Integer.parseInt(downloadConnectTimeoutString);
            }
            else
            {
                downloadReadTimeout = DEFAULT_READ_TIMEOUT_MS;
            }


            //  TODO:  null check and act on null configuration
            //  TODO : validate configuration

            String statusDocument = ExecuteStatusBuilder.getStatusDocument(statusS3Bucket, statusFilename, batchJobId, EnumStatus.STARTED, null, null, null);

            //  Update status document to indicate job has started
            statusUpdater = new S3StatusUpdater(statusS3Bucket, statusFilename);
            statusUpdater.updateStatus(statusDocument, batchJobId);

            logger.info("AWS BATCH JOB ID     : " + batchJobId);
            logger.info("AWS BATCH CE NAME    : " + awsBatchComputeEnvName);
            logger.info("AWS BATCH QUEUE NAME : " + awsBatchQueueName);
            logger.info("-----------------------------------------------------");

            Options options = new Options();

            options.addOption("b", true, "restrict to bounding box specified by left lower/right upper coordinates e.g. -b 120,-32,130,-29");
            options.addOption("z", true, "restrict data to specified z index range e.g. -z 2,4");
            options.addOption("t", true, "restrict data to specified date/time range in ISO 8601 format e.g. -t 2017-01-12T21:58:02Z,2017-01-12T22:58:02Z");
            // reexamine how config would be passed to program - as an argument perhaps?
            options.addOption("c", true, "aggregation overrides to be applied - xml");

            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);

            //  TODO:  Parameters are currently passed as command line arguments & are positional.
            //  TODO:  need to revisit to accept named parameters passed in any order.
            //  TODO:  Some scheme like passive them as a <name>=<value> pair for each one might be workable
            //  TODO:  (as long as the value doesn't include spaces)
            //
            //  Currently expects the following params (IN THIS ORDER) -
            //        - layer
            //        - subset
            //        - result
            String layer = commandLine.getArgs()[0];
            String subset = commandLine.getArgs()[1];
            String resultMime = commandLine.getArgs()[2];
            email = commandLine.getArgs()[3];

            SubsetParameters subsetParams = new SubsetParameters(subset);

            logger.info("Layer name        = " + layer);
            logger.info("Subset parameters = " + subset);
            logger.info("Request MIME type = " + resultMime);


            //  Query geoserver to get a list of files for the aggregation
            //  TODO: source geoserver location from config
            HttpIndexReader indexReader = new HttpIndexReader("http://geoserver-123.aodn.org.au/geoserver/imos/ows");


            Set<DownloadRequest> downloads = indexReader.getDownloadRequestList(layer, "time", "file_url", subsetParams);

            //  Form output file location
            S3URI s3URI = new S3URI(outputBucketName, batchJobId + "/" + outputFilename);

            //  TODO : remove if not required
            String overridesArg = commandLine.getOptionValue("c");


            LatLonRect bbox = null;
            SubsetParameters.SubsetParameter latSubset = subsetParams.get("LATITUDE");
            SubsetParameters.SubsetParameter lonSubset = subsetParams.get("LONGITUDE");

            if (latSubset != null && lonSubset != null) {

                double minLon = Double.parseDouble(lonSubset.start);
                double minLat = Double.parseDouble(latSubset.start);
                double maxLon = Double.parseDouble(lonSubset.end);
                double maxLat = Double.parseDouble(latSubset.end);
                LatLonPoint lowerLeft = new LatLonPointImmutable(minLat, minLon);
                LatLonPoint upperRight = new LatLonPointImmutable(maxLat, maxLon);
                bbox = new LatLonRect(lowerLeft, upperRight);

                logger.info("Bounding box: LAT [" + minLat + ", " + maxLat + "], LON [" + minLon + ", " + maxLon + "]");
            }


            CalendarDateRange timeRange = null;

            //  Apply time range (if provided)
            SubsetParameters.SubsetParameter timeSubset = subsetParams.get("TIME");
            if (timeSubset != null) {
                CalendarDate startTime = CalendarDate.parseISOformat("Gregorian", timeSubset.start);
                CalendarDate endTime = CalendarDate.parseISOformat("Gregorian", timeSubset.end);
                timeRange = CalendarDateRange.of(startTime, endTime);
                logger.info("Time range specified for aggregation: START [" + timeSubset.start + "], END [" + timeSubset.end + "]");
            }


            //  TODO: source s3Region name - if required?
            //  Apply overrides (if provided)
            AggregationOverrides overrides = getAggregationOverrides(aggregatorConfigS3Bucket, aggregatorTemplateFileS3Key, null, layer);

            //  Apply download configuration
            DownloadConfig downloadConfig = getDownloadConfig(aggregatorConfigS3Bucket, downloadConfigS3Key);

            //  Apply connect/read timeouts
            Downloader downloader = new Downloader(downloadConnectTimeout, downloadReadTimeout);

            Path outputFile = Files.createTempFile("agg", ".nc");

            try (
                    ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
                    NetcdfAggregator netcdfAggregator = new NetcdfAggregator(outputFile, overrides, bbox, null, timeRange)
            ) {
                for (Download download : downloadManager.download(downloads)) {
                    netcdfAggregator.add(download.getPath());
                    downloadManager.remove();
                }

                logger.info("Copying output file to : " + s3URI.toString());

                DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
                TransferManager tx = new TransferManager(credentialProviderChain.getCredentials());
                Upload myUpload = tx.upload(s3URI.getBucket(), s3URI.getKey(), outputFile.toFile());
                myUpload.waitForCompletion();
                tx.shutdownNow();

                HashMap<String, String> outputMap = new HashMap<>();
                outputMap.put("result", WpsConfig.getS3ExternalURL(s3URI.getBucket(), s3URI.getKey()));

                statusDocument = ExecuteStatusBuilder.getStatusDocument(statusS3Bucket, statusFilename, batchJobId, EnumStatus.SUCCEEDED, null, null, outputMap);
                statusUpdater.updateStatus(statusDocument, batchJobId);
            } finally {
                Files.deleteIfExists(outputFile);
            }

            emailService.sendCompletedJobEmail(email, batchJobId, jobReportUrl, expirationPeriod);
        } catch (Throwable e) {
            e.printStackTrace();
            if (statusUpdater != null) {
                if (batchJobId != null) {
                    String statusDocument = null;
                    try {
                        statusDocument = ExecuteStatusBuilder.getStatusDocument(statusS3Bucket, statusFilename, batchJobId, EnumStatus.FAILED, "Exception occurred during aggregation :" + e.getMessage(), "AggregationError", null);
                        statusUpdater.updateStatus(statusDocument, batchJobId);
                    } catch (UnsupportedEncodingException uex) {
                        logger.error("Unable to update status. Status: " + statusDocument);
                        uex.printStackTrace();
                    }
                }
            }
            try {
                emailService.sendFailedJobEmail(email, batchJobId, jobReportUrl);
            } catch (Exception ex) {
                logger.error("Unable to send failed job email. Error Message:", ex);
            }
            System.exit(1);
        }
    }

    private DownloadConfig getDownloadConfig(String s3Bucket, String s3Key)
    {
        DownloadConfig.ConfigBuilder builder = new DownloadConfig.ConfigBuilder();

        //  Read config file from S3

        //  Populate builder with values from config file

        DownloadConfig config = new DownloadConfig(builder);
        return config;
    }



    private AggregationOverrides getAggregationOverrides(String s3Bucket, String s3Key, String s3Region, String layer)
    {
        AggregationOverrides overrides = new AggregationOverrides();

        logger.info("Loading aggregation templates file from S3.  Bucket [" + s3Bucket + "], Key [" + s3Key + "], Region [" + s3Region + "]");

        //  Read config file from S3
        String overrideTemplateDocument = null;
        try
        {
            overrideTemplateDocument = S3Utils.readS3ObjectAsString(s3Bucket, s3Key, s3Region);

            //  Unmarshall from XML
            JAXBContext context = JAXBContext.newInstance(Templates.class);
            Unmarshaller u = context.createUnmarshaller();

            //  Try and match the template to the layer - apply the specified template if a match found
            Templates templates = (Templates) u.unmarshal(new StringInputStream(overrideTemplateDocument));
            List<Template> templateList = templates.getTemplate();

            //  Config file has 'template' elements which contain 'attributes' and 'variables'
            for(Template currentTemplate : templateList)
            {
                //  The 'match' attribute on the template can contain a comma-separated list
                //  of match expressions for the template.
                StringTokenizer tokenizer = new StringTokenizer(currentTemplate.getMatch(), ",");
                boolean match = false;
                while(tokenizer.hasMoreTokens()) {
                    //  Apply the first template that matches the layer name
                    if (Pattern.matches(tokenizer.nextToken(), layer)) {
                        match = true;
                        break;
                    }
                }

                logger.info("Template match [" + match + "]: Layer [" + layer + "], Template Name [" + currentTemplate.getName() + "], Match [" + currentTemplate.getMatch() + "]");

                //  Next template
                if(!match)
                {
                    continue;
                }

                //  Convert the 'attribute' elements into GlobalAttributeOverride objects
                Template.Attributes srcAttributes = currentTemplate.getAttributes();
                if(srcAttributes != null) {
                    List<Attribute> srcAttributeList = srcAttributes.getAttribute();
                    for (Attribute currentSrcAttr : srcAttributeList) {

                        String attributeValue = null;
                        if (currentSrcAttr.getValue() != null)
                        {
                            if(currentSrcAttr.getValue().isEmpty() == false) {
                                //  Use the first value
                                attributeValue = currentSrcAttr.getValue().get(0);
                            }
                            else
                            {
                                logger.info("Attribute values empty.");
                            }
                        }
                        else
                        {
                            logger.info("Attribute value null.");
                        }

                        logger.info("Applying Attribute Override: Name [" + currentSrcAttr.getName() + "], Type [" + currentSrcAttr.getType() + "], Value [" + attributeValue + "]");

                        GlobalAttributeOverride newOverride = new GlobalAttributeOverride(currentSrcAttr.getName(), DataType.getType(currentSrcAttr.getType()), currentSrcAttr.getMatch(), attributeValue);
                        overrides.getAttributes().add(newOverride);
                    }
                }


                //  Convert the 'variable' elements into VariableOverrides objects
                Template.Variables srcVariables = currentTemplate.getVariables();

                if(srcVariables != null)
                {
                    List<Variable> srcVariableList = srcVariables.getVariable();
                    for (Variable currentSrcVariable : srcVariableList) {
                        //  Variables can have attributes
                        List<VariableAttributeOverride> variableAttributeOverrides = null;
                        List<Attribute> variableAttributes = currentSrcVariable.getAttribute();
                        if (variableAttributes != null && variableAttributes.size() > 0) {
                            variableAttributeOverrides = new ArrayList<>();
                            //  Attributes can have multiple values
                            for (Attribute currentVariableAttr : variableAttributes) {
                                VariableAttributeOverride newVariableAttributeOverride = new VariableAttributeOverride(currentVariableAttr.getName(), DataType.getType(currentVariableAttr.getType()), currentVariableAttr.getValue());
                                variableAttributeOverrides.add(newVariableAttributeOverride);
                            }
                        }

                        logger.info("Applying Variable Override: Name [" + currentSrcVariable.getName() + "], Type [" + currentSrcVariable.getType() + "]");
                        for(VariableAttributeOverride var : variableAttributeOverrides)
                        {
                            logger.info("    - Attribute override: Name [" + var.getName() + "], Type [" + var.getType() + "], Values [" + var.getValues() + "]");
                        }

                        VariableOverrides newOverride = new VariableOverrides(currentSrcVariable.getName(), DataType.getType(currentSrcVariable.getType()), variableAttributeOverrides);
                        overrides.getVariableOverridesList().add(newOverride);
                    }
                }
            }
        }
        catch(IOException ex)
        {
            logger.error("Unable to load aggregations override file from S3.", ex);
        }
        catch(JAXBException ex)
        {
            logger.error("Unable to unmarshall XML from aggregations override file.", ex);
        }

        return overrides;
    }

}
