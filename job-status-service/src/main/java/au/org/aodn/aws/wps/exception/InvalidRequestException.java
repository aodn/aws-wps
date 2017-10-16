package au.org.aodn.aws.wps.exception;

public class InvalidRequestException extends Exception {
    public InvalidRequestException(Exception e) {
        super(e);
    }
}
