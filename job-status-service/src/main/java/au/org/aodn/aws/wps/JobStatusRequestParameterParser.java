package au.org.aodn.aws.wps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static au.org.aodn.aws.wps.status.WpsConfig.STATUS_SERVICE_FORMAT_PARAMETER_NAME;
import static au.org.aodn.aws.wps.status.WpsConfig.STATUS_SERVICE_JOB_ID_PARAMETER_NAME;

public class JobStatusRequestParameterParser {

    Logger LOGGER = LoggerFactory.getLogger(JobStatusRequestParameterParser.class);

    private final Map<String, String> queryParameters;


    public JobStatusRequestParameterParser(JobStatusRequest request) {
        this.queryParameters = request.getQueryStringParameters();
    }

    public String getJobId() {
        return getMapValueIgnoreCase(STATUS_SERVICE_JOB_ID_PARAMETER_NAME, queryParameters);
    }

    public String getFormat() {
        return getMapValueIgnoreCase(STATUS_SERVICE_FORMAT_PARAMETER_NAME, queryParameters);
    }

    private String getMapValueIgnoreCase(String searchKey, Map<String, String> map)
    {
        //  Look for uppercase or lowercase or mixed case matches
        if(searchKey != null && map != null) {
            Set<String> keys = map.keySet();
            for(String key : keys)
            {
                if(key.toLowerCase().equalsIgnoreCase(searchKey.toLowerCase()))
                {
                    return map.get(key);
                }
            }
        }
        return null;
    }
}
