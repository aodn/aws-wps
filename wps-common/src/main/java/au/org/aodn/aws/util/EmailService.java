package au.org.aodn.aws.util;

import au.org.aodn.aws.exception.EmailException;
import au.org.aodn.aws.geoserver.client.SubsetParameters;
import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.util.NumberRange;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class EmailService {

    public static final String DEFAULT_DEPTH_UNITS_LABEL = "m";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private AmazonSimpleEmailService client;
    private EmailTemplateManager templateManager;

    public EmailService() throws Exception {
        Regions region = Regions.fromName(WpsConfig.getProperty(WpsConfig.AWS_REGION_SES_CONFIG_KEY));
        this.client = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(region).build();
        templateManager = new EmailTemplateManager();
    }

    public void sendEmail(String to, String bccAddress,  String from, String subject, String htmlBody, String textBody) {
        try {
            LOGGER.info(String.format("Sending email to %s", to));
            Destination destination = new Destination().withToAddresses(to);

            if(bccAddress != null) {
                ArrayList<String> bccAddresses = new ArrayList();
                bccAddresses.add(bccAddress);
                destination.setBccAddresses(bccAddresses);
            }

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
            LOGGER.info(String.format("Email sent to %s.  Bcc: %s", to, bccAddress));
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to send email. Error message: %s", e.getMessage()), e);
            LOGGER.error(String.format("To: %s", to));
            LOGGER.error(String.format("Bcc: %s", bccAddress));
            LOGGER.error(String.format("From: %s", from));
            LOGGER.error(String.format("Subject: %s", subject));
            LOGGER.error(String.format("Html Body: %s", htmlBody));
            LOGGER.error(String.format("Text Body: %s", textBody));
        }
    }

    public void sendRegisteredJobEmail(String to, String uuid, SubsetParameters subsetParams, String collectionTitle) throws EmailException {
        String subject = WpsConfig.REGISTERED_JOB_EMAIL_SUBJECT + uuid;
        String requestDetail = formatRequestDetail(subsetParams, collectionTitle);
        String textBody = templateManager.getRegisteredEmailContent(uuid, requestDetail);
        String from = WpsConfig.JOB_EMAIL_FROM_ADDRESS;

        sendEmail(to, null, from, subject, null, textBody);
    }

    public void sendCompletedJobEmail(String to, String uuid, String statusPageLink, int expirationPeriodInDays, SubsetParameters subsetParams, String collectionTitle) throws EmailException {
        String subject = WpsConfig.COMPLETED_JOB_EMAIL_SUBJECT + uuid;
        String expirationPeriod = String.format("%d days", expirationPeriodInDays);
        String requestDetail = formatRequestDetail(subsetParams, collectionTitle);

        String textBody = templateManager.getCompletedEmailContent(uuid, expirationPeriod, statusPageLink, requestDetail);
        String from = WpsConfig.JOB_EMAIL_FROM_ADDRESS;

        sendEmail(to, null, from, subject, null, textBody);
    }

    public void sendFailedJobEmail(String to, String bccAddress, String uuid, SubsetParameters subsetParams, String collectionTitle) throws EmailException {
        String subject = WpsConfig.FAILED_JOB_EMAIL_SUBJECT + uuid;
        String requestDetail = formatRequestDetail(subsetParams, collectionTitle);
        String textBody = templateManager.getFailedEmailContent(uuid, requestDetail);
        String from = WpsConfig.JOB_EMAIL_FROM_ADDRESS;

        sendEmail(to, bccAddress, from, subject, null, textBody);
    }


    public static String getRegisteredJobEmailTemplate() {
        return String.format("%s/%s", WpsConfig.EMAIL_TEMPLATES_LOCATION, WpsConfig.REGISTERED_JOB_EMAIL_TEMPLATE_NAME);
    }

    public static String getCompletedJobEmailTemplate() {
        return String.format("%s/%s", WpsConfig.EMAIL_TEMPLATES_LOCATION, WpsConfig.COMPLETED_JOB_EMAIL_TEMPLATE_NAME);
    }

    public static String getFailedJobEmailTemplate() {
        return String.format("%s/%s", WpsConfig.EMAIL_TEMPLATES_LOCATION, WpsConfig.FAILED_JOB_EMAIL_TEMPLATE_NAME);
    }

    public static String formatRequestDetail(SubsetParameters subsetParameters, String collection) {
        String details = "";

        if (subsetParameters != null && collection != null) {

            String spatialStr = portalFormatSpatial(subsetParameters.getBbox());
            String temporalStr = portalFormatTemoral(subsetParameters.getTimeRange());
            String depthStr = portalFormatDepth(subsetParameters.getVerticalRange());

            details = collection != null ? details.concat("Collection: " + collection + '\n') : details;
            details = spatialStr != null ? details.concat(spatialStr + '\n') : details;
            details = temporalStr != null ? details.concat(temporalStr + '\n') : details;
            details = depthStr != null ? details.concat(depthStr + '\n') : details;
        } else {
            details = "(no request parameters are available)";
        }

        return details;
    }

    public static String portalFormatSpatial(LatLonRect bbox) {
        if (bbox != null) {
            String minLon = String.valueOf(bbox.getLonMin());
            String minLat = String.valueOf(bbox.getLatMin());
            String maxLon = String.valueOf(bbox.getLonMax());
            String maxLat = String.valueOf(bbox.getLatMax());

            String spatial = "";

            if (minLon.equals(maxLon) && minLat.equals(maxLat)) {
                spatial = spatial.concat("Timeseries at Lat/Lon: " + minLat + ',' + minLon);
            } else {
                spatial = spatial.concat("Spatial: From Lat/Lon " + minLat + ',' + minLon + " to Lat/Lon " + maxLat + ',' + maxLon);
            }

            return spatial;
        }

        return null;
    }

    public static String formatDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MMM-dd-HH:mm-'UTC'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    public static String portalFormatTemoral(CalendarDateRange timeRange) {
        if (timeRange != null) {
            String startDate = formatDate(timeRange.getStart().toDate());
            String endDate = formatDate(timeRange.getEnd().toDate());

            return "Temporal: " + startDate + " to " + endDate;
        }

        return null;
    }

    public static String portalFormatDepth(NumberRange verticalRange) {
        if (verticalRange != null) {
            return "Depth: " + verticalRange.getMin() + DEFAULT_DEPTH_UNITS_LABEL + " to " + verticalRange.getMax() + DEFAULT_DEPTH_UNITS_LABEL;
        }
        return null;
    }
}
