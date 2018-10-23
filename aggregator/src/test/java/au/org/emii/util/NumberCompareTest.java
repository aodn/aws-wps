package au.org.emii.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NumberCompareTest {

    @Test
    public void testCompare() {

        Boolean res = NumberCompare.equalsWithinDelta(30.002, 30,  0.01);
        assertEquals(true, res);

        res = NumberCompare.equalsWithinDelta(-30.02, -30,  0.000001);
        assertEquals(false, res);
    }
}
