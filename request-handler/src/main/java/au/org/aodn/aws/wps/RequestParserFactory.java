package au.org.aodn.aws.wps;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.org.aodn.aws.wps.exception.InvalidRequestException;
import java.util.Map;

/**
 * Created by craigj on 8/08/17.
 */
public class RequestParserFactory {
    Logger logger = LoggerFactory.getLogger(RequestParserFactory.class);

    public RequestParser getRequestParser(AwsApiRequest request) {
        if (request.getHttpMethod().equals("POST")) {
            return new XmlBodyParser(request.getBody());
        }
        else if (request.getHttpMethod().equals("GET")) {
            return new QueryStringParameterParser(request);
        }
        else {
            // TODO: handle as required by spec
            Map<String, String> parameters = request.getQueryStringParameters();
            Map<String, String> headers = request.getHeaders();

            if (!parameters.isEmpty()) {
                for (String key : parameters.keySet()) {
                    logger.error("  - Query parameter: Key [" + key + "], Value [" + parameters.get(key) + "]");
                }
            }
            else {
                throw new InvalidRequestException("Request parameters are empty");
            }

            //  Log some debugging information
            logger.error("Unsupported HTTP method invoked: " + request.getHttpMethod());

            for (String key : headers.keySet()) {
                logger.error("  - Query header: Key [" + key + "], Value [" + headers.get(key) + "]");
            }

            logger.error("BODY: [" + request.getBody() + "]");

            throw new UnsupportedOperationException(request.getHttpMethod() + " not supported");
        }
    }
}

