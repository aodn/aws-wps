package au.org.emii.aggregator.overrides;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import ucar.ma2.DataType;

/**
 * Created by craigj on 1/03/17.
 */
public class GlobalAttributeOverrideConverter extends DeserialisingOnlyConverter {
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String name = reader.getAttribute("name");
        DataType type = reader.getAttribute("type") != null ? DataType.getType(reader.getAttribute("type")) : null;
        String match = reader.getAttribute("match");
        String value = reader.getAttribute("value");
        return new GlobalAttributeOverride(name, type, match, value);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(GlobalAttributeOverride.class);
    }
}
