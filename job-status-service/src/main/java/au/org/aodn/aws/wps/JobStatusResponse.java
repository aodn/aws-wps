package au.org.aodn.aws.wps;

import java.util.LinkedHashMap;
import java.util.Map;

public class JobStatusResponse {
    private boolean isBase64Encoded;
    private int statusCode;
    private Map<String, String> headers;
    private String body;

    JobStatusResponse(boolean isBase64Encoded, int statusCode, Map<String, String> headers, String body) {
        this.isBase64Encoded = isBase64Encoded;
        this.statusCode = statusCode;
        this.headers = new LinkedHashMap<>(headers);
        this.body = body;
    }

    public boolean isBase64Encoded() {
        return isBase64Encoded;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public static class ResponseBuilder {
        private boolean isBase64Encoded;
        private int statusCode;
        private Map<String, String> headers;
        private String body;


        public ResponseBuilder() {
            isBase64Encoded = false;
            statusCode = 200;
            headers = new LinkedHashMap<>();
            body = "";
        }

        public ResponseBuilder isBase64Encoded(boolean value) {
            isBase64Encoded = value;
            return this;
        }

        public ResponseBuilder statusCode(int value) {
            statusCode = value;
            return this;
        }

        public ResponseBuilder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public ResponseBuilder body(String value) {
            body = value;
            return this;
        }

        public JobStatusResponse build() {
            return new JobStatusResponse(isBase64Encoded, statusCode, headers, body);
        }
    }
}
