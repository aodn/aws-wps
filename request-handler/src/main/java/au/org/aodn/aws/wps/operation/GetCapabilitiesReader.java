package au.org.aodn.aws.wps.operation;


import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import freemarker.cache.StringTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;


public class GetCapabilitiesReader {

    private static final Logger logger = LoggerFactory.getLogger(GetCapabilitiesReader.class);

    private final Configuration freemarkerConfig;

    public GetCapabilitiesReader(String templateS3Bucket, String templateS3Key, String s3RegionName) throws IOException
    {
        //  Get from S3 bucket location
        AmazonS3Client s3Client = new AmazonS3Client();
        Region region = Region.getRegion(Regions.fromName(s3RegionName));
        s3Client.setRegion(region);

        S3Object templateObject = s3Client.getObject(templateS3Bucket, templateS3Key);
        S3ObjectInputStream contentStream = templateObject.getObjectContent();

        //  read file to String
        String templateString = null;
        try {
            templateString = Utils.inputStreamToString(contentStream);
            logger.info("Freemarker template: " + templateString);
        }
        catch(IOException ioex)
        {
            //  Bad stuff - blow up!
            logger.error("Problem loading tempate: ", ioex);
            throw ioex;
        }

        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate("GetCapabilitiesTemplate", templateString);
        freemarkerConfig = new Configuration();
        freemarkerConfig.setTemplateLoader(stringLoader);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public String read(Map<String, String> parameters) throws Exception {
        try {
            if (freemarkerConfig == null) {
                return "GetCapabilities template not loaded";
            }

            Template template = freemarkerConfig.getTemplate("GetCapabilitiesTemplate");
            StringWriter writer = new StringWriter();
            template.process(parameters, writer);
            return writer.toString();
        } catch (FileNotFoundException e) {
            logger.error("No template {} found for GetCapabilities", "GetCapabilitiesTemplate");
            throw e;
        } catch (TemplateException|IOException e) {
            logger.error("Error loading GetCapabilities document", e);
            throw e;
        }
    }

}
