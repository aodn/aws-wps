package au.org.emii.aggregator.converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NetCDFConverter extends Converter {

    final static String MIME_TYPE = "application/x-netcdf";
    private final static String EXTENSION = "nc";

    @Override
    public void convert(Path outputFile, Path convertedFile) throws Exception {
        try {
            // No conversion necessary
            Files.move(outputFile, convertedFile);
        } catch (IOException e) {
            throw new Exception("Could create output file", e);
        }
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public String getExtension() {
        return EXTENSION;
    }

}
