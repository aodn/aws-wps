package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.variable.AbstractVariable.NumericValue;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.unidata.geoloc.LatLonRect;

/* Independent 1 dimensional latitude/longitude subset operations */

class LatLon1D extends LatLonCoords {
    LatLon1D(NetcdfVariable latitude, NetcdfVariable longitude) {
        super(latitude, longitude);
    }

    @Override
    public XYRanges getNearestXYPoint(double longitudeRequested, double latitudeRequested) {
        try {
            int lastNearestX = -1; // last x index nearest to minimum lat/lon
            int lastNearestY = -1; // last y index nearest to minimum lat/lon

            double lastNearestDistance = Double.MAX_VALUE;

            for (NumericValue longitudeValue: longitude.getNumericValues()) {
                for (NumericValue latitudeValue: latitude.getNumericValues()) {
                    double minLatLonDistance = distance(latitudeValue.getValue().doubleValue(), latitudeRequested,
                        longitudeValue.getValue().doubleValue(), longitudeRequested);

                    if (minLatLonDistance < lastNearestDistance) {
                        lastNearestX = longitudeValue.getIndex()[0];
                        lastNearestY = latitudeValue.getIndex()[0];
                        lastNearestDistance = minLatLonDistance;
                    }
                }
            }

            return new XYRanges(new Range(lastNearestX, lastNearestX), new Range(lastNearestY, lastNearestY));
        } catch (InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public XYRanges subsetGrid(LatLonRect bboxRequested) throws AggregationException {
        try {
            CoordRange longitudeRange = new LongitudeRange(bboxRequested.getLonMin(), bboxRequested.getLonMax());
            CoordRange latitudeRange = new LatitudeRange(bboxRequested.getLatMin(), bboxRequested.getLatMax());

            Range xRange = getIndexRange(longitude, longitudeRange);
            Range yRange = getIndexRange(latitude, latitudeRange);

            if (xRange == null || yRange == null) {
                throw new AggregationException("Bounding box selected no data");
            }

            return new XYRanges(xRange, yRange);
        } catch (InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    private Range getIndexRange(NetcdfVariable variable, CoordRange range) throws InvalidRangeException {
        int minIdx = Integer.MAX_VALUE; // minimum index within coordinate range
        int maxIdx = Integer.MIN_VALUE; // maximum index within coordinate range

        for (NumericValue numericValue: variable.getNumericValues()) {
            double value = numericValue.getValue().doubleValue();

            if (range.contains(value)) {
                int idx = numericValue.getIndex()[0];
                if (idx < minIdx) minIdx = idx;
                if (idx > maxIdx) maxIdx = idx;
            }
        }

        return minIdx == Integer.MAX_VALUE ? null : new Range(minIdx, maxIdx);
    }

}
