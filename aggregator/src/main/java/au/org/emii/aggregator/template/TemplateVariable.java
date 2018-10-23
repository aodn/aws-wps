package au.org.emii.aggregator.template;

import au.org.emii.aggregator.variable.AbstractVariable;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;

import java.util.*;

/**
 * Simple representation of a NetCDF Variable for use as a template for creating aggregation variables
 */
public class TemplateVariable extends AbstractVariable {
    private final String shortName;
    private final List<Attribute> attributes;
    private final List<Dimension> dimensions;
    private final AxisType axisType;
    private final DataType dataType;
    private final boolean isUnsigned;

    public TemplateVariable(NetcdfVariable variable, String timeDimension) {
        this.shortName = variable.getShortName();
        this.attributes = variable.getAttributes();
        this.axisType = variable.getAxisType();
        this.dataType = variable.getDataType();
        this.isUnsigned = variable.isUnsigned();

        // Copy dimensions setting any time dimension to zero length/unlimited

        List<Dimension> dimensions = new ArrayList<>();

        for (Dimension dimension : variable.getDimensions()) {
            if (dimension.getShortName().equals(timeDimension)) {
                dimensions.add(new Dimension(dimension.getShortName(), 0, true, true, false));
            } else {
                dimensions.add(new Dimension(dimension.getShortName(), dimension.getLength(), true));
            }
        }

        this.dimensions = dimensions;
    }

    @Override
    public String getShortName() {
        return shortName;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public AxisType getAxisType() {
        return axisType;
    }

    @Override
    public List<Dimension> getDimensions() {
        return new ArrayList<>(dimensions);
    }

    @Override
    public List<Attribute> getAttributes() {
        return new ArrayList<>(attributes);
    }

    @Override
    public Array read(int[] origin, int[] shape) throws InvalidRangeException {
        throw new UnsupportedOperationException("Template variable contains no data");
    }

    @Override
    public boolean isUnsigned() {
        return isUnsigned;
    }

    @Override
    public long getMaxChunkSize() {
        throw new UnsupportedOperationException("Template variable contains no data");
    }
}
