package au.org.emii.aggregator.coordsystem;

import ucar.ma2.Range;

public class XYRanges {
    Range xRange;
    Range yRange;

    XYRanges(Range xRange, Range yRange) {
        this.xRange = xRange;
        this.yRange = yRange;
    }

    public Range getXRange() {
        return xRange;
    }

    public Range getYRange() {
        return yRange;
    }
}
