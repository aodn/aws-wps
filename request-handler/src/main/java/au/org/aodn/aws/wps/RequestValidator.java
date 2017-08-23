package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.exception.ValidationException;

import java.util.Properties;

public interface RequestValidator {
    void validate(AwsApiRequest request, Properties config) throws ValidationException;
}
