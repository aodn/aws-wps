package au.org.emii.aggregator;

import au.org.emii.aggregator.dataset.NetcdfDatasetAdapter;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.index.IndexChunk;
import au.org.emii.aggregator.index.IndexChunkIterator;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.VariableOverrides;
import au.org.emii.aggregator.overrides.xstream.AggregationOverridesReader;
import au.org.emii.aggregator.template.TemplateDataset;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.variable.UnpackerOverrides;
import au.org.emii.aggregator.variable.UnpackerOverrides.Builder;
import au.org.emii.util.NumberRange;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImmutable;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static au.org.emii.aggregator.variable.NetcdfVariable.DEFAULT_MAX_CHUNK_SIZE;

/**
 * NetCDF Aggregator
 */

public class NetcdfAggregator implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(NetcdfAggregator.class);
    private static final Group GLOBAL = null;

    private final Path outputPath;
    private final AggregationOverrides aggregationOverrides;
    private final LatLonRect bbox;
    private final NumberRange verticalSubset;
    private final CalendarDateRange dateRange;
    private final long maxChunkSize;

    private NetcdfFileWriter writer;

    private NetcdfDatasetIF templateDataset;
    private boolean outputFileCreated;
    private Map<String, UnpackerOverrides> unpackerOverrides;
    private int slicesWritten = 0;

    public NetcdfAggregator(Path outputPath, AggregationOverrides aggregationOverrides, Long maxChunkSize,
                            LatLonRect bbox, NumberRange verticalSubset, CalendarDateRange dateRange
    ) {
        assertOutputPathValid(outputPath);

        this.outputPath = outputPath;
        this.aggregationOverrides = aggregationOverrides;
        this.bbox = bbox;
        this.verticalSubset = verticalSubset;
        this.dateRange = dateRange;
        this.maxChunkSize = maxChunkSize != null ? maxChunkSize : DEFAULT_MAX_CHUNK_SIZE;

        outputFileCreated = false;

        // use overrides specified in config if any when unpacking the first dataset
        unpackerOverrides = getUnpackerOverrides(aggregationOverrides.getVariableOverridesList());
    }

    public void add(Path datasetLocation) throws AggregationException {
        add(datasetLocation.toString());
    }

    public void add(String datasetLocation) throws AggregationException {
        try (NetcdfDatasetAdapter dataset = NetcdfDatasetAdapter.open(datasetLocation, unpackerOverrides, maxChunkSize)) {
            NetcdfDatasetIF subsettedDataset = dataset.subset(dateRange, verticalSubset, bbox);

            if (!outputFileCreated) {
                unpackerOverrides = getOverridesApplied(dataset); // ensure same changes applied to all other datasets
                logger.info("Creating output file {} using {} as a template", outputPath, datasetLocation);
                templateDataset = new TemplateDataset(subsettedDataset, aggregationOverrides,
                    dateRange, verticalSubset, bbox);
                // create empty dataset from template
                createOutputFile(templateDataset);
                // copy subsetted coordinate axes and other static (non-record) data to output file
                copyStaticData(subsettedDataset);

                outputFileCreated = true;
            }

            logger.info("Adding {} to output file. Size {} bytes", datasetLocation, dataset.getSize());

            appendTimeSlices(subsettedDataset);

            logger.info("Cumulative output file size. Size {} bytes", Files.size(outputPath));

        } catch (AggregationException | IOException e) {
            logger.error("Error adding " + datasetLocation + " to output file.", e);
            throw new AggregationException(e);
        }
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

    private void createOutputFile(NetcdfDatasetIF template) throws AggregationException {
        try {
            Nc4Chunking chunking = new Nc4ChunkingDefault(3, false);
            writer = NetcdfFileWriter.createNew(Version.netcdf4,
                outputPath.toString(), chunking);

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

            // Create file

            writer.create();
        } catch (IOException e) {
            throw new AggregationException("Could not create output file", e);
        }
    }

    private void copyStaticData(NetcdfDatasetIF subsettedDataset) throws AggregationException {
        for (NetcdfVariable variable: templateDataset.getVariables()) {
            if (variable.isUnlimited()) {
                continue;
            }

            NetcdfVariable subsettedVariable = subsettedDataset.findVariable(variable.getShortName());
            Variable outputVariable = writer.findVariable(variable.getShortName());
            copy(subsettedVariable, outputVariable);
        }
    }

    private void copy(NetcdfVariable oldVar, Variable newVar) throws AggregationException {
        try {
            if (oldVar.getRank() == 0) {
                // not an array - just copy the data - it can't be chunked
                writer.write(newVar, oldVar.read());
                writer.flush();
            } else {
                chunkedCopy(oldVar, newVar, 0);
            }
        } catch (InvalidRangeException|IOException e) {
            throw new AggregationException(e);
        }
    }

    private void appendTimeSlices(NetcdfDatasetIF dataset) throws AggregationException {
        if (dataset.getTimeAxis() == null) {
            return; // no time varying data
        }

        for (NetcdfVariable templateVariable: templateDataset.getVariables()) {
            if (!templateVariable.isUnlimited()) {
                continue;
            }

            NetcdfVariable subsettedVariable = dataset.findVariable(templateVariable.getShortName());
            Variable outputVariable = writer.findVariable(subsettedVariable.getShortName());
            chunkedCopy(subsettedVariable, outputVariable, slicesWritten);
        }

        slicesWritten += dataset.getTimeAxis().getSize();
    }

    private void chunkedCopy(NetcdfVariable subsettedVariable, Variable outputVariable, int offset) throws AggregationException {
        try {
            // copy data in maxChunkSize chunks (perhaps some of this should go into AbstractVariable)
            long maxChunkElems = subsettedVariable.getMaxChunkSize() / subsettedVariable.getDataType().getSize();

            IndexChunkIterator index = new IndexChunkIterator(subsettedVariable.getShape(), maxChunkElems);
            while (index.hasNext()) {
                // read next chunk of data from source variable
                IndexChunk chunk = index.next();
                Array data = subsettedVariable.read(chunk.getOffset(), chunk.getShape());

                // determine where chunk should be copied to in output variable taking into account existing data
                // copied from other files (offset in first dimension)
                int[] outputPosition = chunk.getOffset();
                outputPosition[0] += offset;

                if (data.getSize() > 0) {// zero when record dimension = 0
                    writer.write(outputVariable, outputPosition, data);
                    writer.flush();
                }
            }
        } catch (InvalidRangeException|IOException e) {
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

    private Map<String, UnpackerOverrides> getUnpackerOverrides(List<VariableOverrides> variableOverridesList) {
        Map<String, UnpackerOverrides> result = new LinkedHashMap<>();

        for (VariableOverrides overrides: variableOverridesList) {
            Builder builder = new UnpackerOverrides.Builder();
            builder.newDataType(overrides.getType());
            builder.newFillerValue(overrides.getFillerValue());
            builder.newValidMin(overrides.getValidMin());
            builder.newValidMax(overrides.getValidMax());
            builder.newValidRange(overrides.getValidRange());
            builder.newMissingValues(overrides.getMissingValues());
            result.put(overrides.getName(), builder.build());
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

    // Usage: java -jar {classpath} au.org.emii.aggregator.NetcdfAggregator [-b bbox] [-z zsubset] [-t timeRange] [-o overridesConfigFile] fileList
    // fileList should contain a list of files to be included in the aggregation

    public static void main(String[] args) throws ParseException, AggregationException, IOException, InvalidRangeException {
        Options options = new Options();

        options.addOption("b", true, "restrict to bounding box specified by left lower/right upper coordinates e.g. -b 120,-32,130,-29");
        options.addOption("z", true, "restrict data to specified z index range e.g. -z 2,4");
        options.addOption("t", true, "restrict data to specified date/time range in ISO 8601 format e.g. -t 2017-01-12T21:58:02Z,2017-01-12T22:58:02Z");
        options.addOption("o", true, "xml file containing aggregation overrides to be applied");
        options.addOption("c", true, "maximum number of variable bytes to copy at a time");

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        List<String> inputFiles = Files.readAllLines(Paths.get(line.getArgs()[0]), Charset.forName("utf-8"));
        Path outputFile = Paths.get(line.getArgs()[1]);

        String bboxArg = line.getOptionValue("b");
        String zSubsetArg = line.getOptionValue("z");
        String timeArg = line.getOptionValue("t");
        String overridesArg = line.getOptionValue("o");
        String maxChunkSizeArg = line.getOptionValue("c");

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

        NumberRange zSubset = null;

        if (zSubsetArg != null) {
            String[] zSubsetIndexes = zSubsetArg.split(",");
            zSubset = new NumberRange(zSubsetIndexes[0], zSubsetIndexes[1]);
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

        Long maxChunkSize = null;

        if (maxChunkSizeArg != null) {
            maxChunkSize = new Long(maxChunkSizeArg);
        }

        try (
            NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, overrides, maxChunkSize, bbox, zSubset, timeRange)
        ) {
            for (String file:inputFiles) {
                if (file.trim().length() == 0) continue;
                netcdfAggregator.add(file);
            }
        }
    }

}
