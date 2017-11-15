package au.org.emii.wps.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.xml.HasXPath;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import ucar.nc2.NCdumpW;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.MatcherAssert.assertThat;

public class NcmlValidatable {
    private static final String XPATH = "XPath";
    private final String ncml;

    public static NcmlValidatable getNcml(String location) throws IOException {
        StringWriter stringWriter = new StringWriter();
        NCdumpW.print(location, stringWriter, false, false, true, false, null, null);
        return new NcmlValidatable(stringWriter.toString());
    }

    public NcmlValidatable(String ncml) {
        this.ncml = ncml;
    }

    public void content(Matcher... matchers) {
        for (Matcher matcher: matchers) {
            if (isXPathMatcher(matcher)) {
                // Node matcher
                Node node = getNode(ncml);
                assertThat(node, matcher);
            } else {
                // String matcher
                assertThat(ncml, matcher);
            }
        }
    }

    private boolean isXPathMatcher(Matcher matcher) {
        return matcher instanceof HasXPath || isNestedMatcherContainingXPathMatcher(matcher);
    }

    private boolean isNestedMatcherContainingXPathMatcher(Matcher matcher) {
        Description description = new StringDescription();
        matcher.describeTo(description);
        return description.toString().contains(XPATH);
    }

    private Node getNode(String value) {
        // Use XPath Matching
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        Node node;

        try {
            node = factory.newDocumentBuilder().parse(new ByteArrayInputStream(value.getBytes())).getDocumentElement();
        } catch (SAXException |IOException |ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return node;
    }

}
