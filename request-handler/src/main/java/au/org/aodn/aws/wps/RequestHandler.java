package au.org.aodn.aws.wps;

import java.util.Properties;

public interface RequestHandler {
    AwsApiResponse handleRequest(AwsApiRequest request, Properties config);
}
