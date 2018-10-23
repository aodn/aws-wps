package au.org.emii.test.util;

import au.org.emii.aggregator.dataset.NetcdfDatasetAdapter;
import au.org.emii.aggregator.variable.UnpackerOverrides;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Testing resource utility methods
 */
public class Resource {

    public static Path resourcePath(String resource) {
        try {
            URL url = Resource.class.getResource("/"+resource);
            return java.nio.file.Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError("Unexpected error opening test resource", e);
        }
    }

    public static NetcdfDatasetAdapter openNetcdfDataset(String resource) throws IOException {
        String testFile = resourcePath(resource).toString();
        return NetcdfDatasetAdapter.open(testFile, new HashMap<String, UnpackerOverrides>());
    }

}
