package au.org.emii.wps.util;

import com.jayway.restassured.assertion.XMLAssertion;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class GPathMatcher extends TypeSafeMatcher<String> {
    private final String path;
    private final Matcher valueMatcher;

    public static GPathMatcher hasGPath(String path, Matcher matcher) {
        return new GPathMatcher(path, matcher);
    }

    public GPathMatcher(String path, Matcher valueMatcher) {
        this.path = path;
        this.valueMatcher = valueMatcher;
    }

    @Override
    protected boolean matchesSafely(String value) {
        Object result = getGPathResult(value);

        // Check for match
        return valueMatcher.matches(result);
    }

    @Override
    public void describeTo(Description description) {
        valueMatcher.describeTo(description);
        description.appendText(" at ").appendValue(path);
    }

    @Override
    protected void describeMismatchSafely(String value, Description mismatchDescription) {
        Object result = getGPathResult(value);
        valueMatcher.describeMismatch(result, mismatchDescription);
    }

    private Object getGPathResult(String value) {
        // Use xmlslurper to parse the value
        GPathResult parser = null;

        try {
            parser = new XmlSlurper().parseText(value);
        } catch (IOException |SAXException |ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        // Use RestAssured's XMLAssertion to get GPath result
        XMLAssertion assertion = new XMLAssertion();
        assertion.setKey(path);
        return assertion.getResult(parser, null);
    }

}
