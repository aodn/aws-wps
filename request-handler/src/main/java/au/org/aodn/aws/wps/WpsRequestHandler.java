package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.AwsApiResponse.ResponseBuilder;
import au.org.aodn.aws.wps.exception.ValidationException;
import au.org.aodn.aws.wps.operation.Operation;
import net.opengis.wps._1_0.ExecuteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.xml.bind.JAXBContext;
import java.util.Properties;

public class WpsRequestHandler implements RequestHandler, RequestValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsRequestHandler.class);

    public AwsApiResponse handleRequest(AwsApiRequest request) {

        ResponseBuilder responseBuilder = new ResponseBuilder();

        try {
            JAXBContext context = JAXBContext.newInstance(ExecuteResponse.class);
            RequestParserFactory requestParserFactory = new RequestParserFactory(context);
            RequestParser requestParser = requestParserFactory.getRequestParser(request);
            Operation operation = requestParser.getOperation();
            LOGGER.info("Operation : " + operation.getClass());
            String result = operation.execute();
            LOGGER.info("Executed");
            responseBuilder.body(result);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Exception : " + e.getMessage(), e);
            //TODO: handle as per wps/ogc exception handling requirements
            responseBuilder.statusCode(500);
            responseBuilder.body(e.getMessage());
        }

        return responseBuilder.build();
    }

    @Override
    public void validate(AwsApiRequest request) throws ValidationException {

    }
}
