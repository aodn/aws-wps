package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.variable.NetcdfVariable;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxisTimeHelper;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by craigj on 22/02/17.
 */
public class TimeAxis {

    private final NetcdfVariable variable;
    private final CalendarDates calendarDates;

    public TimeAxis(NetcdfVariable variable) {
        if (variable.getRank() != 1) {
            throw new UnsupportedOperationException("Only 1D time axis supported");
        }

        this.variable = variable;
        this.calendarDates = new CalendarDates(getCalendarDates(variable));
    }

    public Range getSubsetRange(CalendarDateRange timeRange) throws AggregationException {
        return calendarDates.subset(timeRange);
    }

    private List<CalendarDate> getCalendarDates(NetcdfVariable variable) {
        try {
            CoordinateAxisTimeHelper helper= new CoordinateAxisTimeHelper(getCalendar(variable), getUnitsString(variable));
            List<CalendarDate> calendarDates = new ArrayList<>();
            Array data = variable.read();

            for (int i = 0; i < data.getSize(); i++) {
                calendarDates.add( helper.makeCalendarDateFromOffset(data.getDouble(i)));
            }

            return calendarDates;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Calendar getCalendar(NetcdfVariable variable) {
        Attribute calendarAttribute = variable.findAttribute(CF.CALENDAR);
        String calendar = (calendarAttribute == null) ? null : calendarAttribute.getStringValue();
        return Calendar.get(calendar);
    }

    public String getUnitsString(NetcdfVariable variable) {
        Attribute attribute = variable.findAttribute(CDM.UNITS);
        return attribute == null || !attribute.isString() ? "" : attribute.getStringValue();
    }

    public String getDimensionName() {
        return variable.getDimensions().get(0).getShortName();
    }

    public int getSize() {
        return variable.getShape()[0];
    }
}
