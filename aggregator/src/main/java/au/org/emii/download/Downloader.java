package au.org.emii.download;

import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
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
        try {
            File download = template.execute(url.toString(),  HttpMethod.GET, null, clientHttpResponse -> {
                File ret = File.createTempFile("download", "tmp");
                ret.deleteOnExit();
                StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
                return ret;
            });

            // No exception, nothing wrong when arrived here
            FileUtils.moveFile(download, path.toFile());
        }
        catch(Exception e) {
            throw new DownloadException(e.getMessage(), e);
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
