package au.org.aodn.aws.wps.operation;

import java.util.Properties;

public interface Operation {
    Object execute(Properties config);
}
