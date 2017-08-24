package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.AwsApiResponse.ResponseBuilder;
import au.org.aodn.aws.wps.operation.Operation;
import net.opengis.wps._1_0.ExecuteResponse;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Properties;

public class WpsRequestHandler implements RequestHandler, RequestValidator {

    private static final Logger LOGGER = Logger.getLogger(WpsRequestHandler.class);

    public AwsApiResponse handleRequest(AwsApiRequest request, Properties config) {

        ResponseBuilder responseBuilder = new ResponseBuilder();

        try {
            JAXBContext context = JAXBContext.newInstance(ExecuteResponse.class);
            RequestParserFactory requestParserFactory = new RequestParserFactory(context);
            RequestParser requestParser = requestParserFactory.getRequestParser(request);
            Operation operation = requestParser.getOperation();
            LOGGER.debug("Operation : " + operation.getClass());
            String result = operation.execute(config);
            LOGGER.debug("Executed");
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

    public void validate(AwsApiRequest request, Properties config) {
        //  Validate request content as required
    }
}
