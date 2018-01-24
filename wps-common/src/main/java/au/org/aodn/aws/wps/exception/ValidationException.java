package au.org.aodn.aws.wps.exception;

public class ValidationException extends Exception {

    public ValidationException()
    {
        super();
    }

    public ValidationException(String message)
    {
        super(message);
    }

    public ValidationException(String message, Throwable ex)
    {
        super(message, ex);
    }

    public ValidationException(Throwable ex)
    {
        super(ex);
    }
}