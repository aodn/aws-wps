package au.org.emii.aggregator.coordsystem;

import static ucar.unidata.geoloc.LatLonPointImpl.latNormal;

public class LatitudeRange implements CoordRange {
    private final double southBL;
    private final double northBL;

    LatitudeRange(double southBL, double northBL) {
        this.southBL = latNormal(southBL);
        this.northBL = latNormal(northBL);
    }

    @Override
    public boolean contains(double value) {
        double latitude = latNormal(value);
        return latitude >= southBL && latitude <= northBL;
    }
}

