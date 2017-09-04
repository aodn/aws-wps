package au.org.emii.aggregationworker;

import au.org.aodn.aws.wps.status.*;
import au.org.emii.aggregator.NetcdfAggregator;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.AggregationOverridesReader;
import au.org.emii.download.Download;
import au.org.emii.download.DownloadConfig;
import au.org.emii.download.DownloadRequest;
import au.org.emii.download.Downloader;
import au.org.emii.download.ParallelDownloadManager;
import au.org.emii.geoserver.client.HttpIndexReader;
import au.org.emii.geoserver.client.SubsetParameters;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import thredds.crawlabledataset.s3.S3URI;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImmutable;
import ucar.unidata.geoloc.LatLonRect;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

@Component
public class AggregationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(au.org.emii.aggregationworker.AggregationRunner.class);

    //  TODO:  change parameters to reflect the current GoGoDuck interface - ie: pass a layer name, temporal extents & geographical extents.
    //         This prototype currently gets a list of files plus the extents.
    //         This component will need to contact geoserver + get the list of files for the named layer.
    @Override
    public void run(String... args) {

        S3StatusUpdater statusUpdater = null;
        String batchJobId = null;

        try {

            //  Capture the AWS job specifics - they are passed to the docker runtime as
            //  environment variables.
            batchJobId = System.getenv("AWS_BATCH_JOB_ID");
            String awsBatchComputeEnvName = System.getenv("AWS_BATCH_CE_NAME");
            String awsBatchQueueName = System.getenv("AWS_BATCH_JQ_NAME");
            String environmentName = System.getenv(WpsConfig.ENVIRONMENT_NAME_ENV_VARIABLE_NAME);

            Properties configuration = WpsConfig.getConfigProperties(environmentName);
            String statusS3Bucket = configuration.getProperty(WpsConfig.STATUS_S3_BUCKET_CONFIG_KEY);
            String statusFileName = configuration.getProperty(WpsConfig.STATUS_S3_KEY_CONFIG_KEY);

            //  Update status document to indicate job has started
            statusUpdater = new S3StatusUpdater(statusS3Bucket, statusFileName);
            statusUpdater.updateStatus(EnumOperation.EXECUTE, batchJobId, EnumStatus.STARTED, null, null);

            logger.info("AWS BATCH JOB ID     : " + batchJobId);
            logger.info("AWS BATCH CE NAME    : " + awsBatchComputeEnvName);
            logger.info("AWS BATCH QUEUE NAME : " + awsBatchQueueName);
            logger.info("ENVIRONMENT NAME     : " + environmentName);

            Options options = new Options();

            options.addOption("b", true, "restrict to bounding box specified by left lower/right upper coordinates e.g. -b 120,-32,130,-29");
            options.addOption("z", true, "restrict data to specified z index range e.g. -z 2,4");
            options.addOption("t", true, "restrict data to specified date/time range in ISO 8601 format e.g. -t 2017-01-12T21:58:02Z,2017-01-12T22:58:02Z");
            // reexamine how config would be passed to program - as an argument perhaps?
            options.addOption("c", true, "aggregation overrides to be applied - xml");

            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);

            //  Check parameters:
            //      Expecting the following params -
            //        - layerName
            //        - subset
            //        - outputFile

            String layerName = commandLine.getArgs()[0];
            String subset = commandLine.getArgs()[1];
            String destinationFile = commandLine.getArgs()[2];
            SubsetParameters subsetParams = new SubsetParameters(subset);

            logger.info("args[0]=" + layerName);
            logger.info("args[1]=" + subset);
            logger.info("args[2]=" + destinationFile);

            //  Query geoserver to get a list of files for the aggregation
            //  TODO: source geoserver location from config
            HttpIndexReader indexReader = new HttpIndexReader("http://geoserver-123.aodn.org.au/geoserver/imos/ows");


            Set<DownloadRequest> downloads = indexReader.getDownloadRequestList(layerName, "time", "file_url", subsetParams);
            S3URI s3URI = new S3URI(destinationFile);

            String overridesArg = commandLine.getOptionValue("c");
            String bboxArg = commandLine.getOptionValue("b");
            String zSubsetArg = commandLine.getOptionValue("z");
            String timeArg = commandLine.getOptionValue("t");


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

            //Range zSubset = null;

            //if (zSubsetArg != null) {
            //    String[] zSubsetIndexes = zSubsetArg.split(",");
            //    int startIndex = Integer.parseInt(zSubsetIndexes[0]);
            //    int endIndex = Integer.parseInt(zSubsetIndexes[1]);
            //    zSubset = new Range(startIndex, endIndex);
            //}

            CalendarDateRange timeRange = null;

            if (timeArg != null) {
                String[] timeRangeComponents = timeArg.split(",");
                CalendarDate startTime = CalendarDate.parseISOformat("Gregorian", timeRangeComponents[0]);
                CalendarDate endTime = CalendarDate.parseISOformat("Gregorian", timeRangeComponents[1]);
                timeRange = CalendarDateRange.of(startTime, endTime);
            }


            AggregationOverrides overrides;

            if (overridesArg != null) {
                overrides = AggregationOverridesReader.load(Paths.get(overridesArg));
            } else {
                overrides = new AggregationOverrides(); // Use default (i.e. no overrides)
            }

            DownloadConfig downloadConfig = new DownloadConfig.ConfigBuilder().build();

            Downloader downloader = new Downloader(60000, 60000);

            Path outputFile = Files.createTempFile("agg", ".nc");

            try (
                    ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
                    NetcdfAggregator netcdfAggregator = new NetcdfAggregator(outputFile, overrides, bbox, null, timeRange)
            ){
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

                statusUpdater.updateStatus(EnumOperation.EXECUTE, batchJobId, EnumStatus.SUCCEEDED, null, null);
            } finally {
                Files.deleteIfExists(outputFile);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if(statusUpdater != null) {
                if(batchJobId != null) {
                    String statusDocument = null;
                    try {
                        statusDocument = statusUpdater.updateStatus(EnumOperation.EXECUTE, batchJobId, EnumStatus.FAILED, null, null);
                    }
                    catch (UnsupportedEncodingException uex)
                    {
                        logger.error("Unable to update status. Status: " + statusDocument);
                        uex.printStackTrace();
                    }
                }
            }
            System.exit(1);
        }
    }
}
