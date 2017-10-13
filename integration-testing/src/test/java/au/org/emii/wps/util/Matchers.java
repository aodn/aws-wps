package au.org.emii.wps.util;

import com.jayway.restassured.internal.matcher.xml.XmlXsdMatcher;

import static com.jayway.restassured.internal.matcher.xml.XmlXsdMatcher.matchesXsdInClasspath;

public class Matchers {

    // Validate using schema defns sourced from classpath
    // Requires the use of a Classpath Resource resolver to resolve referenced
    // resources from the classpath
    public static XmlXsdMatcher validateWith(String schema) {
        String basePath = getParentFolder(schema);

        return matchesXsdInClasspath(schema)
            .using(new ClasspathResourceResolver(basePath));
    }

    private static String getParentFolder(String schema) {
        int lastSlashPos = schema.lastIndexOf("/");
        return lastSlashPos>=0 ? schema.substring(0, lastSlashPos) : "";
    }

}
