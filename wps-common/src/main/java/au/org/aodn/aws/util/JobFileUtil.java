package au.org.aodn.aws.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.StringInputStream;
import net.opengis.ows.v_1_1_0.ExceptionReport;
import net.opengis.ows.v_1_1_0.ExceptionType;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.StatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class JobFileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobFileUtil.class);

    /**
     * Generate an XML string from a XML type object.
     *
     * @param xmlObject
     * @return
     */
    public static String createXmlDocument(Object xmlObject) {
        String responseDoc = null;

        JAXBContext context;
        try {
            context = JAXBContext.newInstance(xmlObject.getClass());
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            m.marshal(xmlObject, writer);
            responseDoc = writer.toString();

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return responseDoc;
    }


    public static final XMLGregorianCalendar getCreationDate()
            throws DatatypeConfigurationException {
        GregorianCalendar currentTime = new GregorianCalendar();
        currentTime.setTime(new Date());
        currentTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(currentTime);
    }

    public static ExceptionReport getExceptionReport(String message, String code) {
        return getExceptionReport(message, code, null);
    }

    public static ExceptionReport getExceptionReport(String message, String code, String locator) {
        ExceptionReport report = new ExceptionReport();
        ExceptionType type = new ExceptionType();
        //  TODO: externalise?
        report.setVersion("1.0.0");
        type.getExceptionText().add(message);
        type.setExceptionCode(code);
        type.setLocator(locator);
        report.getException().add(type);
        return report;
    }


    public static String getExceptionReportString(String message, String code) {
        return getExceptionReportString(message, code, null);
    }

    public static String getExceptionReportString(String message, String code, String locator) {
        ExceptionReport report = getExceptionReport(message, code, locator);
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

    /**
     * Create an ExecuteResponse object from a XML string
     *
     * @param xmlString
     * @return
     */
    public static ExecuteResponse unmarshallExecuteResponse(String xmlString)
    {
        try {

            JAXBContext context = JAXBContext.newInstance(ExecuteResponse.class);
            Unmarshaller u = context.createUnmarshaller();

            return (ExecuteResponse) u.unmarshal(new StringInputStream(xmlString));
        } catch (Exception ex) {

            return null;
        }
    }

    public static boolean isJobWaiting(StatusType currentStatus) {
        //  WPS status will be ProcessAccepted from the time the job is submitted & when it is
        //  picked up for processing.
        if (currentStatus.isSetProcessAccepted() &&
                (!currentStatus.isSetProcessFailed() && !currentStatus.isSetProcessStarted() && !currentStatus.isSetProcessSucceeded())) {
            return true;
        }
        return false;
    }


    public static boolean isJobRunning(StatusType currentStatus) {
        //  WPS status will be ProcessAccepted from the time the job is submitted & when it is
        //  picked up for processing.
        if (currentStatus.isSetProcessStarted()) {
            return true;
        }
        return false;
    }


    public static boolean isJobCompleted(StatusType currentStatus) {
        //  WPS status will be ProcessAccepted from the time the job is submitted & when it is
        //  picked up for processing.
        if (currentStatus.isSetProcessSucceeded() || currentStatus.isSetProcessFailed()) {
            return true;
        }
        return false;
    }


    public static ExecuteResponse getExecuteResponse(String jobFileS3KeyPrefix, String jobId, String statusFilename, String statusS3Bucket) {
        String statusXMLString = getExecuteResponseString(jobFileS3KeyPrefix, jobId, statusFilename, statusS3Bucket);
        if(statusXMLString != null)
        {
            //  Read the status document
            return JobFileUtil.unmarshallExecuteResponse(statusXMLString);
        }

        return null;
    }


    public static String getExecuteResponseString(String jobFileS3KeyPrefix, String jobId, String statusFilename, String statusS3Bucket) {
        String s3Key = jobFileS3KeyPrefix + jobId + "/" + statusFilename;

        //  Check for the existence of the status document
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        boolean statusExists = s3Client.doesObjectExist(statusS3Bucket, s3Key);


        LOGGER.info("Status file exists for jobId [" + jobId + "]? " + statusExists);

        //  If the status file exists and the job is in an 'waiting' state (we have accepted the job but processing
        //  has not yet commenced) we will attempt to work out the queue position of the job and add that to
        //  the status information we send back to the caller.  If the job is being processed or processing has
        //  completed (successful or failed), then we will return the information contained in the status file unaltered.
        if (statusExists) {

            String statusXMLString = null;

            LOGGER.info("Reading status file: Bucket [" + statusS3Bucket + "],  Key [" + s3Key + "]");
            try {
                statusXMLString = S3Utils.readS3ObjectAsString(statusS3Bucket, s3Key);

                return statusXMLString;
            } catch(IOException ioex) {
                LOGGER.error("Unable to unmarshall execute response.", ioex);
            }
        }

        return null;
    }
}
