package au.org.emii.aggregator.overrides.xstream;

import au.org.emii.aggregator.overrides.VariableAttributeOverride;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import ucar.ma2.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by craigj on 28/02/17.
 */
public class VariableAttributeOverrideConverter extends DeserialisingOnlyConverter {
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext unmarshallingContext) {
        String name = null;
        String type = null;
        String value = null;

        for (int i=0; i < reader.getAttributeCount(); i++) {
            if (reader.getAttributeName(i).equals("name")) {
                name = reader.getAttribute(i);
            } else if (reader.getAttributeName(i).equals("type")) {
                type = reader.getAttribute(i);
            } else if (reader.getAttributeName(i).equals("value")) {
                value = reader.getAttribute(i);
            } else {
                throw new ConversionException(String.format("Unexpected attribute %s found for attribute",
                    reader.getAttributeName(i)));
            }
        }

        List<String> values = new ArrayList<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();


            values.add(reader.getValue());
            reader.moveUp();
        }

        if (value != null && values.size() > 0) {
            throw new ConversionException("Value elements and value attribute cannot both be specified on an " +
                "attribute element");
        }

        if (value != null) {
            values.add(value); // its a short cut way of specifying one value only
        }

        return new VariableAttributeOverride(name, DataType.getType(type), values);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(VariableAttributeOverride.class);
    }
}
