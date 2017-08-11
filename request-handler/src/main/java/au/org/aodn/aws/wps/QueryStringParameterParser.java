package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.operation.Operation;

import java.util.Map;

public class QueryStringParameterParser implements RequestParser {
    private final Map<String, String> queryParameters;

    public QueryStringParameterParser(AwsApiRequest request) {
        this.queryParameters = request.getQueryStringParameters();
    }

    @Override
    public Operation getOperation() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
