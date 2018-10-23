package au.org.emii.aggregator.variable;


import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by craigj on 17/02/17.
 */
public class SubsettedVariable extends AbstractVariable {

    private final Map<String, Range> subsets;
    private final NetcdfVariable variable;
    private final List<Dimension> dimensions;


    public SubsettedVariable(NetcdfVariable variable, Map<String, Range> subsets) {
        assertSubsetValid(variable, subsets);

        this.variable = variable;
        this.subsets = subsets;

        dimensions = subsetDimensions(variable.getDimensions());
    }

    @Override
    public String getShortName() {
        return variable.getShortName();
    }

    @Override
    public boolean isUnlimited() {
        return variable.isUnlimited();
    }

    @Override
    public boolean isUnsigned() {
        return variable.isUnsigned();
    }

    @Override
    public DataType getDataType() {
        return variable.getDataType();
    }

    @Override
    public AxisType getAxisType() {
        return variable.getAxisType();
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    @Override
    public List<Attribute> getAttributes() {
        List<Attribute> result = new ArrayList<>();

        for (Attribute attribute: variable.getAttributes()) {
            // Ignore any existing chunking instructions as they will most likely be incompatible
            // with any subsetting performed along with attributes added when enchancing the variable
            if (attribute.getFullName().startsWith("_Chunk") || attribute.getFullName().startsWith("_Coordinate")) {
                continue;
            }

            result.add(attribute);
        }

        return result;
    }

    @Override
    public Array read(int[] origin, int[] shape) throws InvalidRangeException, IOException {
        if (origin.length != getRank()) {
            throw new IllegalArgumentException("Origin does not have same rank as variable");
        }

        if (shape.length != getRank()) {
            throw new IllegalArgumentException("Shape does not have same rank as variable");
        }

        int[] mappedOrigin = new int[variable.getRank()];

        int i = 0;

        for (Dimension dimension: dimensions) {
            Range subset = subsets.get(dimension.getShortName());

            if (subset == null) {
                mappedOrigin[i] = origin[i];
            } else {
                mappedOrigin[i] = subset.first() + origin[i];
            }

            i++;
        }

        return variable.read(mappedOrigin, shape);
    }

    @Override
    public long getMaxChunkSize() {
        return variable.getMaxChunkSize();
    }

    private List<Dimension> subsetDimensions(List<Dimension> dimensions) {
        List<Dimension> result = new ArrayList<>();

        for (Dimension dimension: dimensions) {
            Range subset = subsets.get(dimension.getShortName());

            int length = subset != null ? subset.length() : dimension.getLength();

            result.add(new Dimension(
                    dimension.getShortName(), length, dimension.isShared(), dimension.isUnlimited(),
                    dimension.isVariableLength()));
        }

        return result;
    }

    private static void assertSubsetValid(NetcdfVariable variable, Map<String, Range> subsets) {
        for (Dimension dimension: variable.getDimensions()) {
            Range subset = subsets.get(dimension.getShortName());

            if (subset != null) {
                assertSubsetValid(dimension, subset);
            }
        }
    }

    private static void assertSubsetValid(Dimension dimension, Range subset) {
        if (subset.stride() != 1) {
            throw new UnsupportedOperationException("Stride subsetting supported");
        } else if (subset.last() > dimension.getLength()) {
            throw new IllegalArgumentException(String.format("Subset out of range for %s", dimension.getShortName()));
        }
    }

}
