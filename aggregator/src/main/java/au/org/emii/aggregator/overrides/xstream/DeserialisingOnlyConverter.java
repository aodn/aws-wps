package au.org.emii.aggregator.overrides.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Base class for converters supporting deserialisation only
 */
public abstract class DeserialisingOnlyConverter implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext context) {
        throw new UnsupportedOperationException("Serialisation not supported");
    }
}
