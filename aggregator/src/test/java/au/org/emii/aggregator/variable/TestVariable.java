package au.org.emii.aggregator.variable;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestVariable extends AbstractVariable {
    private final String name;
    private final List<Dimension> dimensions;
    private final Array data;

    public TestVariable(String name, List<Dimension> dimensions, Array data, long maxChunkSize) {
        super(maxChunkSize);
        this.name = name;
        this.dimensions = dimensions;
        this.data = data;
    }

    public TestVariable(String name, String[] dimensionNames, Array data, long maxChunkSize) {
        this(name, getDimensions(dimensionNames, data.getShape()), data, maxChunkSize);
    }

    @Override
    public String getShortName() {
        return "name";
    }

    @Override
    public boolean isUnsigned() {
        return false;
    }

    @Override
    public DataType getDataType() {
        return data.getDataType();
    }

    @Override
    public AxisType getAxisType() {
        return null;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    @Override
    public List<Attribute> getAttributes() {
        return new ArrayList<>();
    }

    @Override
    public Array read(int[] origin, int[] shape) throws InvalidRangeException, IOException {
        Array section = data.sectionNoReduce(origin, shape, null);
        return Array.factory(section.copyToNDJavaArray());
    }

    private static List<Dimension> getDimensions(String[] dimensions, int[] shape) {
        List<Dimension> result = new ArrayList<>();

        for  (int i=0 ; i<shape.length; i++) {
            result.add(new Dimension(dimensions[i], shape[i]));
        }

        return result;
    }

}
