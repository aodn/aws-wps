package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.Constants;
import au.org.aodn.aws.wps.exception.InvalidRequestException;
import au.org.aodn.aws.wps.exception.ValidationException;
import au.org.aodn.aws.wps.status.StatusHelper;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import net.opengis.ows._1.CodeType;
import net.opengis.wps._1_0.DescribeProcess;
import net.opengis.wps._1_0.ProcessDescriptionType;
import net.opengis.wps._1_0.ProcessDescriptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringWriter;
import java.util.List;

public class DescribeProcessOperation implements Operation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescribeProcessOperation.class);

    private final DescribeProcess request;
    public static final String PROCESS_DESCRIPTION_FILE_EXTENSION = ".xml";

    public DescribeProcessOperation(DescribeProcess request) {
        this.request = request;
    }

    public DescribeProcess getRequest()
    {
        return this.request;
    }

    @Override
    public String execute()
    {
        String processDescriptionsS3Bucket = WpsConfig.getConfig(WpsConfig.DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY);
        String S3KeyPrefix = WpsConfig.getConfig(WpsConfig.DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY);
        String s3RegionName = WpsConfig.getConfig(WpsConfig.AWS_REGION_CONFIG_KEY);

        List<CodeType> identifiers = request.getIdentifier();

        StringBuilder stringBuilder = new StringBuilder();

        if(identifiers != null) {
            ProcessDescriptions outputProcessDescriptions = new ProcessDescriptions();

            for (CodeType identifier : identifiers) {
                //  The identifier passed will be prefixed with 'gs:' - ie: gs:GoGoDuck
                //  We will strip off the gs: part for the purposes of reading the S3 file.
                String processName = identifier.getValue();
                LOGGER.info("Process name: " + processName);
                if(processName.indexOf(":") != -1)
                {
                    processName = processName.substring(processName.indexOf(":") + 1);
                    LOGGER.info("Process name after substring: " + processName);
                }

                String xmlDocumentS3Key = S3KeyPrefix + processName + PROCESS_DESCRIPTION_FILE_EXTENSION;
                LOGGER.info("S3 Bucket: " + processDescriptionsS3Bucket);
                LOGGER.info("Process description S3 key: " + xmlDocumentS3Key);

                //  Get from S3 bucket location
                AmazonS3Client s3Client = new AmazonS3Client();
                Region region = Region.getRegion(Regions.fromName(s3RegionName));
                s3Client.setRegion(region);

                S3Object documentObject = s3Client.getObject(processDescriptionsS3Bucket, xmlDocumentS3Key);
                S3ObjectInputStream contentStream = documentObject.getObjectContent();

                //  read file to String
                String documentString = null;
                try {
                    documentString = Utils.inputStreamToString(contentStream);

                    JAXBContext context = JAXBContext.newInstance(ProcessDescriptionType.class);
                    Unmarshaller u = context.createUnmarshaller();
                    ProcessDescriptions currentProcessDescriptions = (ProcessDescriptions) u.unmarshal(new StringInputStream(documentString));

                    if(currentProcessDescriptions.getProcessDescription().size() > 0) {
                        for(ProcessDescriptionType currentDescription : currentProcessDescriptions.getProcessDescription()) {
                            outputProcessDescriptions.getProcessDescription().add(currentDescription);
                        }
                    }

                } catch (Exception ex) {
                    //  Bad stuff - blow up!
                    LOGGER.error("Problem reading XML document for [" + identifier + "]", ex);
                    return StatusHelper.getExceptionReportString("Error retrieving process description: " + ex.getMessage(), "ProcessingError");
                }
            }

            //  Marshall the output
            try {
                JAXBContext descriptionsContext = JAXBContext.newInstance(ProcessDescriptions.class);
                Marshaller m = descriptionsContext.createMarshaller();
                StringWriter stringWriter = new StringWriter();
                m.marshal(outputProcessDescriptions, stringWriter);
                return stringWriter.toString();
            }
            catch(Exception ex)
            {
                LOGGER.error("Error forming process descriptions XML: " + ex.getMessage(), ex);
                return StatusHelper.getExceptionReportString("Error forming process descriptions XML: " + ex.getMessage(), "ProcessingError");
            }
        }

        return stringBuilder.toString();
    }

}
