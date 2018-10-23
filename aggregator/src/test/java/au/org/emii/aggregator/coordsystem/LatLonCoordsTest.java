package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.coordsystem.LatLon2D.LatLonValue;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.variable.TestVariable;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class LatLonCoordsTest {
    @Test
    public void testLatLonValues2D() {
        Array latitudeData = Array.factory(DataType.DOUBLE, new int[]{2, 5}, new double[]{
            2.0, 3.0, -1.0, 4.0, 5.0, 2.5, 3.5, 4.5, -0.5, 0.0
        });

        Array longitudeData = Array.factory(DataType.DOUBLE, new int[]{2, 5}, new double[]{
            120.0, 123.4, 115.5, 116.5, 117.5, 135.1, 140.5, 111.6, 118.0, 119.0
        });

        NetcdfVariable latitude = new TestVariable("latitude", new String[] {"i", "j"}, latitudeData, 16L);
        NetcdfVariable longitude = new TestVariable("longitude", new String[] {"i", "j"}, longitudeData, 16L);

        LatLon2D latLonCoords = new LatLon2D(latitude, longitude);

        double[][] returnedValues = new double[10][4];
        int index = 0;

        for (LatLonValue latLonValue : latLonCoords.getLatLonValues()) {
            returnedValues[index][0] = latLonValue.getX();
            returnedValues[index][1] = latLonValue.getY();
            returnedValues[index][2] = latLonValue.getLat();
            returnedValues[index][3] = latLonValue.getLon();
            index++;
        }

        double[][] expectedValues = new double[][] {
            {0, 0, 2.0, 120.0},
            {0, 1, 3.0, 123.4},
            {0, 2, -1.0, 115.5},
            {0, 3, 4.0, 116.5},
            {0, 4, 5.0, 117.5},
            {1, 0, 2.5, 135.1},
            {1, 1, 3.5, 140.5},
            {1, 2, 4.5, 111.6},
            {1, 3, -0.5, 118.0},
            {1, 4, 0.0, 119.0}
        };

        assertTrue(equal(expectedValues, returnedValues));
    }


    private static boolean equal(final double[][] arr1, final double[][] arr2) {

        if (arr1 == null) {
            return (arr2 == null);
        }

        if (arr2 == null) {
            return false;
        }

        if (arr1.length != arr2.length) {
            return false;
        }

        for (int i = 0; i < arr1.length; i++) {
            if (!Arrays.equals(arr1[i], arr2[i])) {
                return false;
            }
        }

        return true;
    }
}
