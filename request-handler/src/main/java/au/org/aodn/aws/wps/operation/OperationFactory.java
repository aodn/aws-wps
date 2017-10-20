package au.org.aodn.aws.wps.operation;

import net.opengis.wps.v_1_0_0.DescribeProcess;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.GetCapabilities;

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

    public static Operation getInstance(String operationName) {
        if (operationName.equalsIgnoreCase("GetCapabilities")) {
            GetCapabilities request = new GetCapabilities();
            return new GetCapabilitiesOperation(request);
        } else if (operationName.equalsIgnoreCase("DescribeProcess")) {
            DescribeProcess request = new DescribeProcess();
            return new DescribeProcessOperation(request);
        } else if (operationName.equalsIgnoreCase("Execute")) {
            throw new IllegalArgumentException("HTTP GET not supported for Execute operation.");
        } else {
            throw new IllegalArgumentException("Unknown request type " + operationName);
        }
    }
}
