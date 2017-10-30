package au.org.aodn.aws.wps;

/**
 * Created by craigj on 8/08/17.
 */
public class RequestParserFactory {
    public RequestParser getRequestParser(AwsApiRequest request) {
        if (request.getHttpMethod().equals("POST")) {
            return new XmlBodyParser(request.getBody());
        } else if (request.getHttpMethod().equals("GET")) {
            return new QueryStringParameterParser(request);
        } else {
            // TODO: handle as required by spec
            throw new UnsupportedOperationException(request.getHttpMethod() + "not supported");
        }
    }
}

