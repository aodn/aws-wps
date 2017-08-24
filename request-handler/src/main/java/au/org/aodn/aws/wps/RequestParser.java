package au.org.aodn.aws.wps;

import au.org.aodn.aws.wps.exception.InvalidRequestException;
import au.org.aodn.aws.wps.operation.Operation;

public interface RequestParser {
    Operation getOperation() throws InvalidRequestException;
}
