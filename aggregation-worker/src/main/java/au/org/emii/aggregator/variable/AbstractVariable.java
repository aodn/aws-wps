package au.org.emii.aggregator.variable;

import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;

import java.io.IOException;
import java.util.List;

/**
 * Common code for implementations of NetcdfVariable
 */
public abstract class AbstractVariable implements NetcdfVariable {
    private Double min;
    private Double max;

    @Override
    public long getSize() {
        long size = 0;

        for (Dimension dimension: getDimensions()) {
            size+=dimension.getLength();
        }

        return size;
    }

    @Override
    public int getElementSize() {
        return getDataType().getSize();
    }

    @Override
    public int[] getShape() {
        List<Dimension> dimensions = getDimensions();

        int[] result = new int[dimensions.size()];

        for (int i=0 ; i < dimensions.size(); i++) {
            result[i] = dimensions.get(i).getLength();
        }

        return result;
    }

    @Override
    public int getRank() {
        return getShape().length;
    }

    @Override
    public Attribute findAttribute(String attName) {
        for (Attribute attribute: getAttributes()) {
            if (attribute.getShortName().equals(attName)) {
                return attribute;
            }
        }

        return null;
    }

    @Override
    public int findDimensionIndex(String shortName) {
        int i = 0;

        for (Dimension dimension: getDimensions()) {
            if (dimension.getShortName().equals(shortName)) {
                return i;
            }

            i++;
        }

        return -1;
    }

    @Override
    public Dimension findDimension(String shortName) {
        int dimensionIndex = findDimensionIndex(shortName);
        return dimensionIndex != -1 ? getDimensions().get(dimensionIndex) : null;
    }

    @Override
    public Array read() throws IOException {
        try {
            int[] origin = new int[getRank()];
            return read(origin, getShape());
        } catch (InvalidRangeException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    public double getMin() {
        if (min == null) {
            calculateMinMax();
        }

        return min;
    }

    @Override
    public double getMax() {
        if (max == null) {
            calculateMinMax();
        }

        return max;
    }

    private void calculateMinMax() {
        try {
            Array values = read();

            double min = Double.MAX_VALUE;
            double max = Double.MAX_VALUE * -1;

            for (int i=0; i<values.getSize(); i++) {
                double value = values.getDouble(i);
                if (value < min) min = value;
                if (value > max) max = value;
            }

            this.min = min;
            this.max = max;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isUnlimited() {
        for (Dimension dimension: getDimensions()) {
            if (dimension.isUnlimited()) {
                return true;
            }
        }

        return false;
    }
}
