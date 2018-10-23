package au.org.emii.aggregator.variable;

import au.org.emii.aggregator.variable.AbstractVariable.NumericValue;
import au.org.emii.util.NumberRange;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class AbstractVariableTest {
    @Test
    public void testNumericValueIterator() {
        Array latitudeData = Array.factory(DataType.DOUBLE, new int[]{10}, new double[]{
            2.0, 3.0, -1.0, 4.0, 5.0, 2.5, 3.5, 4.5, -0.5, 0.0
        });

        NetcdfVariable latitude = new TestVariable("latitude", new String[] {"latitude"}, latitudeData, 16L);

        ArrayList<Integer> indexes = new ArrayList<>();
        ArrayList<Number> values = new ArrayList<>();

        for (NumericValue numericValue: latitude.getNumericValues()) {
            assertEquals(1, numericValue.getIndex().length);
            indexes.add(numericValue.getIndex()[0]);
            values.add(numericValue.getValue());
        }

        Integer[] expectedIndexes = new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        Double[] expectedValues = new Double[] {2.0, 3.0, -1.0, 4.0, 5.0, 2.5, 3.5, 4.5, -0.5, 0.0};

        assertTrue(Arrays.equals(expectedIndexes, indexes.toArray()));
        assertTrue(Arrays.equals(expectedValues, values.toArray()));
    }

    @Test
    public void testBounds1DVariable() {
        Array data = Array.factory(DataType.DOUBLE, new int[]{10}, new double[]{
            2.0, 3.0, -1.0, 4.0, 5.0, 2.5, 3.5, 4.5, -0.5, 0.0
        });

        NetcdfVariable testVariable = new TestVariable("test", new String[] {"lat"}, data, 16L);

        NumberRange bounds = testVariable.getBounds();

        assertEquals(-1.0, bounds.getMin().doubleValue(), Double.MIN_VALUE);
        assertEquals(5.0, bounds.getMax().doubleValue(), Double.MIN_VALUE);
    }

    @Test
    public void testBounds2DVariable() {
        Array data = Array.factory(DataType.DOUBLE, new int[]{2, 5}, new double[]{
            2.0, 3.0, -1.0, 4.0, 5.0, 2.5, 3.5, 4.5, -0.5, 0.0
        });

        NetcdfVariable testVariable = new TestVariable("test", new String[] {"i", "j"}, data, 16L);

        NumberRange bounds = testVariable.getBounds();

        assertEquals(-1.0, bounds.getMin().doubleValue(), Double.MIN_VALUE);
        assertEquals(5.0, bounds.getMax().doubleValue(), Double.MIN_VALUE);
    }

}