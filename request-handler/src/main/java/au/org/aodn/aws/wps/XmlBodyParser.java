package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.exception.InvalidRequestException;
import au.org.aodn.aws.wps.operation.OperationFactory;
import au.org.aodn.aws.wps.operation.Operation;
import com.amazonaws.util.StringInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.UnsupportedEncodingException;

public class XmlBodyParser implements RequestParser {
    private final JAXBContext context;
    private final String body;

    public XmlBodyParser(JAXBContext context, String body) {
        this.context = context;
        this.body = body;
    }

    @Override
    public Operation getOperation() throws InvalidRequestException {
        try {
            Unmarshaller u = context.createUnmarshaller();
            Object request = u.unmarshal(new StringInputStream(body));
            return OperationFactory.getInstance(request);
        } catch (JAXBException|UnsupportedEncodingException e) {
            throw new InvalidRequestException(e);
        }
    }

}
