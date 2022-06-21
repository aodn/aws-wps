package au.org.emii.download;

import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File downloader
 */
public class Downloader {

    protected RestTemplate template;

    public Downloader(RestTemplate template) {
        this.template = template;
    }

    public void download(URL url, Path path) throws IOException {
        // Don't need to delete temp file as it is running in docker and delete when exit
        File download = template.execute(url.toString(),  HttpMethod.GET, null, clientHttpResponse -> {
            File ret = File.createTempFile("download", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });

        // No exception, nothing wrong when arrived here
        FileUtils.moveFile(download, path.toFile());
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unexpected system error attempting to delete %s", path), e);
        }
    }
}
