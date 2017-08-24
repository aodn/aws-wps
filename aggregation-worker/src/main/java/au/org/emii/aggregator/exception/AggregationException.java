package au.org.emii.aggregator.exception;

/**
 * Created by craigj on 12/01/17.
 */
public class AggregationException extends Exception {

    public AggregationException(Throwable t) {
        super(t);
    }

    public AggregationException(String message, Throwable t) {
        super(message, t);
    }

    public AggregationException(String s) {
        super(s);
    }
}
