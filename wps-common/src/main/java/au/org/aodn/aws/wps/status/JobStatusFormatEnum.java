package au.org.aodn.aws.wps.status;

public enum JobStatusFormatEnum {
    XML("application/xml"),
    HTML("text/html"),
    ADMIN("text/html"),
    QUEUE("text/html");

    private final String mimeType;

    JobStatusFormatEnum(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }

}
