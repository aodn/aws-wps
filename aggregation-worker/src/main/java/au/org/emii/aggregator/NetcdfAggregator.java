package au.org.emii.aggregator;

import au.org.emii.aggregator.dataset.NetcdfDatasetAdapter;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.exception.SubsetException;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.AggregationOverridesReader;
import au.org.emii.aggregator.template.TemplateDataset;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.variable.UnpackerOverrides;
import au.org.emii.download.Download;
import au.org.emii.download.DownloadConfig;
import au.org.emii.download.DownloadRequest;
import au.org.emii.download.Downloader;
import au.org.emii.download.ParallelDownloadManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImmutable;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NetCDF Aggregator
 */

public class NetcdfAggregator implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(au.org.emii.aggregator.NetcdfAggregator.class);
    private static final Group GLOBAL = null;

    private final Path outputPath;
    private final AggregationOverrides aggregationOverrides;
    private final LatLonRect bbox;
    private final Range verticalSubset;
    private final CalendarDateRange dateRange;

    private NetcdfFileWriter writer;

    private NetcdfDatasetIF templateDataset;
    private boolean fileProcessed;
    private Map<String, UnpackerOverrides> unpackerOverrides;
    private long slicesProcessed = 0L;

    public NetcdfAggregator(Path outputPath, AggregationOverrides aggregationOverrides,
                            LatLonRect bbox, Range verticalSubset, CalendarDateRange dateRange
    ) {
        assertOutputPathValid(outputPath);

        this.outputPath = outputPath;
        this.aggregationOverrides = aggregationOverrides;
        this.bbox = bbox;
        this.verticalSubset = verticalSubset;
        this.dateRange = dateRange;

        fileProcessed = false;

        // use overrides specified in config if any when unpacking the first dataset
        unpackerOverrides = aggregationOverrides.getUnpackerOverrides();
    }

    public void add(Path datasetLocation) throws SubsetException, AggregationException, IOException {
        try (NetcdfDatasetAdapter dataset = NetcdfDatasetAdapter.open(datasetLocation, unpackerOverrides)) {
            NetcdfDatasetIF subsettedDataset = dataset.subset(dateRange, verticalSubset, bbox);

            if (!fileProcessed) {
                unpackerOverrides = getOverridesApplied(dataset); // ensure same changes applied to all other datasets
                logger.info("Creating output file " + outputPath + " using " + datasetLocation + " as a template");
                templateDataset = new TemplateDataset(subsettedDataset, aggregationOverrides,
                    dateRange, verticalSubset, bbox);
                copyToOutputFile(templateDataset);
                fileProcessed = true;
            }

            logger.info("Adding " + datasetLocation + " to output file");

            appendRecordVariables(subsettedDataset);

            for (NetcdfVariable variable: templateDataset.getVariables()) {
                if (variable.isUnlimited()) {
                    NetcdfVariable subsettedVariable = subsettedDataset.findVariable(variable.getShortName());
                    slicesProcessed += subsettedVariable.getShape()[0];
                    break;
                }
            }

            System.out.println(String.format("%s estimated size, %s actual size",
                Long.toString(estimateSize()), Long.toString(Files.size(outputPath))));
        }
    }

    private long estimateSize() {
        long estimatedTotalSize = 0L;

        for (NetcdfVariable variable: templateDataset.getVariables()) {
            if (!variable.isUnlimited()) {
                continue;
            }

            int[] shape = variable.getShape();
            long numberOfTimeSliceElements = 1;

            for (int i=1; i<shape.length; i++) {
                numberOfTimeSliceElements *= shape[i];
            }

            long estimatedVariableSize = slicesProcessed * numberOfTimeSliceElements * variable.getElementSize();
          //  estimatedTotalSize += ((estimatedVariableSize + 2048L) / 2048L) * 2048L;  //default chunking is 2048 byte chunks
            estimatedTotalSize += estimatedVariableSize;  //default chunking is 2048 byte chunks
        }

        return estimatedTotalSize;
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    private void assertOutputPathValid(Path outputPath) {
        try {
            if (Files.exists(outputPath) && Files.size(outputPath) > 0L) {
                throw new IllegalArgumentException(String.format("Output file %s exists and is not empty", outputPath.toString()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyToOutputFile(NetcdfDatasetIF template) throws IOException {
        try {
            writer = NetcdfFileWriter.createNew(Version.netcdf4,
                outputPath.toString(), null);

            // Copy global attributes to output file

            for (Attribute attribute : template.getGlobalAttributes()) {
                writer.addGroupAttribute(GLOBAL, attribute);
            }

            // Copy dimensions to output file

            List<Dimension> fileDimensions = new ArrayList<>();

            for (Dimension dimension: template.getDimensions()) {
                Dimension fileDimension = writer.addDimension(GLOBAL, dimension.getShortName(), dimension.getLength(), true, dimension.isUnlimited(), dimension.isVariableLength());
                fileDimensions.add(fileDimension);
            }

            // Copy variables to output file

            for (NetcdfVariable variable: template.getVariables()) {
                List<Dimension> variableDimensions = new ArrayList<>();

                for (Dimension dimension: variable.getDimensions()) {
                    for (Dimension fileDimension: fileDimensions) {
                        if (fileDimension.getFullName().equals(dimension.getFullName())) {
                            variableDimensions.add(fileDimension);
                        }
                    }
                }

                Variable fileVariable = writer.addVariable(GLOBAL, variable.getShortName(), variable.getDataType(), variableDimensions);

                for (Attribute attribute: variable.getAttributes()) {
                    writer.addVariableAttribute(fileVariable, attribute);
                }
            }

            // Finished defining file contents

            writer.create();

            // Copy static data (coordinate axes, etc) to output file

            for (NetcdfVariable variable: template.getVariables()) {
                if (!variable.isUnlimited()) {
                    Variable fileVariable = writer.findVariable(variable.getShortName());
                    writer.write(fileVariable, variable.read());
                }
            }
        } catch (InvalidRangeException e) {
            throw new RuntimeException("Unexpected error ", e);
        }
    }

    private void appendRecordVariables(NetcdfDatasetIF dataset) throws AggregationException, IOException {
        for (NetcdfVariable templateVariable: templateDataset.getVariables()) {
            if (!templateVariable.isUnlimited()) {
                continue;
            }

            NetcdfVariable datasetVariable = dataset.findVariable(templateVariable.getShortName());
            append(datasetVariable);
        }

        writer.flush();
    }

    // Add time slices (records) from source variable to output variable
    private void append(NetcdfVariable srcVariable) throws AggregationException, IOException {
        try {
            Variable destVariable = writer.findVariable(srcVariable.getShortName());

            // determine number of time slices in the source variable
            int timeSlicesCount = srcVariable.getShape()[0]; // assumes time dimension is slowest varying i.e. the first

            // copy one time slice at a time for the moment
            int[] timeSlice = srcVariable.getShape();
            timeSlice[0] = 1;

            // start copying from the start of the source variable
            int[] srcOrigin = new int[srcVariable.getRank()];

            // start adding at the end of the destination variable
            int[] destOrigin = new int[destVariable.getRank()];
            destOrigin[0] = destVariable.getShape()[0];

            // add each time slice from the source variable to the output variable
            for (int i=0; i<timeSlicesCount; i++) {
                // get timeslice from source variable
                srcOrigin[0] = i;
                Array data = srcVariable.read(srcOrigin, timeSlice);
                // add it to the destination variable
                writer.write(destVariable, destOrigin, data);
                // want to add after this one next time
                destOrigin[0]++;
            }
        } catch (InvalidRangeException e) {
            throw new AggregationException(e);
        }
    }

    private Map<String, UnpackerOverrides> getOverridesApplied(NetcdfDatasetIF dataset) {
        Map<String, UnpackerOverrides> result = new LinkedHashMap<>();

        for (NetcdfVariable variable: dataset.getVariables()) {
            result.put(variable.getShortName(), getUnpackerOverrides(variable));
        }

        return result;
    }

    private UnpackerOverrides getUnpackerOverrides(NetcdfVariable variable) {
        UnpackerOverrides.Builder builder = new UnpackerOverrides.Builder()
            .newDataType(variable.getDataType());

        // ensure same filler values applied

        Attribute fillerAttribute = variable.findAttribute(CDM.FILL_VALUE);

        if (fillerAttribute != null) {
            builder.newFillerValue(fillerAttribute.getNumericValue());
        }

        // ensure same missing values applied

        Attribute missingValuesAttribute = variable.findAttribute(CDM.MISSING_VALUE);

        if (missingValuesAttribute != null) {
            Number[] missingValues = new Number[missingValuesAttribute.getLength()];
            for (int i=0; i<missingValues.length; i++) {
                missingValues[i] = missingValuesAttribute.getNumericValue(i);
            }
            builder.newMissingValues(missingValues);
        }

        return builder.build();
    }

    // Usage: java -jar {classpath} au.org.emii.aggregator.NetcdfAggregator [-b bbox] [-z zsubset] [-t timeRange] [-c overridesConfigFile] fileList destination
    // fileList should contain a list of files to be included in the aggregation

    public static void main(String[] args) throws ParseException, AggregationException, IOException, InvalidRangeException, SubsetException {
        Options options = new Options();

        options.addOption("b", true, "restrict to bounding box specified by left lower/right upper coordinates e.g. -b 120,-32,130,-29");
        options.addOption("z", true, "restrict data to specified z index range e.g. -z 2,4");
        options.addOption("t", true, "restrict data to specified date/time range in ISO 8601 format e.g. -t 2017-01-12T21:58:02Z,2017-01-12T22:58:02Z");
        options.addOption("c", true, "xml file containing aggregation overrides to be applied");

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse( options, args );

        List<String> inputFiles = Files.readAllLines(Paths.get(line.getArgs()[0]), Charset.forName("utf-8"));
        Path outputFile = Paths.get(line.getArgs()[1]);

        String bboxArg = line.getOptionValue("b");
        String zSubsetArg = line.getOptionValue("z");
        String timeArg = line.getOptionValue("t");
        String overridesArg = line.getOptionValue("c");

        LatLonRect bbox = null;

        if (bboxArg != null) {
            String[] bboxCoords = bboxArg.split(",");
            double minLon = Double.parseDouble(bboxCoords[0]);
            double minLat = Double.parseDouble(bboxCoords[1]);
            double maxLon = Double.parseDouble(bboxCoords[2]);
            double maxLat = Double.parseDouble(bboxCoords[3]);
            LatLonPoint lowerLeft = new LatLonPointImmutable(minLat, minLon);
            LatLonPoint upperRight = new LatLonPointImmutable(maxLat, maxLon);
            bbox = new LatLonRect(lowerLeft, upperRight);
        }

        Range zSubset = null;

        if (zSubsetArg != null) {
            String[] zSubsetIndexes = zSubsetArg.split(",");
            int startIndex = Integer.parseInt(zSubsetIndexes[0]);
            int endIndex = Integer.parseInt(zSubsetIndexes[1]);
            zSubset = new Range(startIndex, endIndex);
        }

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

        Set<DownloadRequest> downloads = new LinkedHashSet<>();

        for (String inputFile: inputFiles) {
            if (inputFile.trim().isEmpty()) continue;
            URL url = new URL(inputFile.split(",")[0]);
            long size = Long.parseLong(inputFile.split(",")[1]);
            downloads.add(new DownloadRequest(url, size));
        }

        try (
            ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
            au.org.emii.aggregator.NetcdfAggregator netcdfAggregator = new au.org.emii.aggregator.NetcdfAggregator(
                outputFile, overrides, bbox, zSubset, timeRange)
        ){
            for (Download download : downloadManager.download(downloads)) {
                netcdfAggregator.add(download.getPath());
                downloadManager.remove();
            }
        }
    }

}
