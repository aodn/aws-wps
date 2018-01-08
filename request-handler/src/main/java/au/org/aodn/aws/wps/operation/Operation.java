package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.wps.exception.ValidationException;

public interface Operation {
    String execute() throws OGCException, ValidationException;
}
