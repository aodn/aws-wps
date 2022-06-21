package au.org.aodn.aws.wps;
import com.amazonaws.util.StringInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LocalStorage implements Storage<File> {

    protected Logger logger = LoggerFactory.getLogger(LocalStorage.class);

    @Override
    public FilterInputStream getObjectStream(String path, String name) throws IOException {
        logger.info("Get object stream from {}/{}", path, name);
        return new BufferedInputStream(new FileInputStream(path + File.separator + name));
    }

    @Override
    public String readObjectAsString(String path, String name) throws IOException {
        logger.info("Read object as string from {}/{}", path, name);
        return StringUtils.fileContentsToString(path + File.separator + name);
    }

    @Override
    public File getObject(String path, String name) {
        return new File(path + File.separator + name);
    }

    @Override
    public void uploadToTarget(File file, String path, String name, String contentType) throws InterruptedException, IOException {
        logger.info("Upload file to {}/{}", path, name);
        FileUtils.copyFile(file, new File(path + File.separator + name));
    }

    @Override
    public void uploadToTarget(String document, String path, String name, String contentType) throws IOException {
        logger.info("Upload string to {}/{}", path, name);
        logger.info(document);
        FileUtils.copyToFile(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)), getObject(path, name));
    }

    @Override
    public int getExpirationinDays(String path) {
        return 0;
    }
}
