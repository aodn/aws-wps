package au.org.emii.aggregator.overrides.xstream;

import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.GlobalAttributeOverrides;
import au.org.emii.aggregator.overrides.VariableOverrides;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Deserialise a Template xml configuration element into a Template object
 */
public class AggregationOverridesConverter extends DeserialisingOnlyConverter {
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        GlobalAttributeOverrides attributeOverrides = new GlobalAttributeOverrides();
        List<VariableOverrides> variableOverrides = new ArrayList<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (reader.getNodeName().equals("attributes")) {
                attributeOverrides = (GlobalAttributeOverrides)context.convertAnother(null, GlobalAttributeOverrides.class);
            }

            if (reader.getNodeName().equals("variables")) {
                variableOverrides = CollectionReader.readCollection(reader, context, "variable", VariableOverrides.class);
            }

            reader.moveUp();
        }

        return new AggregationOverrides(attributeOverrides, variableOverrides);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(AggregationOverrides.class);
    }
}
