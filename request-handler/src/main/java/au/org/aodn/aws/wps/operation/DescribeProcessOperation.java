package au.org.aodn.aws.wps.operation;

import au.org.aodn.aws.exception.OGCException;
import au.org.aodn.aws.util.DescribeProcessHelper;
import au.org.aodn.aws.wps.status.WpsConfig;
import net.opengis.ows.v_1_1_0.CodeType;
import net.opengis.wps.v_1_0_0.DescribeProcess;
import net.opengis.wps.v_1_0_0.ProcessDescriptionType;
import net.opengis.wps.v_1_0_0.ProcessDescriptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.List;

public class DescribeProcessOperation implements Operation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescribeProcessOperation.class);
    private final DescribeProcess request;

    public DescribeProcessOperation(DescribeProcess request) {
        this.request = request;
    }

    public DescribeProcess getRequest() {
        return this.request;
    }

    @Override
    public String execute() throws OGCException {

        List<CodeType> identifiers = request.getIdentifier();

        StringBuilder stringBuilder = new StringBuilder();

        if (identifiers != null) {
            ProcessDescriptions outputProcessDescriptions = new ProcessDescriptions();
            outputProcessDescriptions.setLang(WpsConfig.getConfig(WpsConfig.LANGUAGE_KEY));

            for (CodeType identifier : identifiers) {
                //  The identifier passed will be prefixed with 'gs:' - ie: gs:GoGoDuck
                //  We will strip off the gs: part for the purposes of reading the S3 file.
                String processName = identifier.getValue();

                ProcessDescriptionType processDescription = DescribeProcessHelper.getProcessDescription(processName);
                if(processDescription != null)
                {
                    outputProcessDescriptions.getProcessDescription().add(processDescription);
                }
            }

            //  Marshall the output
            try {
                JAXBContext descriptionsContext = JAXBContext.newInstance(ProcessDescriptions.class);
                Marshaller m = descriptionsContext.createMarshaller();
                StringWriter stringWriter = new StringWriter();
                m.marshal(outputProcessDescriptions, stringWriter);
                return stringWriter.toString();
            } catch (Exception ex) {
                LOGGER.error("Error forming process descriptions XML: " + ex.getMessage(), ex);
                throw new OGCException("ProcessingError", "Error forming process descriptions XML: " + ex.getMessage());
            }
        }

        return stringBuilder.toString();
    }
}
