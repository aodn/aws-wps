package au.org.emii.download;

import java.net.URL;
import java.nio.file.Path;

/**
 * File download metadata
 */
public class Download {
    private final URL url;
    private final Path path;
    private long size;

    public Download(URL url, Path path, long size) {
        this.url = url;
        this.path = path;
        this.size = size;
    }

    public Path getPath() {
        return path;
    }

    public URL getURL() {
        return url;
    }

    public long getSize() {
        return size;
    }
}
