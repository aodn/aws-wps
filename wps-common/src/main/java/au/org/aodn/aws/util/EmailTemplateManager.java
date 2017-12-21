package au.org.aodn.aws.util;

import au.org.aodn.aws.exception.EmailException;
import au.org.aodn.aws.wps.status.WpsConfig;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Properties;

import static au.org.aodn.aws.util.EmailService.JOB_EMAIL_CONTACT_ADDRESS;

public class EmailTemplateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplateManager.class);

    public static final String UUID = "uuid";
    public static final String JOB_REPORT_URL = "jobReportUrl";
    public static final String EXPIRATION_PERIOD = "expirationPeriod";
    public static final String CONTACT_EMAIL = "contactEmail";

    private VelocityEngine velocityEngine;

    //  TODO:  Convert these to Freemarker - to align all of our templating using one framework
    public EmailTemplateManager() throws Exception {
        Properties p = new Properties();
        p.setProperty("resource.loader", "class");
        p.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        p.setProperty("runtime.log", "/tmp/velocity.log");
        velocityEngine = new VelocityEngine();
        velocityEngine.init(p);
    }


    public String getRegisteredEmailContent(String uuid) throws EmailException {
        try {
            Template t = velocityEngine.getTemplate(EmailService.getRegisteredJobEmailTemplate());
            VelocityContext context = new VelocityContext();
            context.put(UUID, uuid);
            context.put(JOB_REPORT_URL, WpsConfig.getStatusServiceHtmlEndpoint(uuid));
            context.put(CONTACT_EMAIL, JOB_EMAIL_CONTACT_ADDRESS);

            StringWriter writer = new StringWriter();
            t.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            String errorMsg = "Unable to retrieve registered job email content.";
            LOGGER.error(String.format("%s Error Message:", errorMsg), e);
            throw new EmailException(errorMsg, e);
        }
    }

    public String getCompletedEmailContent(String uuid, String expirationPeriod, String statusUrl) throws EmailException {
        try {
            Template t = velocityEngine.getTemplate(EmailService.getCompletedJobEmailTemplate());
            VelocityContext context = new VelocityContext();
            context.put(UUID, uuid);
            context.put(JOB_REPORT_URL, statusUrl);
            context.put(EXPIRATION_PERIOD, expirationPeriod);
            context.put(CONTACT_EMAIL, JOB_EMAIL_CONTACT_ADDRESS);

            StringWriter writer = new StringWriter();
            t.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            String errorMsg = "Unable to retrieve completed job email content.";
            LOGGER.error(String.format("%s Error Message:", errorMsg), e);
            throw new EmailException(errorMsg, e);
        }
    }

    public String getFailedEmailContent(String uuid) throws EmailException {
        try {
            Template t = velocityEngine.getTemplate(EmailService.getFailedJobEmailTemplate());
            VelocityContext context = new VelocityContext();
            context.put(UUID, uuid);
            context.put(JOB_REPORT_URL, WpsConfig.getStatusServiceHtmlEndpoint(uuid));
            context.put(CONTACT_EMAIL, JOB_EMAIL_CONTACT_ADDRESS);

            StringWriter writer = new StringWriter();
            t.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            String errorMsg = "Unable to retrieve failed job email content.";
            LOGGER.error(String.format("%s Error Message:", errorMsg), e);
            throw new EmailException(errorMsg, e);        }
    }
}
