package au.org.aodn.aws.wps.status;

import net.opengis.ows._1.CodeType;
import net.opengis.wps._1_0.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static au.org.aodn.aws.wps.status.StatusHelper.getS3ExternalURL;

public class ExecuteStatusBuilder {

    static int CHUNK_SIZE = 512;

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


    public static final String getStatusDocument(String s3Bucket, String statusFilename, String jobId, EnumStatus jobStatus, String message, String messageCode, HashMap<String, String> outputsHrefs)
    {
        String statusLocation = getS3ExternalURL(s3Bucket, jobId + "/" + statusFilename);
        ExecuteStatusBuilder statusBuilder = new ExecuteStatusBuilder(statusLocation, jobId);
        return statusBuilder.createResponseDocument(jobStatus, message, messageCode, outputsHrefs);
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

        //  TODO: can we update percentage complete to a rough value at various stages?
        if (jobStatus==EnumStatus.ACCEPTED)
        {
            status.setProcessAccepted("Accepted job " + jobId + " for processing");
        }
        else if (jobStatus==EnumStatus.STARTED)
        {
            status.setProcessStarted(getProcessStartedType("Job " + jobId + " is currently running", new Integer(0)));
        }
        else if (jobStatus==EnumStatus.PAUSED)
        {
            status.setProcessPaused(getProcessStartedType("Job " + jobId + " is currently paused", new Integer(0)));
        }
        else if (jobStatus==EnumStatus.SUCCEEDED)
        {
            status.setProcessSucceeded("Job " + jobId + " has completed");
            //  If outputs were passed - add them to the response
            if(outputs != null)
            {
                for(Map.Entry<String, String> currentEntry : outputs.entrySet())
                {
                    String key = currentEntry.getKey();
                    String href = currentEntry.getValue();
                    LOGGER.info("OUTPUT [" + key + "]=[" + href + "]");
                    addOutputToResponse(response, key, href);
                }
            }
        }
        else if (jobStatus==EnumStatus.FAILED)
        {
            status.setProcessFailed(getProcessFailedType(failedMessage, failedCode));
        }

        response.setStatus(status);


        return StatusHelper.createResponseXmlDocument(response);
    }


    public static void main(String[] args) {
        System.out.println();
        ExecuteStatusBuilder e = new ExecuteStatusBuilder();
    }


    private ProcessFailedType getProcessFailedType(String message, String code)
    {
        ProcessFailedType failed = new ProcessFailedType();
        failed.setExceptionReport(StatusHelper.getExceptionReport(message, code));
        return failed;
    }


    private ProcessStartedType getProcessStartedType(String message, Integer percentComplete)
    {
        ProcessStartedType started = new ProcessStartedType();
        started.setValue(message);
        started.setPercentCompleted(percentComplete);
        return started;
    }


    private void addOutputToResponse(ExecuteResponse response, String outputIdentifier, String outputHref)
    {
        OutputDataType output = new OutputDataType();

        OutputReferenceType outputReference = new OutputReferenceType();
        outputReference.setHref(outputHref);
        output.setReference(outputReference);

        CodeType outputIdentifierCode = new CodeType();
        outputIdentifierCode.setValue(outputIdentifier);
        output.setIdentifier(outputIdentifierCode);
        if(response.getProcessOutputs() == null) {
            ExecuteResponse.ProcessOutputs outputs = new ExecuteResponse.ProcessOutputs();
            response.setProcessOutputs(outputs);
        }
        response.getProcessOutputs().getOutput().add(output);
    }


    private void addOutputToResponse(ExecuteResponse response, String outputIdentifier, InputStream inStream)
    {
        OutputDataType output = new OutputDataType();

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
                throw new RuntimeException("Unable to add literal output to response : " + ioex.getMessage(), ioex);
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



