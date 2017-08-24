package au.org.emii.aggregator.dataset;

import au.org.emii.aggregator.coordsystem.LatLonCoords;
import au.org.emii.aggregator.coordsystem.LatLonCoords.XYRanges;
import au.org.emii.aggregator.coordsystem.TimeAxis;
import au.org.emii.aggregator.dataset.NetcdfDatasetIF;
import au.org.emii.aggregator.exception.SubsetException;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.variable.SubsettedVariable;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Subsetted dataset
 */
public class SubsettedDataset extends AbstractNetcdfDataset {
    private final List<Attribute> globalAttributes;
    private final List<Dimension> dimensions;
    private final List<NetcdfVariable> variables;

    public SubsettedDataset(NetcdfDatasetIF dataset, CalendarDateRange timeRange, Range verticalSubset,
                            LatLonRect bbox) throws SubsetException {
        // just copy global attributes

        this.globalAttributes = dataset.getGlobalAttributes();

        // Determine subsetted dimension ranges

        Map<String, Range> subsettedDimensions = new LinkedHashMap<>();

        if (timeRange != null) {
            TimeAxis timeAxis = dataset.getTimeAxis();
            Range tRange = timeAxis.getSubsetRange(timeRange);
            subsettedDimensions.put(timeAxis.getDimensionName(), tRange);
        }

        // Add z dimension subset for this coordinate system if applicable

        if (verticalSubset != null && dataset.hasVerticalAxis()) {
            NetcdfVariable verticalAxis = dataset.getVerticalAxis();

            if (verticalAxis.getShape()[0] <= verticalSubset.last()) {
                throw new SubsetException(
                    String.format("Vertical subset outside of Z axis %s range", verticalAxis.getShortName()));
            }

            subsettedDimensions.put(verticalAxis.getDimensions().get(0).getShortName(), verticalSubset);
        }

        // Add x/y dimension subsets for this coordinate system as applicable

        if (bbox != null) {
            LatLonCoords coords = dataset.getLatLonCoords();
            XYRanges xyRanges = coords.getXYRanges(bbox);
            subsettedDimensions.put(coords.getXDimensionName(), xyRanges.getXRange());
            subsettedDimensions.put(coords.getYDimensionName(), xyRanges.getYRange());
        }


        // Build subsetted dimension list

        List<Dimension> dimensions = new ArrayList<>();

        for (Dimension dimension: dataset.getDimensions()) {
            Range subset = subsettedDimensions.get(dimension.getShortName());
            int length = subset != null ? subset.length() : dimension.getLength();
            dimensions.add(new Dimension(dimension.getShortName(), length, dimension.isShared(),
                dimension.isUnlimited(), dimension.isVariableLength()));
        }

        this.dimensions = dimensions;

        // Build subsetted variables list

        List<NetcdfVariable> variables = new ArrayList<>();

        for (NetcdfVariable variable: dataset.getVariables()) {
            variables.add(new SubsettedVariable(variable, subsettedDimensions));
        }

        this.variables = variables;
    }

    @Override
    public List<Attribute> getGlobalAttributes() {
        return globalAttributes;
    }

    @Override
    public List<Dimension> getDimensions() {
        return dimensions;
    }

    @Override
    public List<NetcdfVariable> getVariables() {
        return variables;
    }

}
