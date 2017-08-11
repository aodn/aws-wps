package au.org.emii.aggregator.overrides;

import au.org.emii.aggregator.datatype.NumericTypes;
import au.org.emii.aggregator.overrides.VariableAttributeOverrideConverter;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import org.apache.commons.lang.StringUtils;
import ucar.ma2.DataType;

import java.util.List;

/**
 * Output attribute config
 */

@XStreamAlias("attribute")
@XStreamConverter(VariableAttributeOverrideConverter.class)
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

    public Number[] getAttributeNumericValues(String name) {
        Number[] result = new Number[values.size()];

        for (int i=0; i<values.size(); i++) {
            result[i] = NumericTypes.parse(type, values.get(i));
        }

        return result;
    }

}


