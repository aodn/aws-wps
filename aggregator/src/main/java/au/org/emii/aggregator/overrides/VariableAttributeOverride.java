package au.org.emii.aggregator.overrides;

import au.org.emii.aggregator.datatype.NumericTypes;
import org.apache.commons.lang.StringUtils;
import ucar.ma2.DataType;

import java.util.List;

/**
 * Output attribute config
 */

public class VariableAttributeOverride {
    private String name;
    private DataType type;

    private List<String> values;

    public VariableAttributeOverride(String name, DataType dataType, List<String> values) {
        this.name = name;
        this.type = dataType;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public List<String> getValues() {
        return values;
    }

    public String toString() {
        return String.format("name=%s type=%s values=%s", name, type, StringUtils.join(values, ","));
    }

    public Number getAttributeNumericValue() {
        return NumericTypes.parse(type, values.get(0));
    }

    public Number getAttributeNumericValue(DataType variableDataType) {
        return NumericTypes.parse(variableDataType, values.get(0));
    }

    public Number[] getAttributeNumericValues() {
        return getAttributeNumericValues(type);
    }

    public Number[] getAttributeNumericValues(DataType variableDataType) {
        Number[] result = new Number[values.size()];

        for (int i=0; i<values.size(); i++) {
            result[i] = NumericTypes.parse(variableDataType, values.get(i));
        }

        return result;
    }
}


