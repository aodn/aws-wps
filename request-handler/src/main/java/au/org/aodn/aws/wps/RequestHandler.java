package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.AwsApiResponse.ResponseBuilder;
import au.org.aodn.aws.wps.operation.Operation;
import net.opengis.wps._1_0.ExecuteResponse;

import javax.xml.bind.JAXBContext;
import java.util.Properties;

public class RequestHandler {
    public AwsApiResponse handleRequest(AwsApiRequest request, Properties config) {
        ResponseBuilder responseBuilder = new ResponseBuilder();

        try {
            JAXBContext context = JAXBContext.newInstance(ExecuteResponse.class);
            RequestParserFactory requestParserFactory = new RequestParserFactory(context);
            RequestParser requestParser = requestParserFactory.getRequestParser(request);
            Operation operation = requestParser.getOperation();
            System.out.println("Operation : " + operation.getClass());
            Object result = operation.execute(config);
            System.out.print("Executed");
            responseBuilder.body(result.toString());
        } catch (Exception e) {
            e.printStackTrace();

            StatusCreator statusUpdater = new StatusCreator();
            String responseDocument = StatusCreator.createXmlDocument(statusUpdater.createResponseDocument(EnumStatus.FAILED, e.getMessage(), e.getClass().getName()));
            responseBuilder.body(responseDocument);
        }

        return responseBuilder.build();
    }
}
