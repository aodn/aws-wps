package au.org.aodn.aws.wps;

import java.util.Map;

public class JobStatusRequest {

    private String httpMethod;
    private Map<String, String> headers;
    private Map<String, String> queryStringParameters;

    private boolean isBase64Encoded;

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQueryStringParameters() {
        return queryStringParameters;
    }

    public void setQueryStringParameters(Map<String, String> queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
    }
}
