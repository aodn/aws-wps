package au.org.aodn.aws.wps.status;

import net.opengis.ows._1.ExceptionReport;
import net.opengis.ows._1.ExceptionType;
import net.opengis.wps._1_0.ResponseBaseType;


import javax.xml.bind.JAXBContext;
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


    public static final XMLGregorianCalendar getCreationDate()
            throws DatatypeConfigurationException
    {
        GregorianCalendar currentTime = new GregorianCalendar();
        currentTime.setTime(new Date());
        currentTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(currentTime);
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

}
