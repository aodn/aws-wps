package au.org.emii.aggregator.coordsystem;

import static ucar.unidata.geoloc.LatLonPointImpl.lonNormal;

public class LongitudeRange implements CoordRange {
    private final double westBL;
    private final double eastBL;
    private final boolean crossesDateline;

    LongitudeRange(double westBL, double eastBL) {
        this.westBL = lonNormal(westBL);
        this.eastBL = lonNormal(eastBL);
        this.crossesDateline = this.westBL > this.eastBL;
    }

    @Override
    public boolean contains(double value) {
        double longitude = lonNormal(value);

        if (crossesDateline) {
            // bounding box crosses the +/- 180 seam
            return (longitude >= westBL) || (longitude <= eastBL);
        } else {
            // check "normal" lon case
            return (longitude >= westBL) && (longitude <= eastBL);
        }
    }
}
