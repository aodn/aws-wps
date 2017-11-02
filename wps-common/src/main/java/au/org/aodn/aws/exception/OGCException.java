package au.org.aodn.aws.exception;

public class OGCException extends Throwable {
    private final String exceptionCode;
    private final String locator;
    private final String exceptionText;

    public OGCException(String exceptionCode, String locator, String exceptionText) {
        this.exceptionCode = exceptionCode;
        this.locator = locator;
        this.exceptionText = exceptionText;
    }

    public OGCException(String exceptionCode, String exceptionText) {
        this(exceptionCode, null, exceptionText);
    }

    public String getExceptionCode() {
        return exceptionCode;
    }

    public String getLocator() {
        return locator;
    }

    public String getExceptionText() {
        return exceptionText;
    }

    @Override
    public String getMessage() {
        return String.format("OGCException: exceptionCode='%s' locator='%s' exceptionText='%s'",
                exceptionCode, locator, exceptionText);
    }
}