package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.Constants;
import au.org.aodn.aws.wps.exception.ValidationException;
import net.opengis.wps._1_0.GetCapabilities;

import java.util.Properties;

public class GetCapabilitiesOperation implements Operation {
    private final GetCapabilities request;

    public GetCapabilitiesOperation(GetCapabilities request) {
        this.request = request;
    }

    @Override
    public String execute() {
        throw new UnsupportedOperationException(Constants.UNSUPPORTED_METHOD_EXCEPTION_MESSAGE);
    }
}
