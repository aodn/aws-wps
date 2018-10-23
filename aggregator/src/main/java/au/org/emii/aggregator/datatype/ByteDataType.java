package au.org.emii.aggregator.datatype;

import static ucar.nc2.iosp.netcdf3.N3iosp.NC_FILL_BYTE;

/**
 * Byte DataType helper
 */
public class ByteDataType implements NumericType {

    @Override
    public Number valueOf(Number number) {
        return number.byteValue();
    }

    @Override
    public boolean isDefaultFillValue(Number value) {
        return value.equals(NC_FILL_BYTE) || value.equals(Byte.MIN_VALUE) || value.equals(Byte.MAX_VALUE);
    }

    @Override
    public Number defaultFillValue() {
        return NC_FILL_BYTE;
    }

    @Override
    public Number parse(String value) {
        return Byte.parseByte(value);
    }


}
