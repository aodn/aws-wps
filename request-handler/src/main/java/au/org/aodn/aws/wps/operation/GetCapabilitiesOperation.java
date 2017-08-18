package au.org.aodn.aws.wps.operation;

import net.opengis.wps._1_0.GetCapabilities;

import java.util.Properties;

public class GetCapabilitiesOperation implements Operation {
    private final GetCapabilities request;

    public GetCapabilitiesOperation(GetCapabilities request) {
        this.request = request;
    }

    @Override
    public Object execute(Properties config) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
