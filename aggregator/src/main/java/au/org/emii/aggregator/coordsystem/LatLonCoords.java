package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.unidata.geoloc.LatLonRect;

import java.util.Arrays;

/**
 * Operations on latitude/longitude coordinates
 */
public abstract class LatLonCoords {
    protected final NetcdfVariable latitude;
    protected final NetcdfVariable longitude;

    protected LatLonCoords(NetcdfVariable latitude, NetcdfVariable longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

    }

    public static LatLonCoords getInstance(NetcdfVariable latitude, NetcdfVariable longitude) {
        if (latitude.getRank()==1 && longitude.getRank()==1) {
            return new LatLon1D(latitude, longitude);
        } else if (longitude.getRank()==2 && Arrays.equals(latitude.getShape(), longitude.getShape())) {
            return new LatLon2D(latitude, longitude);
        } else {
            throw new UnsupportedOperationException("Only independent 1 dimensional or dependent 2 dimensional lat/lon supported");
        }
    }

    public XYRanges subset(LatLonRect bbox) throws AggregationException {
        boolean pointSubset = bbox.getLowerLeftPoint().equals(bbox.getUpperRightPoint());

        if (pointSubset) {
            return getNearestXYPoint(bbox.getLonMin(), bbox.getLatMin());
        } else {
            return subsetGrid(bbox);
        }
    }

    protected abstract XYRanges getNearestXYPoint(double lonMin, double latMin) throws AggregationException;

    protected abstract XYRanges subsetGrid(LatLonRect bbox) throws AggregationException;

    public String getXDimensionName() {
        return longitude.getDimensions().get(0).getShortName();
    }

    public String getYDimensionName() {
        if (latitude.getRank() == 2) {
            return latitude.getDimensions().get(1).getShortName();
        } else {
            return latitude.getDimensions().get(0).getShortName();
        }
    }

    /**
     * From: http://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
     * Calculate distance between two points in latitude and longitude.
     * Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point
     * @returns Distance in Meters
     */
    static double distance(double lat1, double lat2, double lon1,
                           double lon2) {
        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }
}
