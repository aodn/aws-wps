package au.org.emii.aggregator.template;

import au.org.emii.aggregator.variable.AbstractVariable;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple representation of a NetCDF Variable for use as a template for creating aggregation variables
 */
public class TemplateVariable extends AbstractVariable {
    private final String shortName;
    private final List<Attribute> attributes;
    private final List<Dimension> dimensions;
    private final Array data;
    private final AxisType axisType;

    public TemplateVariable(NetcdfVariable variable, String timeDimension) {
        try {
            this.shortName = variable.getShortName();
            this.attributes = variable.getAttributes();
            this.axisType = variable.getAxisType();

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

            if (variable.findDimension(timeDimension) != null) {
                // time varying data - don't copy to template
                this.data = Array.factory(variable.getDataType(), getShape());
            } else {
                // static data (coordinate variables) - copy to template
                this.data = variable.read().copy();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TemplateVariable(String temp, ArrayList<Attribute> attributes, List<Dimension> dimensons, AxisType type,
                            Array data) {
        this.shortName = temp;
        this.attributes = attributes;
        this.dimensions = dimensons;
        this.axisType = type;
        this.data = data;
    }

    @Override
    public String getShortName() {
        return shortName;
    }

    @Override
    public DataType getDataType() {
        return data.getDataType();
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
        return Array.factory(data.sectionNoReduce(origin, shape, null).copyToNDJavaArray());
    }

    @Override
    public boolean isUnsigned() {
        return data.isUnsigned();
    }

}
