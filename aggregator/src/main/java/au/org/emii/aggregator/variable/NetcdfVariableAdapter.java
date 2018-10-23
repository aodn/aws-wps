package au.org.emii.aggregator.variable;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableDS;

import java.io.IOException;
import java.util.List;

/**
 * Convert a ucar.m2.VariableDS to the NetcdfVariable interface used in this library
 */
public class NetcdfVariableAdapter extends AbstractVariable {

    private final VariableDS variable;

    public NetcdfVariableAdapter(VariableDS variable, long maxChunkSize) {
        super(maxChunkSize);
        this.variable = variable;
    }

    @Override
    public String getShortName() {
        return variable.getShortName();
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
        return variable instanceof CoordinateAxis ? ((CoordinateAxis) variable).getAxisType() : null;
    }

    @Override
    public List<Dimension> getDimensions() {
        return variable.getDimensions();
    }

    @Override
    public List<Attribute> getAttributes() {
        return variable.getAttributes();
    }

    @Override
    public Array read(int[] origin, int[] shape) throws InvalidRangeException, IOException {
        return variable.read(origin, shape);
    }

}
