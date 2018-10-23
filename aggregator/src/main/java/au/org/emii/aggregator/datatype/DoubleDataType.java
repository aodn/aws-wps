package au.org.emii.aggregator.datatype;

import static ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_DOUBLE;

/**
 * Double DataType helper
 */
public class DoubleDataType implements NumericType {

    @Override
    public Number valueOf(Number number) {
        return number.doubleValue();
    }

    @Override
    public boolean isDefaultFillValue(Number value) {
        return value.equals(NC_FILL_DOUBLE);
    }

    @Override
    public Number defaultFillValue() {
        return NC_FILL_DOUBLE;
    }

    @Override
    public Number parse(String value) {
        return Double.parseDouble(value);
    }
}
