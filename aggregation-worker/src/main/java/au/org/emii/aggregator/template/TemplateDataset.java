package au.org.emii.aggregator.template;

import au.org.emii.aggregator.dataset.AbstractNetcdfDataset;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.datatype.NumericTypes;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.template.TemplateVariable;
import au.org.emii.aggregator.template.ValueTemplate;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Template output dataset definition - contains all non-time varying information to be
 * written to output dataset.
 */
public class TemplateDataset extends AbstractNetcdfDataset {
    private final List<Attribute> globalAttributes;
    private final List<Dimension> dimensions;
    private final List<NetcdfVariable> variables;

    public TemplateDataset(NetcdfDatasetIF dataset, AggregationOverrides aggregationOverrides,
                           CalendarDateRange timeRange, Range verticalSubset, LatLonRect bbox) {

        this.globalAttributes = getGlobalAttributes(dataset, aggregationOverrides.getAttributes(), timeRange);

        // Determine variables/dimensions to add

        List<NetcdfVariable> templateVariables = new ArrayList<>();
        Map<String, Dimension> templateDimensions = new LinkedHashMap<>();

        String timeDimension = dataset.getTimeAxis().getDimensionName();

        for (NetcdfVariable variable: dataset.getVariables()) {
            if (!aggregationOverrides.includeVariable(variable.getShortName())) {
                continue;
            }

            TemplateVariable templateVariable = new TemplateVariable(variable, timeDimension);

            templateVariables.add(templateVariable);

            for (Dimension templateDimension: templateVariable.getDimensions()) {
                templateDimensions.put(templateDimension.getShortName(), templateDimension);
            }
        }

        // Get template dimensions in original dataset order

        List<Dimension> orderedTemplateDimensions = new ArrayList<>();

        for (Dimension dimension: dataset.getDimensions()) {
            Dimension templateDimension = templateDimensions.get(dimension.getShortName());

            if (templateDimension != null) {
                orderedTemplateDimensions.add(templateDimension);
            }
        }

        this.dimensions = new ArrayList<>(orderedTemplateDimensions);
        this.variables = new ArrayList<>(templateVariables);
    }

    @Override
    public List<Attribute> getGlobalAttributes() {
        return globalAttributes;
    }

    @Override
    public List<NetcdfVariable> getVariables() {
        return variables;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    private List<Attribute> getGlobalAttributes(NetcdfDatasetIF dataset, List<GlobalAttributeOverride> attributeOverrides,
                                                CalendarDateRange timeRange) {
        // Copy existing attributes

        Map<String, Attribute> result = new LinkedHashMap<>();

        for (Attribute attribute : dataset.getGlobalAttributes()) {
            result.put(attribute.getShortName(), attribute);
        }

        // Apply attribute overrides

        for (GlobalAttributeOverride override: attributeOverrides) {
            String attributeName = override.getName();

            if (result.containsKey(attributeName)) {
                Map<String, String> substitutableValues = getSubstitutableValues(dataset, timeRange);
                result.put(attributeName, applyOverride(result.get(attributeName), override, substitutableValues));
            } else {
                result.put(attributeName, createAttribute(override.getName(), override.getType(), override.getValue()));
            }
        }

        return new ArrayList<>(result.values());
    }

    private Map<String, String> getSubstitutableValues(NetcdfDatasetIF dataset, CalendarDateRange timeRange) {
        Map<String, String> result = new LinkedHashMap<>();

        LatLonRect newBbox = dataset.getBbox();

        result.put("LAT_MIN", Double.toString(newBbox.getLatMin()));
        result.put("LAT_MAX", Double.toString(newBbox.getLatMax()));
        result.put("LON_MIN", Double.toString(newBbox.getLonMin()));
        result.put("LON_MAX", Double.toString(newBbox.getLonMax()));

        if (timeRange != null) {
            result.put("TIME_START", timeRange.getStart().toString());
            result.put("TIME_END", timeRange.getEnd().toString());
        }

        return result;
    }

    private Attribute applyOverride(Attribute attribute, GlobalAttributeOverride override,
                                    Map<String, String> substitutableValues) {
        if (attribute.isArray()) {
            throw new UnsupportedOperationException("Update of array attributes not supported");
        }

        String currentValue = attribute.isString() ? attribute.getStringValue() : attribute.getNumericValue().toString();
        ValueTemplate valueTemplate = new ValueTemplate(override.getPattern(), override.getValue());
        String newValue = valueTemplate.getValue(currentValue, substitutableValues);

        return createAttribute(override.getName(), override.getType(), newValue);
    }

    private Attribute createAttribute(String name, DataType type, String value) {
        if (type.isNumeric()) {
            return new Attribute(name, NumericTypes.parse(type, value));
        } else {
            return new Attribute(name, value);
        }
    }
}
