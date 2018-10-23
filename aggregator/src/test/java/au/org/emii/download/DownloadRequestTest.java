package au.org.emii.download;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * DownloadRequest unit tests
 */
public class DownloadRequestTest {
    @Test
    public void testNoDuplicatesInLinkedHashSet() throws MalformedURLException {
        Set<DownloadRequest> downloads = new LinkedHashSet<>();

        downloads.add(new DownloadRequest(new URL("http://example.com/downloads/file1.text"), 5325326L));
        downloads.add(new DownloadRequest(new URL("http://example.com/downloads/file2.text"), 532532L));
        downloads.add(new DownloadRequest(new URL("http://example.com/downloads/file1.text"), 5325326L));

        assertArrayEquals(new DownloadRequest[]{
                new DownloadRequest(new URL("http://example.com/downloads/file1.text"), 5325326L),
                new DownloadRequest(new URL("http://example.com/downloads/file2.text"), 532532L)
            }, downloads.toArray()
        );
    }

}

