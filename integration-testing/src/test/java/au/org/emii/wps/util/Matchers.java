package au.org.emii.wps.util;

import io.restassured.internal.matcher.xml.XmlXsdMatcher;

import static io.restassured.internal.matcher.xml.XmlXsdMatcher.matchesXsdInClasspath;

public class Matchers {

    // Validate using schema defns sourced from classpath
    // Requires the use of a Classpath Resource resolver to resolve referenced
    // resources from the classpath
    public static XmlXsdMatcher validateWith(String schema) {
        String basePath = Classpath.getParentFolder(schema);

        return matchesXsdInClasspath(schema)
            .using(new ClasspathResourceResolver(basePath));
    }

}
