package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.exception.InvalidRequestException;
import au.org.aodn.aws.wps.operation.OperationFactory;
import au.org.aodn.aws.wps.operation.Operation;
import au.org.aodn.aws.wps.request.XmlRequestParser;

public class XmlBodyParser implements RequestParser {
    private final String request;

    public XmlBodyParser(String request) {
        this.request = request;
    }

    @Override
    public Operation getOperation() throws InvalidRequestException {
        XmlRequestParser parser = new XmlRequestParser();
        return OperationFactory.getInstance(parser.parse(request));
    }

}
