package au.org.aodn.aws.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
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
}
