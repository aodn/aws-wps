package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.exception.ValidationException;

import java.util.Properties;

public interface Operation {
    Object execute(Properties config);
    void validate(Properties config) throws ValidationException;
}
