package au.org.aodn.aws.util;

import au.org.aodn.aws.exception.OGCException;
import com.amazonaws.util.StringInputStream;
import net.opengis.wps.v_1_0_0.ProcessDescriptionType;
import net.opengis.wps.v_1_0_0.ProcessDescriptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

public class DescribeProcessHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescribeProcessHelper.class);

    private static final String PROCESS_DESCRIPTION_FILE_EXTENSION = ".xml";

    public static ProcessDescriptionType getProcessDescription(String qualifiedProcessName)
            throws OGCException {

        String processName = qualifiedProcessName;

        if(processName.indexOf(":")!=-1)
        {
            processName = processName.substring(processName.indexOf(":") + 1);
        }

        String processDescription = String.format("/processes/%s%s", processName, PROCESS_DESCRIPTION_FILE_EXTENSION);

        LOGGER.info("Process description: " + processDescription);

        if(DescribeProcessHelper.class.getResource(processDescription) == null)
        {
            throw new OGCException("InvalidParameterValue", "identifier", "No such process '" + processName + "'");
        }

        try (InputStream contentStream = DescribeProcessHelper.class.getResourceAsStream(processDescription)) {
            //  read file to String
            String documentString = Utils.inputStreamToString(contentStream);

            JAXBContext context = JAXBContext.newInstance(ProcessDescriptionType.class);
            Unmarshaller u = context.createUnmarshaller();
            ProcessDescriptions currentProcessDescriptions = (ProcessDescriptions) u.unmarshal(new StringInputStream(documentString));

            if (currentProcessDescriptions.getProcessDescription().size() > 0) {
                for (ProcessDescriptionType currentDescription : currentProcessDescriptions.getProcessDescription()) {
                    if (currentDescription.getIdentifier().getValue().equalsIgnoreCase(qualifiedProcessName)) {
                        //  If the identifier matches the one requested
                        return currentDescription;
                    }
                }
            }

            return null;
        } catch (IOException|JAXBException ex) {
            //  Bad stuff - blow up!
            LOGGER.error("Problem reading XML document for [" + processName + "]", ex);
            throw new OGCException("ProcessingError", "Error retrieving process description: " + ex.getMessage());
        }
    }
}
