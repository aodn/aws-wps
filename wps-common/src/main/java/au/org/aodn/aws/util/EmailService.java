package au.org.aodn.aws.util;

import au.org.aodn.aws.exception.EmailException;
import au.org.aodn.aws.wps.status.WpsConfig;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static au.org.aodn.aws.wps.status.WpsConfig.JOB_EMAIL_FROM_EMAIL;

public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private AmazonSimpleEmailService client;
    private EmailTemplateManager templateManager;

    public EmailService() throws Exception {
        Regions region = Regions.fromName(WpsConfig.getProperty(WpsConfig.AWS_REGION_SES_CONFIG_KEY));
        this.client = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(region).build();
        templateManager = new EmailTemplateManager();
    }

    public void sendEmail(String to, String from, String subject, String htmlBody, String textBody) {
        try {
            LOGGER.info(String.format("Sending email to %s", to));
            Destination destination = new Destination().withToAddresses(to);
            Body body = new Body();

            if (htmlBody != null) {
                Content html = new Content()
                        .withCharset("UTF-8").withData(htmlBody);
                body.withHtml(html);
            }

            if (textBody != null) {
                Content text = new Content()
                        .withCharset("UTF-8").withData(textBody);
                body.withText(text);
            }


            Content subjectContent = new Content()
                    .withCharset("UTF-8").withData(subject);

            Message message = new Message()
                    .withBody(body)
                    .withSubject(subjectContent);

            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            destination)
                    .withMessage(message)
                    .withSource(from);
            client.sendEmail(request);
            LOGGER.info(String.format("Email sent to %s", to));
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to send email. Error message: %s", e.getMessage()), e);
            LOGGER.error(String.format("To: %s", to));
            LOGGER.error(String.format("From: %s", from));
            LOGGER.error(String.format("Subject: %s", subject));
            LOGGER.error(String.format("Html Body: %s", htmlBody));
            LOGGER.error(String.format("Text Body: %s", textBody));
        }
    }

    public void sendRegisteredJobEmail(String to, String uuid) throws EmailException {
        String subject = "IMOS download request registered - " + uuid;
        String textBody = templateManager.getRegisteredEmailContent(uuid);
        String from = WpsConfig.getProperty(JOB_EMAIL_FROM_EMAIL);

        sendEmail(to, from, subject, null, textBody);
    }

    public void sendCompletedJobEmail(String to, String uuid, String outputFileLocation, int expirationPeriodInDays) throws EmailException {
        String subject = "IMOS download available - " + uuid;
        String expirationPeriod = String.format("%d days", expirationPeriodInDays);
        String textBody = templateManager.getCompletedEmailContent(uuid, expirationPeriod, outputFileLocation);
        String from = WpsConfig.getProperty(JOB_EMAIL_FROM_EMAIL);

        sendEmail(to, from, subject, null, textBody);
    }

    public void sendFailedJobEmail(String to, String uuid) throws EmailException {
        String subject = "IMOS download error - " + uuid;
        String textBody = templateManager.getFailedEmailContent(uuid);
        String from = WpsConfig.getProperty(JOB_EMAIL_FROM_EMAIL);

        sendEmail(to, from, subject, null, textBody);
    }
}
