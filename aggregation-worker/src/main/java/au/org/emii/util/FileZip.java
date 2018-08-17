package au.org.emii.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileZip {

    public static final int BUFFER_SIZE = 1024;

    public static File zipFiles(String filePath, List<File> srcFiles) throws IOException {
        File outFile = new File(filePath);
        FileOutputStream outStream = new FileOutputStream(outFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outStream);
        for(File currentSrcFile : srcFiles) {
            FileInputStream inStream = new FileInputStream(currentSrcFile);
            ZipEntry zipEntry = new ZipEntry(currentSrcFile.getName());
            zipOutputStream.putNextEntry(zipEntry);

            byte[] bytes = new byte[BUFFER_SIZE];
            int length;
            while((length = inStream.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }

            inStream.close();
        }

        zipOutputStream.close();
        outStream.close();

        return outFile;
    }
}
