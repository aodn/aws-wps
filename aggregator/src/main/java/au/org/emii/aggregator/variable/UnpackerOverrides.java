package au.org.emii.aggregator.variable;

import ucar.ma2.DataType;

import java.util.Arrays;

/**
 * Optional overrides for values to be used in the unpacked variable
 */
public class UnpackerOverrides {
    private final Number newFillerValue;
    private final Number[] newValidRange;
    private final Number newValidMin;
    private final Number newValidMax;
    private final Number[] newMissingValues;
    private final DataType newDataType;

    private UnpackerOverrides(Builder builder) {
        this.newFillerValue = builder.newFillerValue;
        this.newValidRange = builder.newValidRange == null ? null :
            Arrays.copyOf(builder.newValidRange, builder.newValidRange.length);
        this.newValidMin = builder.newValidMin;
        this.newValidMax = builder.newValidMax;
        this.newMissingValues = builder.newMissingValues == null ? null :
            Arrays.copyOf(builder.newMissingValues, builder.newMissingValues.length);
        this.newDataType = builder.newDataType;
    }

    public Number getNewFillerValue() {
        return newFillerValue;
    }

    public Number[] getNewValidRange() {
        return newValidRange == null ? null :Arrays.copyOf(newValidRange, newValidRange.length);
    }

    public Number getNewValidMin() {
        return newValidMin;
    }

    public Number getNewValidMax() {
        return newValidMax;
    }

    public Number[] getNewMissingValues() {
        return newMissingValues == null ? null : Arrays.copyOf(newMissingValues, newMissingValues.length);
    }

    public DataType getNewDataType() {
        return newDataType;
    }

    public static class Builder {
        private Number newFillerValue;
        private Number[] newValidRange;
        private Number newValidMin;
        private Number newValidMax;
        private Number[] newMissingValues;
        private DataType newDataType;

        public Builder newFillerValue(Number newFillerValue) {
            this.newFillerValue = newFillerValue;
            return this;
        }

        public Builder newValidRange(Number[] newValidRange) {
            if (newValidRange != null && newValidRange.length != 2) {
                throw new IllegalArgumentException("Expected two values - minimum/maximum");
            }

            this.newValidRange = newValidRange;
            return this;
        }

        public Builder newValidMin(Number newValidMin) {
            if (newValidMax != null && newValidMin != null && newValidMin.doubleValue() > newValidMax.doubleValue()) {
                throw new IllegalArgumentException(
                    String.format("New valid minimum %f > new valid maximum %f", newValidMin, newValidMax));
            }

            this.newValidMin = newValidMin;
            return this;
        }

        public Builder newValidMax(Number newValidMax) {
            if (newValidMin != null && newValidMax != null && newValidMax.doubleValue() < newValidMin.doubleValue()) {
                throw new IllegalArgumentException(
                    String.format("New valid maximum %f < new valid minimum %f", newValidMax, newValidMin));
            }

            this.newValidMax = newValidMax;
            return this;
        }

        public Builder newMissingValues(Number[] newMissingValues) {
            this.newMissingValues = newMissingValues;
            return this;
        }

        public Builder newDataType(DataType newDataType) {
            this.newDataType = newDataType;
            return this;
        }

        public UnpackerOverrides build() {
            return new UnpackerOverrides(this);
        }
    }
}

