package au.org.emii.aggregator.dataset;

import au.org.emii.aggregator.coordsystem.LatLonCoords;
import au.org.emii.aggregator.coordsystem.TimeAxis;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.dataset.SubsettedDataset;
import au.org.emii.aggregator.exception.SubsetException;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDateRange;
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
            throw new UnsupportedOperationException("No variable with axis type time found in dataset");
        }

        return new TimeAxis(time);
    }

    @Override
    public boolean hasVerticalAxis() {
        return getVerticalAxis() != null;
    }

    @Override
    public NetcdfVariable getVerticalAxis() {
        return findVariable(AxisType.GeoZ);
    }

    @Override
    public LatLonCoords getLatLonCoords() {
        NetcdfVariable latitudeVariable = findVariable(AxisType.Lat);
        NetcdfVariable longitudeVariable = findVariable(AxisType.Lon);

        if (latitudeVariable == null || longitudeVariable == null) {
            throw new UnsupportedOperationException("One or both of longitude/latitude axes not found");
        }

        return new LatLonCoords(latitudeVariable, longitudeVariable);
    }

    @Override
    public SubsettedDataset subset(CalendarDateRange timeRange, Range verticalSubset,
                                   LatLonRect bbox) throws SubsetException {
        return new SubsettedDataset(this, timeRange, verticalSubset, bbox);
    }

    @Override
    public LatLonRect getBbox() {
        return getLatLonCoords().getBbox();
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
