package au.org.aodn.aws.wps.status;

import net.opengis.ows._1.*;

import net.opengis.wps._1_0.*;
import net.opengis.ows._1.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

public class StatusHelper
{

    static int CHUNK_SIZE = 512;

    public static final String getStatusDocument(String s3Bucket, String statusFilename, String jobId, EnumStatus jobStatus, String message, String messageCode, HashMap<String, String> outputsHrefs)
    {
        String statusLocation = getS3ExternalURL(s3Bucket, jobId + "/" + statusFilename);
        ExecuteStatusBuilder statusBuilder = new ExecuteStatusBuilder(statusLocation, jobId);
        return statusBuilder.createResponseDocument(jobStatus, message, messageCode, outputsHrefs);
    }


    public static String getS3ExternalURL(String s3Bucket, String S3Key)
    {
        return WpsConfig.getS3BaseUrl() + s3Bucket + "/" + S3Key;
    }


    /**
     * Generate an XML string from a response XML type.
     * @param response
     * @return
     */
    public static String createResponseXmlDocument (ResponseBaseType response) {
        String responseDoc = null;

        JAXBContext context;
        try {
            context = JAXBContext.newInstance(response.getClass());
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            m.marshal(response, writer);
            responseDoc = writer.toString();

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return responseDoc;
    }


    public static ProcessFailedType getProcessFailedType(String message, String code)
    {
        ProcessFailedType failed = new ProcessFailedType();
        failed.setExceptionReport(getExceptionReport(message, code));
        return failed;
    }



    public static ExceptionReport getExceptionReport(String message, String code)
    {
        ExceptionReport report = new ExceptionReport();
        ExceptionType type = new ExceptionType();
        type.getExceptionText().add(message);
        type.setExceptionCode(code);
        report.getException().add(type);
        return report;
    }


    public static String getExceptionReportString(String message, String code)
    {
        ExceptionReport report = getExceptionReport(message, code);
        String responseDoc = null;

        JAXBContext context;
        try {
            context = JAXBContext.newInstance(ExceptionReport.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            m.marshal(report, writer);
            responseDoc = writer.toString();

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return responseDoc;
    }


    public static ProcessStartedType getProcessStartedType(String message, Integer percentComplete)
    {
        ProcessStartedType started = new ProcessStartedType();
        started.setValue(message);
        started.setPercentCompleted(percentComplete);
        return started;
    }


    public static final XMLGregorianCalendar getCreationDate()
            throws DatatypeConfigurationException
    {
        GregorianCalendar currentTime = new GregorianCalendar();
        currentTime.setTime(new Date());
        currentTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(currentTime);
    }


    public static void addExecuteOutputReference(ExecuteResponse response, String outputIdentifier, String outputHref)
    {
        net.opengis.wps._1_0.ObjectFactory objectFactory = new net.opengis.wps._1_0.ObjectFactory();
        OutputDataType output = objectFactory.createOutputDataType();

        OutputReferenceType outputReference = objectFactory.createOutputReferenceType();
        outputReference.setHref(outputHref);
        output.setReference(outputReference);

        CodeType outputIdentifierCode = new CodeType();
        outputIdentifierCode.setValue(outputIdentifier);
        output.setIdentifier(outputIdentifierCode);
        if(response.getProcessOutputs() == null) {
            ExecuteResponse.ProcessOutputs outputs = objectFactory.createExecuteResponseProcessOutputs();
            response.setProcessOutputs(outputs);
        }
        response.getProcessOutputs().getOutput().add(output);
    }

    public static void addExecuteLiteralProcessOutput(ExecuteResponse response, String outputIdentifier, InputStream inStream)
    {
        net.opengis.wps._1_0.ObjectFactory objectFactory = new net.opengis.wps._1_0.ObjectFactory();
        OutputDataType output = objectFactory.createOutputDataType();

        LiteralDataType literalData = new LiteralDataType();
        literalData.setDataType("??");
        literalData.setUom("??");
        if(inStream != null)
        {
            StringBuilder strBuilder = new StringBuilder();
            byte[] chunk = new byte[CHUNK_SIZE];
            BufferedInputStream bufferedStream = new BufferedInputStream(inStream);
            try {
                int bytesRead = bufferedStream.read(chunk);
                while (bytesRead != -1) {
                    strBuilder.append(new String(chunk, 0, bytesRead));
                }
            }
            catch(IOException ioex)
            {
                //  Error out
                throw new RuntimeException("Bad stuff: " + ioex.getMessage(), ioex);
            }

            //  Contents of the stream
            literalData.setValue(strBuilder.toString());
        }
        DataType data = new DataType();
        data.setLiteralData(literalData);
        output.setData(data);
        CodeType outputIdentifierCode = new CodeType();
        outputIdentifierCode.setValue(outputIdentifier);
        output.setIdentifier(outputIdentifierCode);
        response.getProcessOutputs().getOutput().add(output);
    }
}
