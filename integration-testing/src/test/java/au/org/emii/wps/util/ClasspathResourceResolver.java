package au.org.emii.wps.util;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// Classpath resource resolver
// Used to resolve references to external resources in xsd documents to
// schema files in the class path during xml validation

public class ClasspathResourceResolver implements LSResourceResolver {

    private static final Map<String, String> SYSTEM_ID_MAPPING = new HashMap<>();

    {
        SYSTEM_ID_MAPPING.put("../../ows/1.1.0/owsAll.xsd", "/ows/1.1.0/owsAll.xsd");
        SYSTEM_ID_MAPPING.put("../../xlink/1.0.0/xlinks.xsd", "/xlink/1.0.0/xlinks.xsd");
        SYSTEM_ID_MAPPING.put("../../xml/xml.xsd", "/xml/xml.xsd");
    }

    private final String basePath; // location of parent schema - used to resolve relative references

    public ClasspathResourceResolver(String basePath) {
        this.basePath = basePath;
    }

    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId, String baseURI) {

        String resourcePath;

        if (SYSTEM_ID_MAPPING.containsKey(systemId)) {
            resourcePath = SYSTEM_ID_MAPPING.get(systemId);
        } else if (baseURI != null) {
            String baseURIPath = baseURI.replaceFirst("file://", "");
            resourcePath = Paths.get(baseURIPath).getParent().resolve(systemId).toString();
        } else {
            resourcePath = Paths.get(basePath, systemId).toString();
        }

        InputStream resourceAsStream = this.getClass().getResourceAsStream(resourcePath);

        if (resourceAsStream == null) {
            throw new ClasspathResolutionException(
                String.format("Could not find type=%s, namespaceURI=%s, publicId=%s, " +
                "systemId=%s, baseURI=%s at %s.", type, namespaceURI, publicId, systemId, baseURI, resourcePath));
        }

        return new DOMInputImpl(publicId, resourcePath, baseURI, resourceAsStream, null);
    }

    private class ClasspathResolutionException extends RuntimeException {
        public ClasspathResolutionException(String message) {
            super(message);
        }
    }
}
