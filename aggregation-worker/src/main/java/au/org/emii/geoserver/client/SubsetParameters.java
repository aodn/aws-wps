package au.org.emii.geoserver.client;

import au.org.emii.util.NumberRange;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import java.text.SimpleDateFormat;

public class SubsetParameters {
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String TIME = "TIME";
    public static final String DEPTH = "DEPTH";
    private final LatLonRect bbox;
    private final CalendarDateRange timeRange;
    private final NumberRange verticalRange;

    public SubsetParameters(LatLonRect bbox, CalendarDateRange timeRange, NumberRange verticalRange) {
        this.bbox = bbox;
        this.timeRange = timeRange;
        this.verticalRange = verticalRange;
    }

    public LatLonRect getBbox() {
        return bbox;
    }

    public boolean isPointSubset() {
        return bbox.getLowerLeftPoint().equals(bbox.getUpperRightPoint());
    }

    public CalendarDateRange getTimeRange() {
        return timeRange;
    }

    public NumberRange getVerticalRange() {
        return verticalRange;
    }

    public static SubsetParameters parse(String subset) {

        Double latMin, latMax, lonMin, lonMax;
        NumberRange depthRange = null;
        Map<String, ParameterRange> subsets = new HashMap<>();
        String latLonErrorMsg = String.format("Invalid latitude/longitude format for subset: %s Valid latitude/longitude format example: LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219", subset);
        String timeErrorMsg = String.format("Invalid time format for subset: %s Valid time format example: TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z", subset);
        String verticalSubsetErrorMsg = String.format("Invalid z-dimension format for subset: %s Valid time format example: DEPTH,0.0,100.0", subset);
        String subsetErrorMsg = String.format("Invalid format for subset: %s Valid format example: TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219;DEPTH,0.0,100.0", subset);

        // Parse
        for (String part : subset.split(";")) {
            String[] subsetParts = part.split(",");
            try {
                subsets.put(subsetParts[0], new ParameterRange(subsetParts[1], subsetParts[2]));
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException(String.format("%s error: Invalid subset parameter '%s'", subsetErrorMsg, part));
            }
        }

        for (String key: subsets.keySet()) {
            if (!key.equals(LATITUDE) && !key.equals(LONGITUDE) && !key.equals(TIME) && !key.equals(DEPTH)) {
                throw new RuntimeException(String.format("%s error: Invalid subset parameter '%s'", subsetErrorMsg, key));
            }
        }

        ParameterRange latitudeRange = subsets.get(LATITUDE);
        ParameterRange longitudeRange = subsets.get(LONGITUDE);

        if (latitudeRange == null || longitudeRange == null) {
            throw new RuntimeException(latLonErrorMsg);
        }

        try {
            latMin = Double.parseDouble(latitudeRange.start);
            latMax = Double.parseDouble(latitudeRange.end);
            lonMin = Double.parseDouble(longitudeRange.start);
            lonMax = Double.parseDouble(longitudeRange.end);
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format("%s error: '%s'", latLonErrorMsg, e.getMessage()));
        }

        LatLonRect bbox = new LatLonRect(new LatLonPointImpl(latMin, lonMin), new LatLonPointImpl(latMax, lonMax));

        ParameterRange timeRange = subsets.get(TIME);
        CalendarDateRange calendarDateRange = null;

        // Time Validation
        if (timeRange != null) {
            try {
                CalendarDate startTime = CalendarDate.parseISOformat("gregorian", timeRange.start);
                CalendarDate endTime = CalendarDate.parseISOformat("gregorian", timeRange.end);
                calendarDateRange = CalendarDateRange.of(startTime, endTime);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(String.format("%s error: '%s'", timeErrorMsg, e.getMessage()));
            }
        }

        ParameterRange verticalRange = subsets.get(DEPTH);
        try {
            if (verticalRange != null) {
                depthRange = new NumberRange(verticalRange.start, verticalRange.end);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format("%s error: '%s'", verticalSubsetErrorMsg, e));
        }

        return new SubsetParameters(bbox, calendarDateRange, depthRange);
    }

    public String portalFormatSpatial() {

        if (this.bbox != null) {
            String minLon = String.valueOf(this.bbox.getLonMin());
            String minLat = String.valueOf(this.bbox.getLatMin());
            String maxLon = String.valueOf(this.bbox.getLonMax());
            String maxLat = String.valueOf(this.bbox.getLatMax());

            String spatial = "";

            if (minLon.equals(maxLon) && minLat.equals(maxLat)) {
                spatial = spatial.concat("Timeseries at Lat/Lon: " + minLat + ',' + minLon);
            } else {
                spatial = spatial.concat("Spatial: " + minLon + ',' + minLat + ',' + maxLon + ',' + maxLat);
            }

            return spatial;
        }

        return null;
    }

    private String formatDate(Date date) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MMM-dd-HH:mm-'UTC'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    public String portalFormatTemoral() {
        if (this.timeRange != null) {
            String startDate = formatDate(this.timeRange.getStart().toDate());
            String endDate = formatDate(this.timeRange.getEnd().toDate());

            return "Temporal: " + startDate + " to " + endDate;
        }

        return null;
    }

    public String portalFormatDepth() {

        if (this.verticalRange != null) {
            return "Depth: " + this.verticalRange.getMin() + "," + this.verticalRange.getMax();
        }
        return null;
    }

    public static class ParameterRange {
        String start;
        String end;

        ParameterRange(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
}
