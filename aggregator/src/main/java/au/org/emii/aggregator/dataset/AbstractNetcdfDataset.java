package au.org.emii.aggregator.dataset;

import au.org.emii.aggregator.coordsystem.LatLonCoords;
import au.org.emii.aggregator.coordsystem.TimeAxis;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.util.NumberRange;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Common code for implementations of NetcdfDatasetIF
 */
public abstract class AbstractNetcdfDataset implements NetcdfDatasetIF {
    @Override
    public NetcdfVariable findVariable(String shortName) {
        for (NetcdfVariable variable: getVariables()) {
            if (variable.getShortName().equals(shortName)) {
                return variable;
            }
        }

        return null;
    }

    @Override
    public TimeAxis getTimeAxis() {
        NetcdfVariable time = findVariable(AxisType.Time);

        if (time == null) {
            return null;
        }

        return new TimeAxis(time);
    }

    @Override
    public String getTimeDimension() {
        TimeAxis time = getTimeAxis();

        if (time == null) {
            return null;
        }

        return time.getDimensionName();
    }

    @Override
    public boolean hasVerticalAxis() {
        return getVerticalAxis() != null;
    }

    @Override
    public NetcdfVariable getVerticalAxis() {
        return findVariable(AxisType.Height);
    }

    @Override
    public LatLonCoords getLatLonCoords() {
        NetcdfVariable latitudeVariable = findVariable(AxisType.Lat);
        NetcdfVariable longitudeVariable = findVariable(AxisType.Lon);

        if (latitudeVariable == null || longitudeVariable == null) {
            throw new UnsupportedOperationException("One or both of longitude/latitude axes not found");
        }

        return LatLonCoords.getInstance(latitudeVariable, longitudeVariable);
    }

    @Override
    public SubsettedDataset subset(CalendarDateRange timeRange, NumberRange verticalSubset,
                                         LatLonRect bbox) throws AggregationException {
        return new SubsettedDataset(this, timeRange, verticalSubset, bbox);
    }

    @Override
    public boolean hasRecordVariables() {
        return getTimeAxis() != null;
    }

    @Override
    public LatLonRect getBbox() {
        NetcdfVariable latitude = findVariable(AxisType.Lat);
        NetcdfVariable longitude = findVariable(AxisType.Lon);

        if (latitude == null || longitude == null) {
            throw new UnsupportedOperationException("Dataset has no latitude and/or longitude axes/axis");
        }

        NumberRange longitudeBounds = longitude.getBounds();
        NumberRange latitudeBounds = latitude.getBounds();

        return new LatLonRect(new LatLonPointImpl(latitudeBounds.getMin().doubleValue(), longitudeBounds.getMin().doubleValue()),
            new LatLonPointImpl(latitudeBounds.getMax().doubleValue(), longitudeBounds.getMax().doubleValue()));
    }


    private NetcdfVariable findVariable(AxisType type) {
        for (NetcdfVariable variable: getVariables()) {
            if (variable.getAxisType() != null && variable.getAxisType().equals(type)) {
                return variable;
            }
        }

        return null;
    }

}
