package au.org.emii.aggregator.variable;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * SubsettedVariable tests
 */
public class SubsettedVariableTest {

    NetcdfVariable variable;

    @Before
    public void createTestVariable() {
        int[] storage = new int[500];

        for (int i=0; i<storage.length; i++) {
            storage[i] = i + 1;
        }

        Array data = Array.factory(DataType.INT, new int[] {10, 5, 10}, storage);

        variable = new TestVariable("temp", new String[] {"time", "lat", "lon"}, data, 16L);
    }

    @Test
    public void testSubsetAllDimensions() throws InvalidRangeException, IOException {
        Map<String, Range> subset = new LinkedHashMap<>();
        subset.put("time", new Range(2, 3));
        subset.put("lat", new Range(1, 2));
        subset.put("lon", new Range(4, 6));

        SubsettedVariable subsettedVariable = new SubsettedVariable(variable, subset);

        Array result = subsettedVariable.read();

        int[] expected = new int[] {
            115, 116, 117,
            125, 126, 127,

            165, 166, 167,
            175, 176, 177
        };

        assertArrayEquals(expected, (int[])result.get1DJavaArray(int.class));
    }

    @Test
    public void testSubsetSingleDimension() throws InvalidRangeException, IOException {
        Map<String, Range> subset = new LinkedHashMap<>();
        subset.put("lat", new Range(1, 1));

        SubsettedVariable subsettedVariable = new SubsettedVariable(variable, subset);

        Array result = subsettedVariable.read();

        int[] expected = new int[] {
             11,  12,  13,  14,  15,  16,  17,  18,  19,  20,
             61,  62,  63,  64,  65,  66,  67,  68,  69,  70,
            111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
            161, 162, 163, 164, 165, 166, 167, 168, 169, 170,
            211, 212, 213, 214, 215, 216, 217, 218, 219, 220,
            261, 262, 263, 264, 265, 266, 267, 268, 269, 270,
            311, 312, 313, 314, 315, 316, 317, 318, 319, 320,
            361, 362, 363, 364, 365, 366, 367, 368, 369, 370,
            411, 412, 413, 414, 415, 416, 417, 418, 419, 420,
            461, 462, 463, 464, 465, 466, 467, 468, 469, 470
        };

        assertArrayEquals(expected, (int[])result.get1DJavaArray(int.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubsetOutOfRange() throws InvalidRangeException, IOException {
        Map<String, Range> subset = new LinkedHashMap<>();
        subset.put("lat", new Range(1, 2));
        subset.put("lon", new Range(10, 13));

        new SubsettedVariable(variable, subset);
    }

    @Test(expected = InvalidRangeException.class)
    public void testOriginOutOfRange() throws InvalidRangeException, IOException {
        Map<String, Range> subset = new LinkedHashMap<>();
        subset.put("lat", new Range(1, 2));
        subset.put("lon", new Range(5, 9));

        SubsettedVariable subsettedVariable = new SubsettedVariable(variable, subset);

        subsettedVariable.read(new int[] {0, 0, 6}, new int[] {1, 1, 1});
    }

    @Test(expected = InvalidRangeException.class)
    public void testShapeOutOfRange() throws InvalidRangeException, IOException {
        Map<String, Range> subset = new LinkedHashMap<>();
        subset.put("lat", new Range(1, 2));
        subset.put("lon", new Range(5, 9));

        SubsettedVariable subsettedVariable = new SubsettedVariable(variable, subset);

        subsettedVariable.read(new int[] {0, 0, 0}, new int[] {0, 6, 1});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOriginRankMismatch() throws InvalidRangeException, IOException {
        Map<String, Range> subset = new LinkedHashMap<>();
        subset.put("lat", new Range(1, 2));
        subset.put("lon", new Range(5, 9));

        SubsettedVariable subsettedVariable = new SubsettedVariable(variable, subset);

        subsettedVariable.read(new int[] {0, 0}, new int[] {0, 2, 1});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShapeRankMismatch() throws InvalidRangeException, IOException {
        Map<String, Range> subset = new LinkedHashMap<>();
        subset.put("lat", new Range(1, 2));
        subset.put("lon", new Range(5, 9));

        SubsettedVariable subsettedVariable = new SubsettedVariable(variable, subset);

        subsettedVariable.read(new int[] {0, 0, 0}, new int[] {0, 2});
    }

}