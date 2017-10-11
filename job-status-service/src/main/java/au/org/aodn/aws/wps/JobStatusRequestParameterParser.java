package au.org.aodn.aws.wps;

import net.opengis.ows._1.AcceptVersionsType;
import net.opengis.ows._1.CodeType;
import net.opengis.wps._1_0.DescribeProcess;
import net.opengis.wps._1_0.GetCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class JobStatusRequestParameterParser {

    Logger LOGGER = LoggerFactory.getLogger(JobStatusRequestParameterParser.class);

    private final Map<String, String> queryParameters;

    private final String JOB_ID_PARAMETER_NAME = "jobId";
    private final String FORMAT_PARAMETER_NAME = "format";


    public JobStatusRequestParameterParser(JobStatusRequest request) {
        this.queryParameters = request.getQueryStringParameters();
    }

    public String getJobId() {
        return getMapValueIgnoreCase(JOB_ID_PARAMETER_NAME, queryParameters);
    }

    public String getFormat() {
        return getMapValueIgnoreCase(FORMAT_PARAMETER_NAME, queryParameters);
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
