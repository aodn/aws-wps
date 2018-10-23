package au.org.emii.aggregator.dataset;

import au.org.emii.aggregator.coordsystem.LatLonCoords;
import au.org.emii.aggregator.coordsystem.TimeAxis;
import au.org.emii.aggregator.coordsystem.XYRanges;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.variable.AbstractVariable.NumericValue;
import au.org.emii.aggregator.variable.NetcdfVariable;
import au.org.emii.aggregator.variable.SubsettedVariable;
import au.org.emii.util.NumberCompare;
import au.org.emii.util.NumberRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
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
    private static final double DELTA = 0.0000001;
    private final List<Attribute> globalAttributes;
    private final List<Dimension> dimensions;
    private final List<NetcdfVariable> variables;
    private static final Logger logger = LoggerFactory.getLogger(SubsettedDataset.class);

    public SubsettedDataset(NetcdfDatasetIF dataset, CalendarDateRange timeRange, NumberRange verticalSubset,
                            LatLonRect bbox) throws AggregationException {
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
            int startIndex = 0, endIndex = 0, i = 0;

            for (NumericValue value: verticalAxis.getNumericValues()) {
                if (NumberCompare.equalsWithinDelta(value.getValue(), verticalSubset.getMin(), DELTA)) { startIndex = i; }
                if (NumberCompare.equalsWithinDelta(value.getValue(), verticalSubset.getMax(), DELTA)) { endIndex = i; }
                i++;
            }

            try {
                Range indexedSubset = new Range(startIndex, endIndex);
                subsettedDimensions.put(verticalAxis.getDimensions().get(0).getShortName(), indexedSubset);
            } catch (InvalidRangeException e) {
                logger.error("Invalid vertical subset " + e.getMessage() + " . Continuing with null vertical subset.");
            }
        }

        // Add x/y dimension subsets for this coordinate system as applicable

        if (bbox != null) {
            LatLonCoords coords = dataset.getLatLonCoords();
            XYRanges xyRanges = coords.subset(bbox);
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
