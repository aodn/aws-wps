package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.Constants;
import net.opengis.wps._1_0.DescribeProcess;

public class DescribeProcessOperation implements Operation {
    private final DescribeProcess request;

    public DescribeProcessOperation(DescribeProcess request) {
        this.request = request;
    }

    @Override
    public String execute() {
        throw new UnsupportedOperationException(Constants.UNSUPPORTED_METHOD_EXCEPTION_MESSAGE);
    }
}
