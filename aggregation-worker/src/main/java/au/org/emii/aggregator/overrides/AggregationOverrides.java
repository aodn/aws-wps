package au.org.emii.aggregator.overrides;

import au.org.emii.aggregator.overrides.AggregationOverridesConverter;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.overrides.VariableOverrides;
import au.org.emii.aggregator.variable.UnpackerOverrides;
import au.org.emii.aggregator.variable.UnpackerOverrides.Builder;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration applicable to aggregation
 */

@XStreamAlias("template")
@XStreamConverter(AggregationOverridesConverter.class)
public class AggregationOverrides {
    private List<GlobalAttributeOverride> attributes;
    private List<VariableOverrides> variableOverridesList;

    public AggregationOverrides(List<GlobalAttributeOverride> attributes, List<VariableOverrides> variableOverridesList) {
        this.attributes = attributes;
        this.variableOverridesList = variableOverridesList;
    }

    public AggregationOverrides() {
        this.attributes = new ArrayList<>();
        this.variableOverridesList = new ArrayList<>();
    }

    public List<GlobalAttributeOverride> getAttributes() {
        return attributes;
    }

    public List<VariableOverrides> getVariableOverridesList() {
        return variableOverridesList;
    }

    public boolean includeVariable(String name) {
        return variableOverridesList.size() == 0 // no variable overrides specified means include all variables
            || hasVariableOverride(name); // variable overrides specified and the variable is included
    }

    public Map<String, UnpackerOverrides> getUnpackerOverrides() {
        Map<String, UnpackerOverrides> result = new LinkedHashMap<>();

        for (VariableOverrides overrides: variableOverridesList) {
            Builder builder = new UnpackerOverrides.Builder();
            builder.newDataType(overrides.getType());
            builder.newFillerValue(overrides.getFillerValue());
            builder.newValidMin(overrides.getValidMin());
            builder.newValidMax(overrides.getValidMax());
            builder.newValidRange(overrides.getValidRange());
            builder.newMissingValues(overrides.getMissingValues());
            result.put(overrides.getName(), builder.build());
        }

        return result;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("attributes=");

        for (GlobalAttributeOverride attribute: attributes) {
            builder.append(attribute.getName());
            builder.append(" ");
        }

        builder.append("variables=");

        for (VariableOverrides variableOverrides : this.variableOverridesList) {
            builder.append(variableOverrides.getName());
            builder.append(" ");
        }

        return builder.toString();
    }

    private boolean hasVariableOverride(String name) {
        return findVariableOverride(name) != null;
    }

    private VariableOverrides findVariableOverride(String name) {
        for (VariableOverrides overrides: variableOverridesList) {
            if (overrides.getName().equals(name)) {
                return overrides;
            }
        }

        return null;
    }
}
