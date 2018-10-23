package au.org.emii.aggregator.overrides;

import ucar.ma2.DataType;
import ucar.nc2.constants.CDM;

import java.util.ArrayList;
import java.util.List;

import static au.org.emii.aggregator.attribute.Attributes.VALID_MAX;
import static au.org.emii.aggregator.attribute.Attributes.VALID_MIN;

/**
 * Output variable config
 */
public class VariableOverrides {
    private String name;
    private DataType type;
    private List<VariableAttributeOverride> attributes;

    public VariableOverrides(String name, DataType type, List<VariableAttributeOverride> attributes) {
        this.name = name;
        this.type = type;
        this.attributes = attributes;
    }

    public VariableOverrides(String name) {
        this(name, null, new ArrayList<VariableAttributeOverride>());
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public List<VariableAttributeOverride> getAttributes() {
        return attributes;
    }

    public Number getFillerValue() {
        return getAttributeNumericValue(CDM.FILL_VALUE);
    }

    public Number getValidMin() {
        return getAttributeNumericValue(VALID_MIN);
    }

    public Number getValidMax() {
        return getAttributeNumericValue(VALID_MAX);
    }

    public Number[] getValidRange() {
        return getAttributeNumericValues(CDM.VALID_RANGE);
    }

    public Number[] getMissingValues() {
        return getAttributeNumericValues(CDM.MISSING_VALUE);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("name=");
        builder.append(name);
        builder.append(" type=");
        builder.append(type);
        builder.append(" attributes=");

        for (VariableAttributeOverride attribute: attributes) {
            builder.append(attribute.getName());
            builder.append(" ");
        }

        return builder.toString();
    }

    private Number[] getAttributeNumericValues(String name) {
        VariableAttributeOverride attributeOverride = findAttribute(name);

        if (attributeOverride == null)
            return null;
        else if (attributeOverride.getType() == null)
            return attributeOverride.getAttributeNumericValues(type);
        else
            return attributeOverride.getAttributeNumericValues();
    }

    private Number getAttributeNumericValue(String name) {
        VariableAttributeOverride attributeOverride = findAttribute(name);

        if (attributeOverride == null)
            return null;
        else if (attributeOverride.getType() == null)
            return attributeOverride.getAttributeNumericValue(type);
        else
            return attributeOverride.getAttributeNumericValue();
    }

    private VariableAttributeOverride findAttribute(String name) {
        for (VariableAttributeOverride attributeOverride: attributes) {
            if (attributeOverride.getName().equals(name)) {
                return attributeOverride;
            }
        }

        return null;
    }

}
