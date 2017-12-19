package au.org.aodn.aws.wps.status;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.util.DescribeProcessHelper;
import au.org.aodn.aws.util.JobFileUtil;
import net.opengis.ows.v_1_1_0.CodeType;
import net.opengis.ows.v_1_1_0.LanguageStringType;
import net.opengis.wps.v_1_0_0.DataType;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.OutputDescriptionType;
import net.opengis.wps.v_1_0_0.ProcessBriefType;
import net.opengis.wps.v_1_0_0.LiteralDataType;
import net.opengis.wps.v_1_0_0.OutputDataType;
import net.opengis.wps.v_1_0_0.OutputReferenceType;
import net.opengis.wps.v_1_0_0.ProcessDescriptionType;
import net.opengis.wps.v_1_0_0.ProcessFailedType;
import net.opengis.wps.v_1_0_0.ProcessStartedType;
import net.opengis.wps.v_1_0_0.StatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static au.org.aodn.aws.wps.status.WpsConfig.AWS_BATCH_JOB_S3_KEY_PREFIX;

public class ExecuteStatusBuilder {

    private static int CHUNK_SIZE = 512;

    private String wpsEndpointUrl;
    private String statusServiceEndpointUrl;
    private String statusFileS3Location;
    private String jobId;


    private static final  Logger LOGGER = LoggerFactory.getLogger(ExecuteStatusBuilder.class);

    public ExecuteStatusBuilder(String jobId, String s3Bucket, String filename) {
        String jobFileS3KeyPrefix = WpsConfig.getProperty(AWS_BATCH_JOB_S3_KEY_PREFIX);
        this.wpsEndpointUrl = WpsConfig.getAwsWpsEndpointUrl();
        this.statusFileS3Location = WpsConfig.getS3ExternalURL(s3Bucket, jobFileS3KeyPrefix + jobId + "/" + filename);
        this.jobId = jobId;
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
    public String createResponseDocument(EnumStatus jobStatus, String processIdentifier, String failedMessage, String failedCode, HashMap<String, String> outputs) {

        ExecuteResponse response = new ExecuteResponse();
        response.setServiceInstance(wpsEndpointUrl);
        response.setLang(WpsConfig.getProperty(WpsConfig.DEFAULT_LANGUAGE));
        response.setStatusLocation(WpsConfig.getStatusServiceXmlEndpoint(jobId));

        ProcessDescriptionType processDescription = null;

        try {
            processDescription = DescribeProcessHelper.getProcessDescription(processIdentifier);
        } catch(OGCException ex) {
            // We'll return a response without this element - even though it isn't
            // compliant with the schema
            LOGGER.error("Unable to retrieve process description for [" + processIdentifier + "]: " + ex.getMessage(), ex);
        }

        StatusType status = new StatusType();

        try {
            status.setCreationTime(JobFileUtil.getCreationDate());
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }

        if (jobStatus == EnumStatus.ACCEPTED) {
            status.setProcessAccepted("Accepted job " + jobId + " for processing");
        } else if (jobStatus == EnumStatus.STARTED) {
            status.setProcessStarted(getProcessStartedType("Job " + jobId + " is currently running", new Integer(0)));
        } else if (jobStatus == EnumStatus.PAUSED) {
            status.setProcessPaused(getProcessStartedType("Job " + jobId + " is currently paused", new Integer(0)));
        } else if (jobStatus == EnumStatus.SUCCEEDED) {
            status.setProcessSucceeded("Job " + jobId + " has completed");
            //  If outputs were passed - add them to the response
            if (outputs != null) {
                for (Map.Entry<String, String> currentEntry : outputs.entrySet()) {
                    String outputIdentifier = currentEntry.getKey();
                    String href = currentEntry.getValue();
                    LanguageStringType title = getOutputTitle(processDescription, outputIdentifier);
                    LOGGER.info("OUTPUT [" + outputIdentifier + "]=[" + href + "]");
                    addOutputToResponse(response, outputIdentifier, title, href);
                }
            }
        } else if (jobStatus == EnumStatus.FAILED) {
            status.setProcessFailed(getProcessFailedType(failedMessage, failedCode));
        }

        //  Form the Process section of the response

        if (processDescription != null) {
            ProcessBriefType processBriefType = new ProcessBriefType();
            processBriefType.setIdentifier(processDescription.getIdentifier());
            processBriefType.setAbstract(processDescription.getAbstract());
            processBriefType.setTitle(processDescription.getTitle());
            processBriefType.setProcessVersion(processDescription.getProcessVersion());
            response.setProcess(processBriefType);
        }

        response.setStatus(status);

        return JobFileUtil.createXmlDocument(response);
    }

    private ProcessFailedType getProcessFailedType(String message, String code) {
        ProcessFailedType failed = new ProcessFailedType();
        failed.setExceptionReport(JobFileUtil.getExceptionReport(message, code));
        return failed;
    }


    private ProcessStartedType getProcessStartedType(String message, Integer percentComplete) {
        ProcessStartedType started = new ProcessStartedType();
        started.setValue(message);
        started.setPercentCompleted(percentComplete);
        return started;
    }


    private LanguageStringType getOutputTitle(ProcessDescriptionType processDescription, String outputIdentifier) {
        if (processDescription == null || processDescription.getProcessOutputs() == null) {
            return null;
        }

        for (OutputDescriptionType output: processDescription.getProcessOutputs().getOutput()) {
            if (output.getIdentifier().getValue().equals(outputIdentifier)) {
                return output.getTitle();
            }
        }

        return null;
    }

    private void addOutputToResponse(ExecuteResponse response, String outputIdentifier, LanguageStringType title, String outputHref) {
        OutputDataType output = new OutputDataType();

        OutputReferenceType outputReference = new OutputReferenceType();
        outputReference.setHref(outputHref);
        output.setReference(outputReference);

        CodeType outputIdentifierCode = new CodeType();
        outputIdentifierCode.setValue(outputIdentifier);
        output.setIdentifier(outputIdentifierCode);
        output.setTitle(title);
        if (response.getProcessOutputs() == null) {
            ExecuteResponse.ProcessOutputs outputs = new ExecuteResponse.ProcessOutputs();
            response.setProcessOutputs(outputs);
        }
        response.getProcessOutputs().getOutput().add(output);
    }


    private void addOutputToResponse(ExecuteResponse response, String outputIdentifier, InputStream inStream) {
        OutputDataType output = new OutputDataType();

        LiteralDataType literalData = new LiteralDataType();
        literalData.setDataType("??");
        literalData.setUom("??");
        if (inStream != null) {
            StringBuilder strBuilder = new StringBuilder();
            byte[] chunk = new byte[CHUNK_SIZE];
            BufferedInputStream bufferedStream = new BufferedInputStream(inStream);
            try {
                int bytesRead = bufferedStream.read(chunk);
                while (bytesRead != -1) {
                    strBuilder.append(new String(chunk, 0, bytesRead));
                }
            } catch (IOException ioex) {
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



