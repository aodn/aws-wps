package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.Constants;
import au.org.aodn.aws.wps.exception.ValidationException;
import au.org.aodn.aws.wps.status.StatusHelper;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import net.opengis.ows._1.CodeType;
import net.opengis.wps._1_0.DescribeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static au.org.aodn.aws.wps.status.WpsConfig.AWS_REGION_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY;

public class DescribeProcessOperation implements Operation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescribeProcessOperation.class);

    private final DescribeProcess request;
    public static final String PROCESS_DESCRIPTION_FILE_EXTENSION = ".xml";

    public DescribeProcessOperation(DescribeProcess request) {
        this.request = request;
    }

    @Override
    public String execute()
    {
        String processDescriptionsS3Bucket = WpsConfig.getConfig(DESCRIBE_PROCESS_S3_BUCKET_CONFIG_KEY);
        String S3KeyPrefix = WpsConfig.getConfig(DESCRIBE_PROCESS_S3_KEY_PREFIX_CONFIG_KEY);
        String s3RegionName = WpsConfig.getConfig(AWS_REGION_CONFIG_KEY);

        List<CodeType> identifiers = request.getIdentifier();

        StringBuilder stringBuilder = new StringBuilder();

        for(CodeType identifier : identifiers)
        {
            String processName = identifier.getValue();
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
                stringBuilder.append(documentString);
            }
            catch(IOException ioex)
            {
                //  Bad stuff - blow up!
                LOGGER.error("Problem loading XML document: ", ioex);
                return StatusHelper.getExceptionReportString("Error retrieving process description: " + ioex.getMessage(), "ProcessingError");
            }
            catch(AmazonS3Exception s3e)
            {
                //  Bad stuff - blow up!
                LOGGER.error("Problem loading XML document: ", s3e);
                return StatusHelper.getExceptionReportString("Error retrieving process description: " + s3e.getMessage(), "ProcessingError");
            }
        }

        return stringBuilder.toString();
    }
}