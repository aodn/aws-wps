package au.org.aodn.aws.wps.operation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Utils {

    public static String inputStreamToString(InputStream inStream) throws IOException
    {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result.toString(StandardCharsets.UTF_8.name());
    }
}