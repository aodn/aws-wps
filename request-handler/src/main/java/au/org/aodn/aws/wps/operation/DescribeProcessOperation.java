package au.org.aodn.aws.wps.operation;

import net.opengis.wps._1_0.DescribeProcess;

import java.util.Properties;

public class DescribeProcessOperation implements Operation {
    private final DescribeProcess request;

    public DescribeProcessOperation(DescribeProcess request) {
        this.request = request;
    }

    @Override
    public Object execute(Properties config) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
