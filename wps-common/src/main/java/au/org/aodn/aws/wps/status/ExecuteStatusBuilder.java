package au.org.aodn.aws.wps.status;

import net.opengis.wps._1_0.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.HashMap;

public class ExecuteStatusBuilder {

    private String location;
    String jobId;
    Logger LOGGER  = LoggerFactory.getLogger(ExecuteStatusBuilder.class);


    public ExecuteStatusBuilder() {}

    public ExecuteStatusBuilder(String location, String jobId) {
        this.location = location;
        this.jobId = jobId;
    }

    public String getStatusLocation() {
        return this.location;

    }

    public String createResponseDocument(EnumStatus jobStatus)
    {
        return createResponseDocument(jobStatus,  "", "", null);
    }


    /**
     * The outputs HashMap is a map of the output name to the output result.
     * At this point we are assuming that the 'value' in the map is a HREF to a result file (we will form an OutputReferenceType for each).
     * The map could contain streams of data in future to support synchronous data response (LiteralDataType + ComplexDataType)
     *
     * @param jobStatus
     * @param failedMessage
     * @param failedCode
     * @param outputs
     * @return
     */
    public String createResponseDocument(EnumStatus jobStatus, String failedMessage, String failedCode, HashMap<String, String> outputs) {

        ExecuteResponse response = new ExecuteResponse();
        response.setStatusLocation(getStatusLocation());

        StatusType status = new StatusType();

        try
        {
            status.setCreationTime(StatusHelper.getCreationDate());
        }
        catch (DatatypeConfigurationException e)
        {
            throw new RuntimeException(e);
        }

        if (jobStatus==EnumStatus.ACCEPTED)
        {
            status.setProcessAccepted("Accepted job " + jobId + " for processing");
        }
        else if (jobStatus==EnumStatus.STARTED)
        {
            status.setProcessStarted(StatusHelper.getProcessStartedType("Job " + jobId + " is currently running", new Integer(0)));
        }
        else if (jobStatus==EnumStatus.PAUSED)
        {
            status.setProcessPaused(StatusHelper.getProcessStartedType("Job " + jobId + " is currently paused", new Integer(0)));
        }
        else if (jobStatus==EnumStatus.SUCCEEDED)
        {
            status.setProcessSucceeded("Job " + jobId + " has completed");
            //  If outputs were passed - add them to the response
            if(outputs != null)
            {
                for(String currentKey : outputs.keySet())
                {
                    String href = outputs.get(currentKey);
                    LOGGER.info("OUTPUT [" + currentKey + "]=[" + href + "]");
                    StatusHelper.addExecuteOutputReference(response, currentKey, href);
                }
            }
        }
        else if (jobStatus==EnumStatus.FAILED)
        {
            status.setProcessFailed(StatusHelper.getProcessFailedType(failedMessage, failedCode));
        }

        response.setStatus(status);


        return StatusHelper.createResponseXmlDocument(response);
    }


    public static void main(String[] args) {
        System.out.println();
        ExecuteStatusBuilder e = new ExecuteStatusBuilder();
    }
}



