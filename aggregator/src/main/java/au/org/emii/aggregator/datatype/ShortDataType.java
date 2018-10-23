package au.org.emii.aggregator.datatype;

import static ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_SHORT;

/**
 * Short DataType helper
 */
public class ShortDataType implements NumericType {

    @Override
    public Number valueOf(Number number) {
        return number.shortValue();
    }

    @Override
    public boolean isDefaultFillValue(Number value) {
        return value.equals(NC_FILL_SHORT) || value.equals(Short.MIN_VALUE) || value.equals(Short.MAX_VALUE);
    }

    @Override
    public Number defaultFillValue() {
        return NC_FILL_SHORT;
    }

    @Override
    public Number parse(String value) {
        return Short.parseShort(value);
    }
}
