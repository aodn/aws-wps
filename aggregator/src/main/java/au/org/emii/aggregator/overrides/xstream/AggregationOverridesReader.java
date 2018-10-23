package au.org.emii.aggregator.overrides.xstream;

import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import com.thoughtworks.xstream.XStream;

import java.nio.file.Path;

/**
 * Class to read aggregation config
 */
public class AggregationOverridesReader {
    public static AggregationOverrides load(Path location) {
        XStream xStream = new XStream();
        xStream.alias("template", AggregationOverrides.class);
        xStream.registerConverter(new AggregationOverridesConverter());
        xStream.alias("attribute", GlobalAttributeOverride.class);
        xStream.registerConverter(new GlobalAttributeOverrideConverter());
        xStream.registerConverter(new GlobalAttributeOverridesConverter());
        xStream.registerConverter(new VariableOverridesConverter());
        xStream.registerConverter(new VariableAttributeOverrideConverter());
        return (AggregationOverrides) xStream.fromXML(location.toFile());
    }
}
