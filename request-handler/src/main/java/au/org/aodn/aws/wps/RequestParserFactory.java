package au.org.aodn.aws.wps;

import javax.xml.bind.JAXBContext;

/**
 * Created by craigj on 8/08/17.
 */
public class RequestParserFactory {
    private final JAXBContext context;

    public RequestParserFactory(JAXBContext context) {
        this.context = context;
    }

    public RequestParser getRequestParser(AwsApiRequest request) {
        if (request.getHttpMethod().equals("POST")) {
            return new XmlBodyParser(context, request.getBody());
        } else if (request.getHttpMethod().equals("GET")) {
            return new QueryStringParameterParser(request);
        } else {
            // TODO: handle as required by spec
            throw new UnsupportedOperationException(request.getHttpMethod() + "not supported");
        }
    }
}

