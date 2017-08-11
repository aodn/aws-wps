package au.org.emii.aggregator.overrides;

import au.org.emii.aggregator.overrides.CollectionReader;
import au.org.emii.aggregator.overrides.VariableOverrides;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Deserialise a Template xml configuration element into a Template object
 */
public class AggregationOverridesConverter implements Converter {
    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
        throw new UnsupportedOperationException("Serialising templates is not supported");
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        List<GlobalAttributeOverride> attributes = new ArrayList<>();
        List<VariableOverrides> variableOverrides = new ArrayList<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (reader.getNodeName().equals("attributes")) {
                attributes = au.org.emii.aggregator.overrides.CollectionReader.readCollection(reader, context, "attribute", GlobalAttributeOverride.class);
            }

            if (reader.getNodeName().equals("variables")) {
                variableOverrides = CollectionReader.readCollection(reader, context, "variable", VariableOverrides.class);
            }

            reader.moveUp();
        }


        return new AggregationOverrides(attributes, variableOverrides);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(AggregationOverrides.class);
    }
}
