package au.org.aodn.aws.wps.operation;


import au.org.aodn.aws.util.Utils;
import freemarker.cache.StringTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.*;
import java.util.Map;


public class GetCapabilitiesReader {

    private static final Logger logger = LoggerFactory.getLogger(GetCapabilitiesReader.class);
    private static final String GET_CAPABILITIES_TEMPLATE = "/templates/GetCapabilities.ftl";

    private final Configuration freemarkerConfig;

    public GetCapabilitiesReader() throws IOException
    {
        try (InputStream inputStream = this.getClass().getResourceAsStream(GET_CAPABILITIES_TEMPLATE)) {
            //  read file to String
            String templateString;
            try {
                templateString = Utils.inputStreamToString(inputStream);
                logger.info("Read freemarker template.");
            } catch (IOException ioex) {
                //  Bad stuff - blow up!
                logger.error("Problem loading template: ", ioex);
                throw ioex;
            }

            StringTemplateLoader stringLoader = new StringTemplateLoader();
            stringLoader.putTemplate("GetCapabilitiesTemplate", templateString);
            freemarkerConfig = new Configuration();
            freemarkerConfig.setTemplateLoader(stringLoader);
            freemarkerConfig.setDefaultEncoding("UTF-8");
            freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        }
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
