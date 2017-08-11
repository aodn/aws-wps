package au.org.emii.aggregator.variable;

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
    String getShortName();

    boolean isUnlimited();
    boolean isUnsigned();

    DataType getDataType();
    AxisType getAxisType();

    int getRank();
    long getSize();
    int getElementSize();
    int[] getShape();

    List<Dimension> getDimensions();
    int findDimensionIndex(String shortName);
    Dimension findDimension(String shortName);

    java.util.List<Attribute> getAttributes();
    Attribute findAttribute(String attName);

    Array read(int[] origin, int[] shape) throws InvalidRangeException, IOException;
    Array read() throws IOException;

    double getMin();
    double getMax();
}
