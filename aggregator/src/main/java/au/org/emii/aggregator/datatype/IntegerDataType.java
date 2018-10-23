package au.org.emii.aggregator.datatype;

import static ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_INT;

/**
 * Integer DataType helper
 */
public class IntegerDataType implements NumericType {

    @Override
    public Number valueOf(Number number) {
        return number.intValue();
    }

    @Override
    public boolean isDefaultFillValue(Number value) {
        return value.equals(NC_FILL_INT) || value.equals(Integer.MIN_VALUE) || value.equals(Integer.MAX_VALUE);
    }

    @Override
    public Number defaultFillValue() {
        return NC_FILL_INT;
    }

    @Override
    public Number parse(String value) {
        return Integer.parseInt(value);
    }
}
