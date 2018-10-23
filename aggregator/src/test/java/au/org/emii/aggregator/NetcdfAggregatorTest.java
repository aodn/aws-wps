package au.org.emii.aggregator;

import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.xstream.AggregationOverridesReader;
import au.org.emii.util.NumberRange;
import com.sun.jna.Native;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.jni.netcdf.Nc4prototypes;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImmutable;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static au.org.emii.test.util.Assert.assertNetcdfFileEqualsCdl;
import static au.org.emii.test.util.Assert.assertNetcdfFilesEqual;
import static au.org.emii.test.util.Resource.resourcePath;

/**
 * NetcdfAggregator tests for different types of collections
 */
public class NetcdfAggregatorTest {
    private static final Logger logger = LoggerFactory.getLogger(NetcdfAggregatorTest.class);
    private Path outputFile;
    private static String libraryVersion;

    @BeforeClass
    public static void getLibNetcdfVersion() {
        // The netcdf C library can change the order of variable attributes when writing them to disk
        // and the order it uses can be different between different versions of the library.
        // Get the version of the library being used so we can adjust expectations accordingly.
        // Note: The beta version of netcdf java includes a method on Nc4Iosp which can do this
        // but we can't use that yet.
        Nc4prototypes nc4 = (Nc4prototypes) Native.loadLibrary("netcdf", Nc4prototypes.class);
        libraryVersion = nc4.nc_inq_libvers();
        logger.info("Using netcdf C library version {}", libraryVersion);
    }

    @Before
    public void createOutputFile() throws IOException {
        outputFile = Files.createTempFile("output", "nc");
    }

    @Test
    public void testSpatialSubsetSingleFile() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-33.0, 113.9), new LatLonPointImmutable(-32.0, 114.9));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-1.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/single-expected.nc"), outputFile);
    }

    @Test
    public void testSpatialSubsetMultipleFile() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-33.0, 113.9), new LatLonPointImmutable(-32.0, 114.9));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
            outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/multiple-expected.nc"), outputFile);
    }

    @Test
    public void testAggregateOverHttp() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-33.0, 113.9), new LatLonPointImmutable(-32.0, 114.9));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
            outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add("http://data.aodn.org.au/IMOS/ACORN/gridded_1h-avg-current-map_non-QC/ROT/2017/12/06/IMOS_ACORN_V_20171206T173000Z_ROT_FV00_1-hour-avg.nc");
            netcdfAggregator.add("http://data.aodn.org.au/IMOS/ACORN/gridded_1h-avg-current-map_non-QC/ROT/2017/12/06/IMOS_ACORN_V_20171206T183000Z_ROT_FV00_1-hour-avg.nc");
        }

        assertNetcdfFileEqualsCdl(resourcePath("au/org/emii/aggregator/over-http-expected.cdl"), outputFile);
    }

    @Test
    public void testSpatialSubsetMultipleFileUnpack() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-30.68, 97.82), new LatLonPointImmutable(-30.64, 97.86));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/srs-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/srs-2.nc"));
        }

        if (libraryVersion.startsWith("4.1.3")) {
            // sses_standard_deviation attribute ordering slightly different with this version - result is still OK.
            assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/unpack-expected-4.1.3.nc"), outputFile);
        }
        else {
            assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/unpack-expected.nc"), outputFile);
        }
    }

    @Test(expected = AggregationException.class)
    public void testSpatialSubsetNoData() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-20.0, 113.9), new LatLonPointImmutable(-18.0, 114.9));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/multiple-expected.nc"), outputFile);
    }

    @Test
    public void testPointSubsetInBbox() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-32.8, 114.0), new LatLonPointImmutable(-32.8, 114.0));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/point-subset-inside.nc"), outputFile);
    }

    @Test
    public void testPointSubsetOutsideBbox() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-20.0, 113.0), new LatLonPointImmutable(-20.0, 113.0));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/acorn-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/point-subset-outside.nc"), outputFile);
    }

    @Test
    public void testNonArrayDepthVariable() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-20.0, 113.0), new LatLonPointImmutable(-20.0, 113.0));
        AggregationOverrides overrides = AggregationOverridesReader.load(
                resourcePath("au/org/emii/aggregator/non-array-depth.xml"));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, overrides, null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/non-array-depth-variable.nc"));
        }

        assertNetcdfFileEqualsCdl(resourcePath("au/org/emii/aggregator/non-array-depth-expected.cdl"), outputFile);
    }

    @Test
    public void testTemporalSubset() throws IOException, AggregationException {
        CalendarDateRange dateRange = CalendarDateRange.of(
                CalendarDate.parseISOformat("gregorian", "2009-05-07"), CalendarDate.parseISOformat("gregorian", "2009-05-22"));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, null, null, dateRange
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/cars.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/temporal-expected.nc"), outputFile);
    }

    @Test
    public void testLatLonProjectionSubset() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-31.0, 113.0), new LatLonPointImmutable(-30.0, 114.0));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/projection-expected.nc"), outputFile);
    }

    @Test
    public void testLatLonProjectionPointSubsetWithin() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-31.0, 113.0), new LatLonPointImmutable(-31.0, 113.0));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/projection-point-within-expected.nc"), outputFile);
    }

    @Test
    public void testLatLonProjectionPointSubsetOutside() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-10.0, 114.0), new LatLonPointImmutable(-10.0, 114.0));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/projection-point-outside-expected.nc"), outputFile);
    }

    @Test(expected = AggregationException.class)
    public void testLatLonProjectionSpatialSubsetNoData() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-10.0, 114.0), new LatLonPointImmutable(-12.0, 114.0));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/projection-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/projection-point-outside-expected.nc"), outputFile);
    }

    @Test
    public void testAggregationOverrides() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-30.68, 97.82), new LatLonPointImmutable(-30.64, 97.86));
        CalendarDateRange timeRange = CalendarDateRange.of(CalendarDate.parseISOformat("gregorian", "2017-02-01T03:19:60"),
                CalendarDate.parseISOformat("gregorian", "2017-02-02T03:19:60"));
        AggregationOverrides overrides = AggregationOverridesReader.load(
                resourcePath("au/org/emii/aggregator/template.xml"));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, overrides, null, bbox, null, timeRange
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/srs-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/srs-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/overrides-expected.nc"), outputFile);
    }

    @Test
    public void testGsla() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-41.5, 83.7), new LatLonPointImmutable(-41.1, 84.1));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/gsla0.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/gsla1.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/gsla-expected.nc"), outputFile);
    }

    @Test
    public void testSrsOcJohnson() throws IOException, AggregationException {
        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, null, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/srs-oc-1.nc"));
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/srs-oc-2.nc"));
        }

        assertNetcdfFilesEqual(resourcePath("au/org/emii/aggregator/srs-oc-expected.nc"), outputFile);
    }

    @Test
    public void testCarsMonthly() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-90.0, 179.5), new LatLonPointImmutable(90, -179.5));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/cars-monthly.nc"));
        }

        assertNetcdfFileEqualsCdl(resourcePath("au/org/emii/aggregator/cars-monthly-expected.cdl"), outputFile);
    }

    @Test
    public void testCarsDepth() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-44.5, 114), new LatLonPointImmutable(-44.5, 114));
        NumberRange verticalSubset = new NumberRange(50,100);
        AggregationOverrides overrides = AggregationOverridesReader.load(
                resourcePath("au/org/emii/aggregator/CARStemplate.xml"));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, overrides, null, bbox, verticalSubset, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/CARSWeeklySubset.nc"));
        }

        assertNetcdfFileEqualsCdl(resourcePath("au/org/emii/aggregator/cars_weekly_expected.cdl"), outputFile);
    }

    @Test
    public void testBathymetry() throws IOException, AggregationException {
        LatLonRect bbox = new LatLonRect(new LatLonPointImmutable(-90.0, -125.862299280124), new LatLonPointImmutable(90, -125.862292710516));

        try (NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                outputFile, new AggregationOverrides(), null, bbox, null, null
        )) {
            netcdfAggregator.add(resourcePath("au/org/emii/aggregator/bathymetry.nc"));
        }

        assertNetcdfFileEqualsCdl(resourcePath("au/org/emii/aggregator/bathymetry-expected.cdl"), outputFile);
    }

    @After
    public void deleteOutputFile() throws IOException {
        Files.deleteIfExists(outputFile);
    }

}