package au.org.emii.util;

import au.org.aodn.aws.util.Utils;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

public class ProvenanceWriter {

    private static final Logger logger = LoggerFactory.getLogger(ProvenanceWriter.class);
    private static final String PROVENANCE_TEMPLATE_NAME = "ProvenanceTemplate";
    private static final String TEMPLATE_DIRECTORY = "/templates";

    public static String write(String templateFile, Map<String, Object> parameters) {

        Configuration config;

        String templatePath = TEMPLATE_DIRECTORY + "/" + templateFile;

        try (InputStream contentStream = ProvenanceWriter.class.getResourceAsStream(templatePath)) {
            //  read file to String
            String templateString = null;
            try {
                templateString = Utils.inputStreamToString(contentStream);
                logger.info("Loaded provenance template.");
            } catch (IOException ioex) {
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
            return "Provenance template '" + PROVENANCE_TEMPLATE_NAME + "' not found";
        } catch (TemplateException | IOException e) {
            logger.error("Error loading provenance document.  Template path [" + templatePath + "]");
            return "Error loading provenance template '" + PROVENANCE_TEMPLATE_NAME + "' not found";
        }
    }
}
