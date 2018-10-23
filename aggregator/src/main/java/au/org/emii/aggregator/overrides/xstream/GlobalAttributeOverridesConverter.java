package au.org.emii.aggregator.overrides.xstream;

import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.overrides.GlobalAttributeOverrides;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import java.util.ArrayList;
import java.util.List;

public class GlobalAttributeOverridesConverter extends DeserialisingOnlyConverter {
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        List<String> removeAttributes = new ArrayList<>();
        List<GlobalAttributeOverride> addOrReplaceAttributes = new ArrayList<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (reader.getNodeName().equals("attribute")) {
                addOrReplaceAttributes.add((GlobalAttributeOverride)context.convertAnother(null, GlobalAttributeOverride.class));
            } else if (reader.getNodeName().equals("remove")) {
                removeAttributes.add(reader.getAttribute("name"));
            } else {
                throw new ConversionException("Unexpected element found " + reader.getNodeName());
            }

            reader.moveUp();
        }

        return new GlobalAttributeOverrides(removeAttributes, addOrReplaceAttributes);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(GlobalAttributeOverrides.class);
    }
}
