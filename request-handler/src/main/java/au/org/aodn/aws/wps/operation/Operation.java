package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.wps.exception.OGCException;

public interface Operation {
    String execute() throws OGCException;
}
