package au.org.aodn.aws.wps;

public interface RequestHandler {
    AwsApiResponse handleRequest(AwsApiRequest request);
}
