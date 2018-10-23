package au.org.emii.aggregator.overrides.xstream;

import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.overrides.VariableAttributeOverride;
import au.org.emii.aggregator.overrides.VariableOverrides;
import org.junit.Test;
import ucar.ma2.DataType;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static au.org.emii.test.util.Resource.resourcePath;

/**
 * Test loading of aggregation template overrides from file
 */
public class AggregationOverridesReaderTest {
    Object[][] expectedAttributes = {
        {"start_time", DataType.STRING, null, "${TIME_START}"},
        {"title", DataType.STRING, ".*", "${0}, ${TIME_START}, ${TIME_END}"}
    };

    Object[][] expectedVariables = {
        {"time", null, 0, new String[] {}},
        {"sea_surface_temperature", DataType.FLOAT, 3, new String[] {
            "name=_FillValue type=float values=9.96920996838687e+36",
            "name=valid_min type=float values=0.0",
            "name=valid_max type=float values=350.0"
        }},
        {"sses_bias", DataType.FLOAT, 1, new String[] {
            "name=valid_range type=float values=0.0,350.0"
        }}
    };

    @Test
    public void testLoad() {
        AggregationOverrides result = AggregationOverridesReader.load(resourcePath("au/org/emii/aggregator/overrides/template.xml"));

        List<GlobalAttributeOverride> attributes = result.getAttributeOverrides().getAddOrReplaceAttributes();

        assertEquals(2, attributes.size());

        for (int i=0; i<attributes.size(); i++) {
            GlobalAttributeOverride attribute = attributes.get(i);
            assertEquals(expectedAttributes[i][0], attribute.getName());
            assertEquals(expectedAttributes[i][1], attribute.getType());
            assertEquals(expectedAttributes[i][2], toString(attribute.getPattern()));
            assertEquals(expectedAttributes[i][3], attribute.getValue());
        }

        List<VariableOverrides> variableOverridesList = result.getVariableOverridesList();

        assertEquals(3, variableOverridesList.size());

        for (int i = 0; i< variableOverridesList.size(); i++) {
            VariableOverrides variableOverrides = variableOverridesList.get(i);
            assertEquals(expectedVariables[i][0], variableOverrides.getName());
            assertEquals(expectedVariables[i][1], variableOverrides.getType());

            List<VariableAttributeOverride> vAttributes = variableOverrides.getAttributes();

            assertEquals(expectedVariables[i][2], vAttributes.size());

            String[] expectedVAttributes = (String[])expectedVariables[i][3];
            String[] actualVAttributes = new String[vAttributes.size()];

            for (int j = 0; j< variableOverrides.getAttributes().size(); j++) {
                actualVAttributes[j] = vAttributes.get(j).toString();
            }

            assertArrayEquals(expectedVAttributes, actualVAttributes);
        }
    }

    private String toString(Pattern pattern) {
        return pattern!=null?pattern.pattern():null;
    }
}
