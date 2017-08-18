package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.AwsApiResponse.ResponseBuilder;
import au.org.aodn.aws.wps.operation.Operation;
import net.opengis.wps._1_0.ExecuteResponse;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
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
            responseBuilder.body(getString(context, result));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception : " + e.getMessage());
            //TODO: handle as per wps/ogc exception handling requirements
            responseBuilder.statusCode(500);
            responseBuilder.body(e.getMessage());
        }

        return responseBuilder.build();
    }

    private String getString(JAXBContext context, Object result) throws JAXBException {
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        m.marshal(result, writer);
        return writer.toString();
    }

}
