package au.org.aodn.aws.wps.status;

import net.opengis.ows._1.ExceptionReport;
import net.opengis.ows._1.ExceptionType;
import net.opengis.wps._1_0.ExecuteResponse;
import net.opengis.wps._1_0.ProcessFailedType;
import net.opengis.wps._1_0.ProcessStartedType;
import net.opengis.wps._1_0.StatusType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class ExecuteStatusBuilder {

    private String location;
    String jobId;

    public ExecuteStatusBuilder() {}

    public ExecuteStatusBuilder(String location, String jobId) {
        this.location = location;
        this.jobId = jobId;
    }

    public String getStatusLocation() {
        return this.location;

    }

    public String createResponseDocument(EnumStatus jobStatus) {
        return createResponseDocument(jobStatus,  "", "");
    }

    public String createResponseDocument(EnumStatus jobStatus,  String failedMessage, String failedCode) {

        ExecuteResponse response = new ExecuteResponse();
        response.setStatusLocation(getStatusLocation());

        StatusType status = new StatusType();

        try {
            GregorianCalendar currentTime = new GregorianCalendar();
            currentTime.setTime(new Date());
            currentTime.setTimeZone(TimeZone.getTimeZone("GMT"));
            XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(currentTime);

            status.setCreationTime(xmlDate);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }

        if (jobStatus==EnumStatus.ACCEPTED) {
            status.setProcessAccepted("Accepted job " + jobId + " for processing");
        } else if (jobStatus==EnumStatus.STARTED) {
            ProcessStartedType started = new ProcessStartedType();
            started.setValue("Job " + jobId + " is currently running");
            status.setProcessStarted(started);
        } else if (jobStatus==EnumStatus.PAUSED) {
            ProcessStartedType started = new ProcessStartedType();
            started.setValue("Job " + jobId + " is currently paused");
            status.setProcessPaused(started);
        }  else if (jobStatus==EnumStatus.SUCCEEDED) {
            status.setProcessSucceeded("Job " + jobId + " has completed");
        } else if (jobStatus==EnumStatus.FAILED) {
            ProcessFailedType failed = new ProcessFailedType();
            ExceptionReport report = new ExceptionReport();
            ExceptionType type = new ExceptionType();
            type.getExceptionText().add(failedMessage);
            type.setExceptionCode(failedCode);
            report.getException().add(type);
            failed.setExceptionReport(report);
        }
        response.setStatus(status);
        return createXmlDocument(response);
    }

    private String createXmlDocument (ExecuteResponse response) {
        String responseDoc = null;

        JAXBContext context;
        try {
            context = JAXBContext.newInstance(ExecuteResponse.class);
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

    public static void main(String[] args) {
        System.out.println();
        ExecuteStatusBuilder e = new ExecuteStatusBuilder();
    }
}



