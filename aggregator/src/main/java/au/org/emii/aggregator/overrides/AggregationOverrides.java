package au.org.emii.aggregator.overrides;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration applicable to aggregation
 */

public class AggregationOverrides {
    private GlobalAttributeOverrides attributeOverrides;
    private List<VariableOverrides> variableOverridesList;

    public AggregationOverrides(GlobalAttributeOverrides attributeOverrides,
                                List<VariableOverrides> variableOverridesList) {
        this.attributeOverrides = attributeOverrides;
        this.variableOverridesList = variableOverridesList;
    }

    public AggregationOverrides() {
        this.attributeOverrides = new GlobalAttributeOverrides();
        this.variableOverridesList = new ArrayList<>();
    }

    public GlobalAttributeOverrides getAttributeOverrides() {
        return attributeOverrides;
    }

    public List<VariableOverrides> getVariableOverridesList() {
        return variableOverridesList;
    }

    public boolean includeVariable(String name) {
        return variableOverridesList.size() == 0 // no variable overrides specified means include all variables
            || hasVariableOverride(name); // variable overrides specified and the variable is included
    }

    public boolean isEmpty() {
        return attributeOverrides.isEmpty() && variableOverridesList.isEmpty();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("attributes=");

        builder.append(attributeOverrides.toString());

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
