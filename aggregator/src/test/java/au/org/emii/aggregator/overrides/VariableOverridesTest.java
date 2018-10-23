package au.org.emii.aggregator.overrides;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.constants.CDM;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ankit on 31/05/17.
 */
public class VariableOverridesTest {

    private static final String FILL_VALUE = "0.0";
    private static final String VALID_RANGE1 = "0.0";
    private static final String VALID_RANGE2 = "350.0";
    private static final String SSES_BIAS = "sses_bias";

    private List<String> fillValues;
    private List<String> validRangeValues;
    private VariableOverrides ssesBias;

    @Before
    public void setup () {
        fillValues = new ArrayList<>();
        fillValues.add(FILL_VALUE);

        validRangeValues = new ArrayList<>();
        validRangeValues.add(VALID_RANGE1);
        validRangeValues.add(VALID_RANGE2);
    }

    @Test
    public void testAttributeOverridesWithDataTypeAndSingleValue() {

        List<VariableAttributeOverride> attributeOverrides = new ArrayList<>();
        attributeOverrides.add(new VariableAttributeOverride(CDM.FILL_VALUE, DataType.FLOAT, fillValues));

        ssesBias = new VariableOverrides("sses_bias", DataType.FLOAT, attributeOverrides);

        Assert.assertNotNull(ssesBias.getFillerValue());
        Assert.assertTrue(ssesBias.getFillerValue().toString().equals(FILL_VALUE));
    }

    @Test
    public void testAttributeOverridesWithDataTypeAndMultipleValues() {

        List<VariableAttributeOverride> attributeOverrides = new ArrayList<>();
        attributeOverrides.add(new VariableAttributeOverride(CDM.VALID_RANGE, DataType.FLOAT, validRangeValues));

        ssesBias = new VariableOverrides("sses_bias", DataType.FLOAT, attributeOverrides);

        Assert.assertNotNull(ssesBias.getValidRange());

        for(Number missingValue : ssesBias.getValidRange()) {
            Assert.assertTrue(missingValue.toString().equals(VALID_RANGE1)
                    || missingValue.toString().equals(VALID_RANGE2));
        }
    }

    @Test
    public void testAttributeOverridesWithoutDataTypeAndSingleValue() {

        List<VariableAttributeOverride> attributeOverrides = new ArrayList<>();
        attributeOverrides.add(new VariableAttributeOverride(CDM.FILL_VALUE, null, fillValues));

        ssesBias = new VariableOverrides(SSES_BIAS, DataType.FLOAT, attributeOverrides);

        Assert.assertNotNull(ssesBias.getFillerValue());
        Assert.assertTrue(ssesBias.getFillerValue().toString().equals(FILL_VALUE));
    }

    @Test
    public void testAttributeOverridesWithoutDataTypeAndMultipleValues() {

        List<VariableAttributeOverride> attributeOverrides = new ArrayList<>();
        attributeOverrides.add(new VariableAttributeOverride(CDM.VALID_RANGE, null, validRangeValues));

        ssesBias = new VariableOverrides("sses_bias", DataType.FLOAT, attributeOverrides);

        Assert.assertNotNull(ssesBias.getValidRange());

        for(Number missingValue : ssesBias.getValidRange()) {
            Assert.assertTrue(missingValue.toString().equals(VALID_RANGE1)
                    || missingValue.toString().equals(VALID_RANGE2));
        }
    }
}
