package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.Constants;
import au.org.aodn.aws.wps.exception.ValidationException;
import net.opengis.wps._1_0.DescribeProcess;

import java.util.Properties;

public class DescribeProcessOperation implements Operation {
    private final DescribeProcess request;

    public DescribeProcessOperation(DescribeProcess request) {
        this.request = request;
    }

    @Override
    public String execute(Properties config) {
        throw new UnsupportedOperationException(Constants.UNSUPPORTED_METHOD_EXCEPTION_MESSAGE);
    }

    @Override
    public void validate(Properties config) throws ValidationException {
        throw new UnsupportedOperationException(Constants.UNSUPPORTED_METHOD_EXCEPTION_MESSAGE);
    }
}
