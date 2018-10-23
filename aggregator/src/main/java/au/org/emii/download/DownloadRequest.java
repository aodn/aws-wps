package au.org.emii.download;

import java.net.URL;

/**
 * Requested download metadata
 */
public class DownloadRequest {
    private final URL url;
    private final long size;

    public DownloadRequest(URL url, long size) {
        this.url = url;
        this.size = size;
    }

    public URL getUrl() {
        return url;
    }

    public long getSize() {
        return size;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadRequest that = (DownloadRequest) o;

        if (size != that.size) return false;
        return url != null ? url.equals(that.url) : that.url == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (int) (size ^ (size >>> 32));
        return result;
    }
}
