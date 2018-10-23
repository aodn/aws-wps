package au.org.emii.aggregator.overrides.xstream;

import au.org.emii.aggregator.overrides.VariableAttributeOverride;
import au.org.emii.aggregator.overrides.VariableOverrides;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ucar.ma2.DataType;

import java.util.List;

/**
 * Created by craigj on 1/03/17.
 */
public class VariableOverridesConverter implements Converter {
    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
        throw new UnsupportedOperationException("Marshalling Variable objects not supported");
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String name = reader.getAttribute("name");
        DataType type = reader.getAttribute("type") != null ? DataType.getType(reader.getAttribute("type")) : null;
        List<VariableAttributeOverride> attributes = CollectionReader.readCollection(reader, context, "attribute", VariableAttributeOverride.class);
        return new VariableOverrides(name, type, attributes);
    }

    @Override
    public boolean canConvert(Class aClass) {
        return aClass.equals(VariableOverrides.class);
    }
}
