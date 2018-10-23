package au.org.emii.aggregator.variable;

import au.org.emii.aggregator.datatype.NumericTypes;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static au.org.emii.aggregator.attribute.Attributes.VALID_MAX;
import static au.org.emii.aggregator.attribute.Attributes.VALID_MIN;

/**
 * Class that can be used to unpack a variable/change its type
 *
 * Allows default filler value, missing values, valid range, valid min and valid max values to be used in the
 * converted variable to be overridden as required.
 */

public class UnpackedVariable extends AbstractVariable {

    private final NetcdfVariable variable;
    private final Number scale;
    private final Number offset;
    private final Number[] newValidRange;
    private final Number newValidMin;
    private final Number newValidMax;
    private final Number oldFillerValue;
    private final Number[] oldMissingValues;
    private final Number newFillerValue;
    private final Number[] newMissingValues;
    private final DataType newDataType;
    private final boolean isUnsigned;

    public UnpackedVariable(NetcdfVariable variable) {
        this(variable, null);
    }

    public UnpackedVariable(NetcdfVariable variable, UnpackerOverrides overrides) {
        this.variable = variable;

        // Use defaults if no overrides specified

        if (overrides == null) {
            overrides = new UnpackerOverrides.Builder().build();
        }

        // get scale factor to use

        Attribute scaleAttribute = variable.findAttribute(CDM.SCALE_FACTOR);

        if (scaleAttribute != null && !scaleAttribute.isString()) {
            scale = scaleAttribute.getNumericValue();
        } else {
            scale = null;
        }

        // get offset to add

        Attribute offsetAttribute = variable.findAttribute(CDM.ADD_OFFSET);

        if (offsetAttribute != null && !offsetAttribute.isString()) {
            offset = offsetAttribute.getNumericValue();
        } else {
            offset = null;
        }

        // store for performance reasons

        isUnsigned = variable.isUnsigned();  // applyScaleOffset is 10 times slower otherwise

        // get data type to use

        if (overrides.getNewDataType() != null) {
            newDataType = overrides.getNewDataType();
        } else if (scale != null) {
            newDataType = DataType.getType(scale.getClass());
        } else if (offset != null) {
            newDataType = DataType.getType(offset.getClass());
        } else {
            newDataType = variable.getDataType();
        }

        // get valid range to use

        Attribute validRangeAtt = variable.findAttribute(CDM.VALID_RANGE);

        if (overrides.getNewValidRange() != null) {
            newValidRange = new Number[2];
            newValidRange[0] = NumericTypes.valueOf(overrides.getNewValidRange()[0], newDataType);
            newValidRange[1] = NumericTypes.valueOf(overrides.getNewValidRange()[1], newDataType);
        } else if (validRangeAtt == null || validRangeAtt.isString() || validRangeAtt.getLength() < 2) {
            newValidRange = null;
        } else {
            newValidRange = new Number[2];
            newValidRange[0] = applyScaleOffset(validRangeAtt.getNumericValue(0));
            newValidRange[1] = applyScaleOffset(validRangeAtt.getNumericValue(1));
        }

        // get valid min to use

        Attribute validMinAttribute = variable.findAttribute(VALID_MIN);

        if (overrides.getNewValidMin() != null) {
            newValidMin = NumericTypes.valueOf(overrides.getNewValidMin(), newDataType);
        } else if (validMinAttribute == null || validMinAttribute.isString()) {
            newValidMin = null;
        } else {
            newValidMin = applyScaleOffset(validMinAttribute.getNumericValue());
        }

        // get valid max to use

        Attribute validMaxAttribute = variable.findAttribute(VALID_MAX);

        if (overrides.getNewValidMax() != null) {
            newValidMax = NumericTypes.valueOf(overrides.getNewValidMax(), newDataType);
        } else if (validMaxAttribute == null || validMaxAttribute.isString()) {
            newValidMax = null;
        } else {
            newValidMax = applyScaleOffset(validMaxAttribute.getNumericValue());
        }

        // get old fill value used

        Attribute fillValueAttribute = variable.findAttribute(CDM.FILL_VALUE);

        if (fillValueAttribute == null || fillValueAttribute.isString()) {
            oldFillerValue = null;
        } else {
            oldFillerValue = fillValueAttribute.getNumericValue();
        }

        // get new fill value to use

        if (oldFillerValue == null) {
            newFillerValue = null;
        } else if (overrides.getNewFillerValue() != null) {
            newFillerValue = NumericTypes.valueOf(overrides.getNewFillerValue(), newDataType);
        } else if (scale == null && offset == null && newDataType.equals(variable.getDataType())) {
            newFillerValue = oldFillerValue;
        } else if (NumericTypes.isDefaultFillValue(oldFillerValue, variable.getDataType())) {
            newFillerValue = NumericTypes.defaultFillValue(newDataType);
        } else {
            newFillerValue = applyScaleOffset(oldFillerValue);
        }

        // get old missing value used

        Attribute missingValueAttribute = variable.findAttribute(CDM.MISSING_VALUE);

        if (missingValueAttribute == null || missingValueAttribute.isString()) {
            oldMissingValues = null;
        } else {
            oldMissingValues = getNumericValues(missingValueAttribute);
        }

        // get new missing value to use

        if (oldMissingValues == null) {
            newMissingValues = null;
        } else if (overrides.getNewMissingValues() != null) {
            newMissingValues = NumericTypes.valueOf(overrides.getNewMissingValues(), newDataType);
        } else {
            newMissingValues = applyScaleOffset(oldMissingValues);
        }
    }

    private Number[] getNumericValues(Attribute attribute) {
        Number[] result = new Number[attribute.getLength()];

        for (int i=0; i<result.length; i++) {
            result[i] = attribute.getNumericValue(i);
        }

        return result;
    }

    @Override
    public String getShortName() {
        return variable.getShortName();
    }

    @Override
    public DataType getDataType() {
        return newDataType;
    }

    @Override
    public AxisType getAxisType() {
        return variable.getAxisType();
    }

    @Override
    public boolean isUnlimited() {
        return false;
    }

    @Override
    public Array read(int[] origin, int[] slice) throws IOException, InvalidRangeException {
        Array data = variable.read(origin, slice);

        if (conversionRequired()) {
            return convert(data);
        } else {
            return data;
        }
    }

    @Override
    public boolean isUnsigned() {
        return (scale != null || offset != null) ? false : variable.isUnsigned();
    }

    @Override
    public List<Dimension> getDimensions() {
        return variable.getDimensions();
    }

    @Override
    public List<Attribute> getAttributes() {
        List<Attribute> attributes = new ArrayList<Attribute>();

        for (Attribute attribute: variable.getAttributes()) {
            if (attribute.getShortName().equals(CDM.SCALE_FACTOR) || attribute.getShortName().equals(CDM.ADD_OFFSET)) {
                // ignore scale/offset - the variable has been unpacked
            } else if (attribute.getShortName().equals(CDM.FILL_VALUE) && newFillerValue != null) {
                attributes.add(new Attribute(CDM.FILL_VALUE, newFillerValue));
            } else if (attribute.getShortName().equals(CDM.VALID_RANGE) && newValidRange != null) {
                attributes.add(new Attribute(CDM.VALID_RANGE, getArray(newValidRange)));
            } else if (attribute.getShortName().equals(VALID_MIN) && newValidMin != null) {
                attributes.add(new Attribute(VALID_MIN, newValidMin));
            } else if (attribute.getShortName().equals(VALID_MAX) && newValidMax != null) {
                attributes.add(new Attribute(VALID_MAX, newValidMax));
            } else if (attribute.getShortName().equals(CDM.MISSING_VALUE) && newMissingValues != null) {
                attributes.add(new Attribute(CDM.MISSING_VALUE, getArray(newMissingValues)));
            } else {
                attributes.add(attribute);
            }
        }

        return attributes;
    }

    @Override
    public long getMaxChunkSize() {
        return variable.getMaxChunkSize();
    }

    // Access to derived metadata for testing purposes

    public Number getFillerValue() {
        return newFillerValue;
    }

    public Number getValidMin() {
        return newValidMin;
    }

    public Number getValidMax() {
        return newValidMax;
    }

    public Number[] getValidRange() {
        return newValidRange;
    }

    public Number[] getMissingValues() {
        return newMissingValues;
    }

    // Private methods

    private boolean conversionRequired() {
        return scale != null || offset != null || isFillerModified() || isMissingValuesModified()
            || isDataTypeModified();
    }

    private boolean isMissingValuesModified() {
        return newMissingValues != null && oldMissingValues != null && !Arrays.equals(newMissingValues, oldMissingValues);
    }

    private boolean isFillerModified() {
        return newFillerValue != null && newFillerValue !=  null && !oldFillerValue.equals(newFillerValue);
    }

    private boolean isDataTypeModified() {
        return newDataType != null && !newDataType.equals(variable.getDataType());
    }

    private Number[] applyScaleOffset(Number[] oldMissingValues) {
        Number[] result = new Number[oldMissingValues.length];

        for (int i=0; i <oldMissingValues.length; i++) {
            result[i] = applyScaleOffset(oldMissingValues[i]);
        }

        return result;
    }

    private Array convert(Array data) {
        Array result = Array.factory(newDataType, data.getShape());

        for (int i=0; i<data.getSize(); i++) {
            final Number value = (Number) data.getObject(i);
            result.setObject(i, convert(value));
        }

        return result;
    }

    private Number convert(Number value) {
        if (oldFillerValue != null && value.equals(oldFillerValue)) {
            return newFillerValue;
        } else if (oldMissingValues != null && isMissingValue(value)) {
            return newMissingValue(value);
        } else {
            return applyScaleOffset(value);
        }
    }

    private Number newMissingValue(Number value) {
        for (int i=0; i<oldMissingValues.length && i<newMissingValues.length; i++) {
            if (value.equals(oldMissingValues[i])) {
                return newMissingValues[i];
            }
        }

        return newMissingValues[0];
    }

    private boolean isMissingValue(Number value) {
        for (int i=0; i< oldMissingValues.length; i++) {
            if (value.equals(oldMissingValues[i])) {
                return true;
            }
        }

        return false;
    }

    private Number applyScaleOffset(Number value) {
        Number result = value;

        if (scale != null || offset != null) {
            double effectiveScale = scale != null ? scale.doubleValue() : 1.0;
            double effectiveOffset = offset != null ? offset.doubleValue() : 0.0;

            if (isUnsigned && value instanceof Byte)
                result = effectiveScale * DataType.unsignedByteToShort((Byte)value) + effectiveOffset;
            else if (isUnsigned && value instanceof Short)
                result = effectiveScale * DataType.unsignedShortToInt((Short)value) + effectiveOffset;
            else if (isUnsigned && value instanceof Integer)
                result = effectiveScale * DataType.unsignedIntToLong((Integer)value) + effectiveOffset;
            else {
                result = effectiveScale * value.doubleValue() + effectiveOffset;
            }
        }

        return NumericTypes.valueOf(result, newDataType);
    }

    private Array getArray(Number[] values) {
        Array result = Array.factory(newDataType, new int[] {values.length});

        for (int i=0; i<values.length; i++) {
            result.setObject(i, values[i]);
        }

        return result;
    }

}
