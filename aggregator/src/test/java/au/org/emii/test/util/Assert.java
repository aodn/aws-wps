package au.org.emii.test.util;

import org.apache.commons.io.FileUtils;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * Created by craigj on 15/02/17.
 */
public class Assert {
    public static void assertNetcdfFilesEqual(Path expected, Path actual) throws IOException {
        String expectedCdl = getAsCdl(expected);
        String actualCdl = getAsCdl(actual);
        assertEquals(expectedCdl, actualCdl);
    }

    public static void assertNetcdfFileEqualsCdl(Path expected, Path actual) throws IOException {
        String expectedCdl = FileUtils.readFileToString(expected.toFile());
        String actualCdl = getAsCdl(actual);
        assertEquals(expectedCdl, actualCdl);
    }

    private static String getAsCdl(Path netcdfFile) throws IOException {
        NetcdfFile expectedFile = NetcdfFile.open(netcdfFile.toAbsolutePath().toString());
        StringWriter outputWriter = new StringWriter();
        NCdumpW.print(expectedFile, outputWriter, NCdumpW.WantValues.all, false, false, null, null);
        String cdl = outputWriter.toString().replaceFirst("netcdf .* ", "netcdf "); // return cdl minus filename
        return removeNCPropertiesGlobalAttribute(cdl);
    }

    /**
     * NetCDF Library version 4.4.1 has added provenance information
     * to files created.This information consists of a persistent attribute
     * named _NCProperties plus two computed attributes, _IsNetcdf4 and
     * _SuperblockVersion. We are removing the _NCProperties global variable
     * to keep the tests running on 4.4.1 or later versions of NetCDF Library.
     *
     * @param cdl
     * @return
     */
    private static String removeNCPropertiesGlobalAttribute(String cdl) {
        int startIndex, endIndex;

        startIndex = cdl.indexOf(":_NCProperties");
        if (startIndex != -1) {
            endIndex = cdl.substring(startIndex, cdl.length()).indexOf("\n");
            if (endIndex != -1) {
                String ncPropertiesLine = cdl.substring(startIndex, startIndex + endIndex + 3);
                return cdl.replace(ncPropertiesLine, "");
            }
        }

        return cdl;
    }
}
