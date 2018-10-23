package au.org.emii.aggregator.dataset;

import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.variable.NetcdfVariableAdapter;
import au.org.emii.aggregator.variable.UnpackedVariable;
import au.org.emii.aggregator.variable.UnpackerOverrides;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static au.org.emii.aggregator.variable.NetcdfVariable.DEFAULT_MAX_CHUNK_SIZE;

/**
 * Create NetcdfDatasetIF instances
 */
public class NetcdfDatasetAdapter extends AbstractNetcdfDataset implements AutoCloseable {

    private static final Set<Enhance> ADD_COORD_SYSTEMS = Collections.unmodifiableSet(EnumSet.of(
        Enhance.CoordSystems
    ));

    private final NetcdfDataset dataset;
    private final List<Dimension> dimensions;
    private final List<Attribute> globalAttributes;
    private final ArrayList<NetcdfVariable> variables;

    private static final Pattern SIZE_REGEXP = Pattern.compile("size= ([0-9]+)");

    public static NetcdfDatasetAdapter open(String location, Map<String, UnpackerOverrides> unpackerOverrides,
                                            long maxChunkSize) throws IOException {
        NetcdfDataset dataset = NetcdfDataset.openDataset(
            location, ADD_COORD_SYSTEMS, -1, null, null);

        return new NetcdfDatasetAdapter(dataset, unpackerOverrides, maxChunkSize);
    }

    public static NetcdfDatasetAdapter open(String location, Map<String, UnpackerOverrides> unpackerOverrides)
        throws IOException {
        return open(location, unpackerOverrides, DEFAULT_MAX_CHUNK_SIZE);
    }

    protected NetcdfDatasetAdapter(NetcdfDataset dataset, Map<String, UnpackerOverrides> unpackerOverrides,
                                   long maxChunkSize) {
        // Ensure coordinate systems and coordinate systems enhancements only have been applied

        EnumSet<Enhance> enhancements = dataset.getEnhanceMode();

        if (enhancements.contains(Enhance.ConvertEnums) ||
            enhancements.contains(Enhance.ScaleMissing) ||
            !enhancements.contains(Enhance.CoordSystems)) {
            throw new UnsupportedOperationException("Coordinate system enhancements and only coordinate system enhancements required");
        }

        this.dataset = dataset;
        this.dimensions = dataset.getDimensions();

        // Add global attributes from wrapped dataset removing ones added by enhancement mode

        List<Attribute> globalAttributes = new ArrayList<>();

        for (Attribute attribute: dataset.getGlobalAttributes()) {
            // Ignore attributes added when building coordinate systems
            if (attribute.getShortName().equals(_Coordinate._CoordSysBuilder)) {
                continue;
            }

            globalAttributes.add(attribute);
        }

        this.globalAttributes = globalAttributes;

        // Add unpacked variables in original file order

        Map<String, NetcdfVariable> variables = new LinkedHashMap<>();

        for (Variable variable : dataset.getReferencedFile().getVariables()) {
            VariableDS variableDS = (VariableDS) dataset.findVariable(variable.getFullNameEscaped());
            NetcdfVariable unpackedVariable = new UnpackedVariable(new NetcdfVariableAdapter(variableDS, maxChunkSize),
                unpackerOverrides.get(variableDS.getShortName()));
            variables.put(unpackedVariable.getShortName(), unpackedVariable);
        }

        this.variables = new ArrayList<>(variables.values());
    }

    public long getSize() {
        Matcher m = SIZE_REGEXP.matcher(dataset.getDetailInfo());

        if (m.find()) {
            return Long.parseLong(m.group(1));
        } else {
            return 0L;
        }
    }

    @Override
    public List<Attribute> getGlobalAttributes() {
        return globalAttributes;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    @Override
    public List<NetcdfVariable> getVariables() {
        return variables;
    }

    @Override
    public void close() throws IOException {
        dataset.close();
    }
}
