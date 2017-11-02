package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.exception.OGCException;

public interface Operation {
    String execute() throws OGCException;
}
