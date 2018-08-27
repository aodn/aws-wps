package au.org.aodn.aws.geoserver.client;

public class TimeNotSupportedException extends Exception {

    public TimeNotSupportedException() {
        super();
    }

    public TimeNotSupportedException(String message) {
        super(message);
    }

    public TimeNotSupportedException(Throwable ex) {
        super(ex);
    }

    public TimeNotSupportedException(String message,  Throwable ex) {
        super(message, ex);
    }
}
