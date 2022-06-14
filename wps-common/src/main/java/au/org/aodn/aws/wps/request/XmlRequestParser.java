package au.org.aodn.aws.wps.request;

import au.org.aodn.aws.wps.exception.InvalidRequestException;
import com.amazonaws.util.StringInputStream;
import net.opengis.wps.v_1_0_0.ExecuteResponse;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.UnsupportedEncodingException;

public class XmlRequestParser {
    private final JAXBContext context;

    public XmlRequestParser() {
        try {
            context = JAXBContext.newInstance(ExecuteResponse.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Unexpected error.  Could not create JAXBContext", e);
        }
    }

    public Object parse(String request) throws InvalidRequestException {
        try {
            Unmarshaller u = context.createUnmarshaller();
            return u.unmarshal(new StringInputStream(request));
        } catch (JAXBException|UnsupportedEncodingException e) {
            throw new InvalidRequestException(e);
        }
    }

}
