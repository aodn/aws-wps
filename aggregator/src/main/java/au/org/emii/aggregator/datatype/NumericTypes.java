package au.org.emii.aggregator.datatype;

import ucar.ma2.DataType;

/**
 * Numeric DataTypes helper
 */
public class NumericTypes {
    public static NumericType get(DataType dataType) {
        if (dataType.equals(DataType.BYTE)) {
            return new ByteDataType();
        } else if (dataType.equals(DataType.SHORT)) {
            return new ShortDataType();
        } else if (dataType.equals(DataType.INT)) {
            return new IntegerDataType();
        } else if (dataType.equals(DataType.LONG)) {
            return new LongDataType();
        } else if (dataType.equals(DataType.FLOAT)) {
            return new FloatDataType();
        } else if (dataType.equals(DataType.DOUBLE)) {
            return new DoubleDataType();
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported data type %s", dataType));
        }
    }

    public static Number valueOf(Number number, DataType dataType) {
        return get(dataType).valueOf(number);
    }

    public static Number[] valueOf(Number[] numbers, DataType dataType) {
        NumericType type = get(dataType);
        Number[] result = new Number[numbers.length];

        for (int i=0; i< numbers.length; i++) {
            result[i] = type.valueOf(numbers[i]);
        }

        return result;
    }

    public static boolean isDefaultFillValue(Number value, DataType dataType) {
        return get(dataType).isDefaultFillValue(value);
    }

    public static Number defaultFillValue(DataType dataType) {
        return get(dataType).defaultFillValue();
    }

    public static Number parse(DataType dataType, String value) {
        return get(dataType).parse(value);
    }
}
