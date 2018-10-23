package au.org.emii.aggregator.coordsystem;

import au.org.emii.aggregator.exception.AggregationException;
import org.junit.Test;
import ucar.ma2.Range;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import java.util.Arrays;

import static org.junit.Assert.*;
import static ucar.nc2.time.CalendarDate.parseISOformat;

/**
 * Created by craigj on 17/02/17.
 */
public class CalendarDatesTest {
    private CalendarDates ascendingDates = new CalendarDates(Arrays.asList(
        toDate("2005-06-07"), toDate("2005-06-09"), toDate("2005-06-11"), toDate("2005-06-13")));

    private CalendarDates descendingDates = new CalendarDates(Arrays.asList(
        toDate("2005-06-13"), toDate("2005-06-11"), toDate("2005-06-09"), toDate("2005-06-07")));

    @Test
    public void testSubsetReturned() throws AggregationException {
        Range subset1 = ascendingDates.subset(dateRange("2005-06-08", "2005-06-12"));
        assertEquals(1, subset1.first());
        assertEquals(2, subset1.last());

        Range subset2 = descendingDates.subset(dateRange("2005-06-08", "2005-06-12"));
        assertEquals(1, subset2.first());
        assertEquals(2, subset2.last());

        Range subset3 = ascendingDates.subset(dateRange("2004-06-09", "2005-06-11"));
        assertEquals(0, subset3.first());
        assertEquals(2, subset3.last());

        Range subset4 = descendingDates.subset(dateRange("2004-06-09", "2005-06-11"));
        assertEquals(1, subset4.first());
        assertEquals(3, subset4.last());
    }

    @Test(expected = AggregationException.class)
    public void testNoDatesSelectedAscending() throws AggregationException {
        ascendingDates.subset(dateRange("2004-06-09", "2005-05-11"));
    }

    @Test(expected = AggregationException.class)
    public void testNoDatesSelectedDescending() throws AggregationException {
        descendingDates.subset(dateRange("2004-06-09", "2005-05-11"));
    }

    private CalendarDateRange dateRange(String start, String end) {
        return dateRange(toDate(start), toDate(end));
    }

    private CalendarDateRange dateRange(CalendarDate start, CalendarDate end) {
        return CalendarDateRange.of(start, end);
    }

    private CalendarDate toDate(String date) {
        return parseISOformat("gregorian", date);
    }

}