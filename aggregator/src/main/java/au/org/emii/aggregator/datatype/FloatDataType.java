package au.org.emii.aggregator.datatype;

import static ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_FLOAT;

/**
 *Float DataType helper
 */
public class FloatDataType implements NumericType {

    @Override
    public Number valueOf(Number number) {
        return number.floatValue();
    }

    @Override
    public boolean isDefaultFillValue(Number value) {
        return value.equals(NC_FILL_FLOAT);
    }

    @Override
    public Number defaultFillValue() {
        return NC_FILL_FLOAT;
    }

    @Override
    public Number parse(String value) {
        return Float.parseFloat(value);
    }
}
