package au.org.emii.aggregator.datatype;

/**
 * Numeric DataType helper interface
 */
public interface NumericType {
    Number valueOf(Number number);

    boolean isDefaultFillValue(Number value);

    Number defaultFillValue();

    Number parse(String value);
}
