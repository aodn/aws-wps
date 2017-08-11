package au.org.emii.aggregator.exception;

/**
 * Created by craigj on 12/01/17.
 */
public class SubsetException extends Exception {

    public SubsetException(Throwable t) {
        super(t);
    }

    public SubsetException(String message, Throwable t) {
        super(message, t);
    }

    public SubsetException(String s) {
        super(s);
    }
}
