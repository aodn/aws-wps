package au.org.emii.aggregator.variable;

import au.org.emii.aggregator.variable.AbstractVariable.NumericValue;
import au.org.emii.util.NumberRange;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;

import java.io.IOException;
import java.util.List;

/**
 * NetCDF variable api used in this library
 */
public interface NetcdfVariable {
    long DEFAULT_MAX_CHUNK_SIZE = 10 * 1000 * 1000; // 10 MB

    String getShortName();

    boolean isUnlimited();
    boolean isUnsigned();

    DataType getDataType();
    AxisType getAxisType();

    int getRank();
    long getSize();
    int[] getShape();

    List<Dimension> getDimensions();

    java.util.List<Attribute> getAttributes();
    Attribute findAttribute(String attName);

    Array read(int[] origin, int[] shape) throws InvalidRangeException, IOException;
    Array read() throws IOException;

    Iterable<NumericValue> getNumericValues();

    NumberRange getBounds();

    long getMaxChunkSize();
}
