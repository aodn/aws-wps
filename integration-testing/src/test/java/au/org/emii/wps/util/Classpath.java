package au.org.emii.wps.util;

public class Classpath {

    private static final String SEPARATOR = "/";

    public static String getParentFolder(String path) {
        int lastSlashPos = path.lastIndexOf("/");
        return lastSlashPos>=0 ? path.substring(0, lastSlashPos) : "";
    }

    public static String get(String... segments) {
        return String.join(SEPARATOR, segments);
    }
}
