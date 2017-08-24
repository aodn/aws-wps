package au.org.aodn.aws.wps.operation;

import net.opengis.wps._1_0.DescribeProcess;
import net.opengis.wps._1_0.Execute;
import net.opengis.wps._1_0.GetCapabilities;

public class OperationFactory {
    public static Operation getInstance(Object request) {
        if (request instanceof Execute) {
            return new ExecuteOperation((Execute) request);
        } else if (request instanceof DescribeProcess) {
            return new DescribeProcessOperation((DescribeProcess) request);
        } else if (request instanceof GetCapabilities) {
            return new GetCapabilitiesOperation((GetCapabilities) request);
        } else {
            throw new IllegalArgumentException("Unknown request type " + request.getClass().getName());
        }
    }
}
