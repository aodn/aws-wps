package au.org.emii.util;

import au.org.aodn.aws.util.Utils;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class ProvenanceWriter {

    private static final Logger logger = LoggerFactory.getLogger(ProvenanceWriter.class);
    private static final String PROVENANCE_TEMPLATE_NAME = "ProvenanceTemplate";

    public static String write(String s3Bucket, String s3Key, Map<String, Object> parameters) {

        Configuration config;

        try {
            //  Get from S3 bucket location
            AmazonS3Client s3Client = new AmazonS3Client();
            //Region region = Region.getRegion(Regions.fromName(s3RegionName));
            //s3Client.setRegion(region);

            S3Object templateObject = s3Client.getObject(s3Bucket, s3Key);
            S3ObjectInputStream contentStream = templateObject.getObjectContent();

            //  read file to String
            String templateString = null;
            try {
                templateString = Utils.inputStreamToString(contentStream);
                logger.info("Provenance template: " + templateString);
            }
            catch(IOException ioex)
            {
                //  Bad stuff - blow up!
                logger.error("Problem loading template: ", ioex);
                throw ioex;
            }

            StringTemplateLoader stringLoader = new StringTemplateLoader();
            stringLoader.putTemplate(PROVENANCE_TEMPLATE_NAME, templateString);
            config = new Configuration();
            config.setTemplateLoader(stringLoader);
            config.setDefaultEncoding("UTF-8");
            config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            if (config == null) {
                return "Provenance templates not configured";
            }

            Template template = config.getTemplate(PROVENANCE_TEMPLATE_NAME);
            StringWriter writer = new StringWriter();
            template.process(parameters, writer);
            return writer.toString();
        } catch (FileNotFoundException e) {
            logger.error("No template (" + PROVENANCE_TEMPLATE_NAME + ") found for provenance document");
            return "Provenance template '"+ PROVENANCE_TEMPLATE_NAME + "' not found";
        } catch (TemplateException|IOException e) {
            logger.error("Error loading provenance document. S3 bucket [" + s3Bucket + ", S3 Key [" + s3Key + "]");
            return "Error loading provenance template '" + PROVENANCE_TEMPLATE_NAME + "' not found";
        }
    }
}

