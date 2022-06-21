package au.org.aodn.aws.wps;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;

public interface Storage<T> {

    FilterInputStream getObjectStream(String path, String name) throws IOException;
    String readObjectAsString(String path, String name) throws IOException;

    T getObject(String path, String name);

    void uploadToTarget(File file, String path, String name, String contentType) throws InterruptedException, IOException;

    void uploadToTarget(String document, String path, String name, String contentType) throws IOException;

    int getExpirationinDays(String path);
}
