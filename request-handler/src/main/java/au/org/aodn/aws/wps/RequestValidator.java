package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.exception.ValidationException;

public interface RequestValidator {
    void validate(AwsApiRequest request) throws ValidationException;
}
