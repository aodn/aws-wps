package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.exception.AggregationException;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import java.util.List;


/**
 * Operations on a list of calendar dates
 */
public class CalendarDates {
    private final List<CalendarDate> dates;

    public CalendarDates(List<CalendarDate> dates) {
        this.dates = dates;
    }

    public Range subset(CalendarDateRange dateRange) throws AggregationException {
        try {
            int minIndex = Integer.MAX_VALUE;
            int maxIndex = Integer.MIN_VALUE;

            for (int i = 0; i<dates.size(); i++) {
                CalendarDate date = dates.get(i);

                if (dateRange.includes(date)) {
                    if (i < minIndex) minIndex = i;
                    if (i > maxIndex) maxIndex = i;
                }
            }

            if (minIndex == Integer.MAX_VALUE) {
                throw new AggregationException("No data for " + dateRange.toString());
            }

            return new Range(minIndex, maxIndex);
        } catch (InvalidRangeException e) {
            throw new RuntimeException(e);  // Shouldn't be possible
        }
    }

}
