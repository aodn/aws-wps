package au.org.aodn.aws.wps;

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


public class StatusCreator {
    private String location;
    private String jobId;

    public StatusCreator() {}

    public StatusCreator(String location, String jobId) {
        this.location = location;
        this.jobId = jobId;
    }

    public String getStatusLocation() {
        return this.location;

    }

    public ExecuteResponse createResponseDocument(EnumStatus jobStatus) {
        return createResponseDocument(jobStatus,  "", "");
    }

    public ExecuteResponse createResponseDocument(EnumStatus jobStatus,  String failedMessage, String failedCode) {

        ExecuteResponse response = new ExecuteResponse();
        String statusLocation = getStatusLocation();
        if(statusLocation != null) {
            response.setStatusLocation(getStatusLocation());
        }
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
        }  else if (jobStatus==EnumStatus.SUCCEDED) {
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


        return response;
    }

    static public String createXmlDocument (ExecuteResponse response) {
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
}


