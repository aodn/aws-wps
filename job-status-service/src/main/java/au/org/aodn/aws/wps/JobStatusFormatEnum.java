package au.org.aodn.aws.wps;

public enum JobStatusFormatEnum {
    XML("application/xml"),
    HTML("text/html");

    private final String mimeType;

    JobStatusFormatEnum(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }

}
