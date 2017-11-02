package au.org.aodn.aws.util;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import net.opengis.wps.v_1_0_0.ProcessDescriptionType;
import net.opengis.wps.v_1_0_0.ProcessDescriptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

public class DescribeProcessHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescribeProcessHelper.class);

    private static final String PROCESS_DESCRIPTION_FILE_EXTENSION = ".xml";

    public static ProcessDescriptionType getProcessDescription(String qualifiedProcessName)
            throws OGCException {

        String processDescriptionsS3Bucket = WpsConfig.getConfig(WpsConfig.DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY);
        String S3KeyPrefix = WpsConfig.getConfig(WpsConfig.DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY);
        String s3RegionName = WpsConfig.getConfig(WpsConfig.AWS_REGION_CONFIG_KEY);

        LOGGER.info("Process name: " + qualifiedProcessName);

        String processName = qualifiedProcessName;

        if(processName.indexOf(":")!=-1)
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

        if(!s3Client.doesObjectExist(processDescriptionsS3Bucket,xmlDocumentS3Key))
        {
            throw new OGCException("InvalidParameterValue", "identifier", "No such process '" + processName + "'");
        }

        S3Object documentObject = s3Client.getObject(processDescriptionsS3Bucket, xmlDocumentS3Key);
        S3ObjectInputStream contentStream = documentObject.getObjectContent();

        //  read file to String
        String documentString = null;
        try
        {
            documentString = Utils.inputStreamToString(contentStream);

            JAXBContext context = JAXBContext.newInstance(ProcessDescriptionType.class);
            Unmarshaller u = context.createUnmarshaller();
            ProcessDescriptions currentProcessDescriptions = (ProcessDescriptions) u.unmarshal(new StringInputStream(documentString));

            if (currentProcessDescriptions.getProcessDescription().size() > 0) {
                for (ProcessDescriptionType currentDescription : currentProcessDescriptions.getProcessDescription()) {
                    if(currentDescription.getIdentifier().getValue().equalsIgnoreCase(qualifiedProcessName)) {
                        //  If the identifier matches the one requested
                        return currentDescription;
                    }
                }
            }

        } catch(Exception ex) {
            //  Bad stuff - blow up!
            LOGGER.error("Problem reading XML document for [" + processName + "]", ex);
            throw new OGCException("ProcessingError", "Error retrieving process description: " + ex.getMessage());
        }
        return null;
    }
}
