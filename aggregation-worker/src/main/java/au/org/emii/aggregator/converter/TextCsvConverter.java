package au.org.emii.aggregator.converter;


import au.org.emii.netcdf.iterator.IndexRangesBuilder;
import au.org.emii.netcdf.iterator.IndexValue;
import au.org.emii.netcdf.iterator.reader.NetcdfReader;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.EnhanceScaleMissing;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDateUnit;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class TextCsvConverter extends Converter {

    final static String MIME_TYPE = "text/csv";
    private final static String EXTENSION = "csv";

    private final static boolean FILL_VALUE_IS_MISSING = true;
    private final static boolean INVALID_DATA_IS_MISSING = true;
    private final static boolean MISSING_DATA_IS_MISSING = true;
    private final static boolean DONT_USE_NANS = false;

    private Set<NetcdfDataset.Enhance> enhanceMode;

    private static final Logger logger = LoggerFactory.getLogger(TextCsvConverter.class);

    public TextCsvConverter() {
        enhanceMode = new HashSet<>();
        enhanceMode.add(NetcdfDataset.Enhance.ScaleMissing);
        enhanceMode.add(NetcdfDataset.Enhance.ConvertEnums);
        enhanceMode.add(NetcdfDataset.Enhance.CoordSystems);
    }

    @Override
    public String getMimeType() { return MIME_TYPE; }

    @Override
    public String getExtension() { return EXTENSION; }

    @Override
    public void convert(Path inputFile, Path outputFile) throws Exception {
        NetcdfDataset ncDataset = null;

        try (PrintStream out = new PrintStream(Files.newOutputStream(outputFile, CREATE_NEW))) {
            ncDataset = openNetcdfDataset(inputFile);
            Set<Variable> variables = getVariables(ncDataset);
            Variable timeVariable = getTimeVariable(ncDataset);
            writeHeaderLine(out, variables, timeVariable);
            Map<Variable, NetcdfReader> variableReaders = createVariableReaders(variables);
            Iterator<Set<IndexValue>> indexTupleIterator = buildIndexTupleIterator(variables);

            while (indexTupleIterator.hasNext()) {
                Set<IndexValue> indexTuple = indexTupleIterator.next();
                writeCsvLine(out, variableReaders, indexTuple, timeVariable);
            }
        } catch (Exception e) {
            FileUtils.deleteQuietly(outputFile.toFile());
            logger.error("Error converting output to csv", e);
            throw new RuntimeException(String.format("Could not convert output to csv: '%s'", e.getMessage()));
        } finally {
            closeQuietly(ncDataset);
        }
    }

    private Set<Variable> getVariables(NetcdfDataset ncDataset) {
        Set<Variable> variables = new LinkedHashSet<>();

        // add coordinate variables first

        variables.addAll(ncDataset.getCoordinateAxes());

        // add other variables

        variables.addAll(ncDataset.getVariables());

        return variables;
    }

    private Variable getTimeVariable(NetcdfDataset ncDataset) {
        return ncDataset.findCoordinateAxis(AxisType.Time);
    }

    private NetcdfDataset openNetcdfDataset(Path inputFile) throws IOException {
        NetcdfDataset ncDataset = NetcdfDataset.openDataset(inputFile.toString(), enhanceMode, 0, null, null);
        setMissingValueHandling(
                ncDataset,
                FILL_VALUE_IS_MISSING,
                INVALID_DATA_IS_MISSING,
                MISSING_DATA_IS_MISSING,
                DONT_USE_NANS
        );
        return ncDataset;
    }

    private void setMissingValueHandling(NetcdfDataset ncDataset,
                                         boolean fillValueIsMissing, boolean invalidDataIsMissing,
                                         boolean missingDataIsMissing, boolean useNaNs) {
        for (Variable ncVariable: ncDataset.getVariables()) {
            EnhanceScaleMissing scaleMissingDecorator = (EnhanceScaleMissing) ncVariable;
            scaleMissingDecorator.setFillValueIsMissing(fillValueIsMissing);
            scaleMissingDecorator.setInvalidDataIsMissing(invalidDataIsMissing);
            scaleMissingDecorator.setMissingDataIsMissing(missingDataIsMissing);
            scaleMissingDecorator.setUseNaNs(useNaNs);
        }
    }

    private void writeHeaderLine(PrintStream out, Set<Variable> variables, Variable timeVariable) {
        Iterator<Variable> variableIterator = variables.iterator();

        while (variableIterator.hasNext()) {
            Variable ncVariable = variableIterator.next();

            String shortName = ncVariable.getShortName();
            out.print(shortName);

            if (ncVariable.equals(timeVariable)) {
                out.print(" (UTC)");
            }
            else {
                Attribute unitsAttribute = ncVariable.findAttribute("units");

                if (unitsAttribute != null) {
                    out.format(" (%s)", unitsAttribute.getStringValue());
                }
            }

            if (variableIterator.hasNext()) {
                out.print(",");
            }
        }

        out.println();
    }

    private Map<Variable, NetcdfReader> createVariableReaders(Set<Variable> variables)
            throws IOException {
        Map<Variable, NetcdfReader> variableReaders = new LinkedHashMap<>();

        for (Variable ncVariable: variables) {
            variableReaders.put(ncVariable, new NetcdfReader(ncVariable));
        }

        return variableReaders;
    }

    private Iterator<Set<IndexValue>> buildIndexTupleIterator(Set<Variable> variables) {
        IndexRangesBuilder indexRangesBuilder = new IndexRangesBuilder();

        for (Variable ncVariable: variables) {
            indexRangesBuilder.addDimensions(ncVariable);
        }

        return indexRangesBuilder.getIterator();
    }

    private void writeCsvLine(PrintStream out, Map<Variable, NetcdfReader> variableReaders, Set<IndexValue> indexTuple, Variable timeVariable) {
        Iterator<Variable> variableReaderIterator = variableReaders.keySet().iterator();

        while (variableReaderIterator.hasNext()) {
            Variable ncVariable = variableReaderIterator.next();

            if (ncVariable.equals(timeVariable)) {
                out.print(getTimeDisplayValue(ncVariable, variableReaders.get(ncVariable), indexTuple));
            }
            else {
                out.print(getDisplayValue(ncVariable, variableReaders.get(ncVariable), indexTuple));
            }

            if (variableReaderIterator.hasNext()) {
                out.print(",");
            }
        }
        out.println();
    }

    private String getTimeDisplayValue(Variable variable, NetcdfReader variableReader, Set<IndexValue> indexTuple) {
        if (isMissing(variable, variableReader, indexTuple)) {
            return "";
        }
        else {
            Attribute calendarAttribute = variable.findAttribute("calendar");

            Attribute unitsAttribute = variable.findAttribute("units");
            Double time = Double.valueOf(getDisplayValue(variable, variableReader, indexTuple));

            String calendarString = calendarAttribute == null ? "gregorian" : calendarAttribute.getStringValue().toLowerCase();
            Calendar calendar = Calendar.get(calendarString);
            CalendarDateUnit dateUnit = CalendarDateUnit.withCalendar(calendar, unitsAttribute.getStringValue());
            return dateUnit.makeCalendarDate(time).toString();
        }
    }

    private String getDisplayValue(Variable variable, NetcdfReader variableReader, Set<IndexValue> indexTuple) {
        if (isMissing(variable, variableReader, indexTuple)) {
            return "";
        } else {
            return variableReader.getString(indexTuple);
        }
    }

    private boolean isMissing(Variable variable, NetcdfReader variableReader, Set<IndexValue> indexTuple) {
        EnhanceScaleMissing scaleMissingDecorator = (EnhanceScaleMissing) variable;
        return scaleMissingDecorator.hasMissing() && scaleMissingDecorator.isMissing(variableReader.getDouble(indexTuple));
    }

    private void closeQuietly(NetcdfDataset ncDataset) {
        if (ncDataset == null) {
            return;
        }

        try {
            ncDataset.close();
        } catch (IOException e) {
            logger.warn("Failed to close input file", e);
        }
    }

}
