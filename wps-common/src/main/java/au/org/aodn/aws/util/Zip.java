package au.org.aodn.aws.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Zip {

    public static final int BUFFER_SIZE = 1024;
    private static Logger logger = LogManager.getLogger(Zip.class);

    public static File zipFiles(String filePath, List<File> srcFiles) throws IOException {
        File outFile = new File(filePath);
        try (FileOutputStream outStream = new FileOutputStream(outFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outStream)) {

            //  Add each file to the ZIP file + create a ZipEntry for each
            for(File currentSrcFile : srcFiles) {
                try (FileInputStream inStream = new FileInputStream(currentSrcFile)) {
                    ZipEntry zipEntry = new ZipEntry(currentSrcFile.getName());
                    zipOutputStream.putNextEntry(zipEntry);

                    byte[] bytes = new byte[BUFFER_SIZE];
                    int length;
                    while ((length = inStream.read(bytes)) >= 0) {
                        zipOutputStream.write(bytes, 0, length);
                    }

                    logger.info("Added file to ZIP: " + currentSrcFile.getName());
                }
            }
        }

        logger.info("ZIP file size: " + outFile.length() + " bytes");

        return outFile;
    }

    /**
     * Unzip the provided zip file to the location provided.
     *
     * @param zipFile  The zip file
     * @param location A location to unzip the files to
     */
    public static void unzipFiles(ZipFile zipFile, File location) {
        if(zipFile != null) {
            logger.info("Unzipping file: " + zipFile.getName());
            logger.info("Zip file size : " + zipFile.size());
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while(zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();

                logger.info("  - Zip entry: Name [" + entry.getName() + "], Size [" + entry.getSize() + "bytes], Compressed size [" + entry.getCompressedSize() + "bytes]");

                if(location.isDirectory() && !location.exists()) {
                    boolean created = location.mkdirs();
                    if(created) {
                        logger.info("Created directories.");
                    }
                }

                File parent = location.getParentFile();
                if(parent != null) {
                    boolean created = parent.mkdirs();
                    if(created) {
                        logger.info("Created parent directories.");
                    }
                }

                File outFile = new File(location, entry.getName());
                logger.info("Unzipping file: " + outFile.getAbsolutePath());

                try (FileOutputStream outStream = new FileOutputStream(outFile);
                     InputStream inStream = zipFile.getInputStream(entry)) {

                    byte[] bytes = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while((bytesRead = inStream.read(bytes)) > 0) {
                        outStream.write(bytes, 0, bytesRead);
                    }

                    outStream.flush();
                } catch(Exception ex) {
                    logger.error("Unable to write file: " + outFile.getAbsolutePath(), ex);
                }
            }
        }
    }
}
