package au.org.emii.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File downloader
 */
public class Downloader {
    private final int connectTimeOut;
    private final int readTimeOut;

    public Downloader(int connectTimeOut, int readTimeOut) {
        this.connectTimeOut = connectTimeOut;
        this.readTimeOut = readTimeOut;
    }

    public void download(URL url, Path path) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(connectTimeOut);
        connection.setReadTimeout(readTimeOut);

        try (InputStream source = connection.getInputStream()) {
            Files.copy(source, path);
        } catch (IOException e) {
            deleteIfExists(path);
            throw e;
        }
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unexpected system error attempting to delete %s", path), e);
        }
    }
}
