package au.org.emii.aggregator.overrides.xstream;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by craigj on 1/03/17.
 */
public class CollectionReader {
    public static <T> List<T> readCollection(HierarchicalStreamReader reader,
                                             UnmarshallingContext context, String itemName, Class<T> clazz) {
        List<T> result = new ArrayList<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (!reader.getNodeName().equals(itemName)) {
                throw new ConversionException("Expected " + itemName + " tag but found " + reader.getNodeName());
            }

            result.add((T) context.convertAnother(null, clazz));
            reader.moveUp();
        }

        return result;
    }


}
