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

public class EmailTemplateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplateManager.class);

    public static final String UUID = "uuid";
    public static final String JOB_REPORT_URL = "jobReportUrl";
    public static final String SITE_ACRONYM = "siteAcronym";
    public static final String EXPIRATION_PERIOD = "expirationPeriod";
    public static final String EMAIL_SIGNATURE = "emailSignature";
    public static final String CONTACT_EMAIL = "contactEmail";
    public static final String EMAIL_FOOTER = "emailFooter";

    private VelocityEngine velocityEngine;

    public EmailTemplateManager() throws Exception {
        Properties p = new Properties();
        p.setProperty("resource.loader", "class");
        p.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        p.setProperty("runtime.log", "/tmp/velocity.log");
        velocityEngine = new VelocityEngine();
        velocityEngine.init(p);
    }

    public String getRegisteredJobSubject(String uuid) throws EmailException {
        return getSubject(uuid, WpsConfig.getRegisteredJobEmailSubjectTemplate());
    }

    public String getCompletedJobSubject(String uuid) throws EmailException {
        return getSubject(uuid, WpsConfig.getCompletedJobEmailSubjectTemplate());
    }

    public String getFailedJobSubject(String uuid) throws EmailException {
        return getSubject(uuid, WpsConfig.getFailedJobEmailSubjectTemplate());
    }

    private String getSubject(String uuid, String template) throws EmailException {
        try {
            Template t = velocityEngine.getTemplate(template);
            VelocityContext context = new VelocityContext();
            context.put(UUID, uuid);
            StringWriter writer = new StringWriter();
            t.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            String errorMsg = "Unable to retrieve email subject.";
            LOGGER.error(String.format("%s Error Message:", errorMsg), e);
            throw new EmailException(errorMsg, e);
        }
    }

    public String getRegisteredEmailContent(String uuid, String jobReportUrl) throws EmailException {
        try {
            Template t = velocityEngine.getTemplate(WpsConfig.getRegisteredJobEmailTemplate());
            VelocityContext context = new VelocityContext();
            context.put(UUID, uuid);
            context.put(SITE_ACRONYM, WpsConfig.getConfig(WpsConfig.SITE_ACRONYM));
            context.put(JOB_REPORT_URL, jobReportUrl);
            context.put(EMAIL_SIGNATURE, WpsConfig.getConfig(WpsConfig.EMAIL_SIGNATURE));
            context.put(CONTACT_EMAIL, WpsConfig.getConfig(WpsConfig.CONTACT_EMAIL));
            context.put(EMAIL_FOOTER, WpsConfig.getConfig(WpsConfig.EMAIL_FOOTER));

            StringWriter writer = new StringWriter();
            t.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            String errorMsg = "Unable to retrieve registered job email content.";
            LOGGER.error(String.format("%s Error Message:", errorMsg), e);
            throw new EmailException(errorMsg, e);
        }
    }

    public String getCompletedEmailContent(String uuid, String jobReportUrl, String expirationPeriod) throws EmailException {
        try {
            Template t = velocityEngine.getTemplate(WpsConfig.getCompletedJobEmailTemplate());
            VelocityContext context = new VelocityContext();
            context.put(UUID, uuid);
            context.put(SITE_ACRONYM, WpsConfig.getConfig(WpsConfig.SITE_ACRONYM));
            context.put(JOB_REPORT_URL, jobReportUrl);
            context.put(EXPIRATION_PERIOD, expirationPeriod);
            context.put(EMAIL_SIGNATURE, WpsConfig.getConfig(WpsConfig.EMAIL_SIGNATURE));
            context.put(CONTACT_EMAIL, WpsConfig.getConfig(WpsConfig.CONTACT_EMAIL));
            context.put(EMAIL_FOOTER, WpsConfig.getConfig(WpsConfig.EMAIL_FOOTER));

            StringWriter writer = new StringWriter();
            t.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            String errorMsg = "Unable to retrieve completed job email content.";
            LOGGER.error(String.format("%s Error Message:", errorMsg), e);
            throw new EmailException(errorMsg, e);
        }
    }

    public String getFailedEmailContent(String uuid, String jobReportUrl) throws EmailException {
        try {
            Template t = velocityEngine.getTemplate(WpsConfig.getFailedJobEmailTemplate());
            VelocityContext context = new VelocityContext();
            context.put(UUID, uuid);
            context.put(JOB_REPORT_URL, jobReportUrl);
            context.put(EMAIL_SIGNATURE, WpsConfig.getConfig(WpsConfig.EMAIL_SIGNATURE));
            context.put(CONTACT_EMAIL, WpsConfig.getConfig(WpsConfig.CONTACT_EMAIL));
            context.put(EMAIL_FOOTER, WpsConfig.getConfig(WpsConfig.EMAIL_FOOTER));

            StringWriter writer = new StringWriter();
            t.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            String errorMsg = "Unable to retrieve failed job email content.";
            LOGGER.error(String.format("%s Error Message:", errorMsg), e);
            throw new EmailException(errorMsg, e);        }
    }
}
