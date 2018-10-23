package au.org.emii.aggregator.datatype;

import static ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_LONG;

/**
 * Long DataType helper
 */
public class LongDataType implements NumericType {

    @Override
    public Number valueOf(Number number) {
        return number.longValue();
    }

    @Override
    public boolean isDefaultFillValue(Number value) {
        return value.equals(NC_FILL_LONG) || value.equals(java.lang.Long.MIN_VALUE) || value.equals(java.lang.Long.MAX_VALUE);
    }

    @Override
    public Number defaultFillValue() {
        return NC_FILL_LONG;
    }

    @Override
    public Number parse(String value) {
        return Long.parseLong(value);
    }
}
