package au.org.aodn.aws.wps.request;

import net.opengis.wps.v_1_0_0.DocumentOutputDefinitionType;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.InputType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.org.aodn.aws.wps.exception.ValidationException;

public class ExecuteRequestHelper {
    private final Execute request;

    public ExecuteRequestHelper(Execute request) {
        this.request = request;
    }

    public String getLiteralInputValue(String identifier) {
        for (InputType input: request.getDataInputs().getInput()) {
            if (input.getIdentifier().getValue().equals(identifier)) {
                return input.getData().getLiteralData().getValue();
            }

        }

        return null;
    }

    public boolean hasRequestedOutput(String identifier) {
        for (DocumentOutputDefinitionType output: getResponseFormOutputs()) {
            if (output.getIdentifier().getValue().equals(identifier)) {
                return true;
            }
        }

        return false;
    }

    public void validateInputs() throws ValidationException {
        String layerName = getLiteralInputValue("layer");
        String subset = getLiteralInputValue("subset");
        String email = getEmail();

        if (layerName == null) {
            throw new ValidationException("Request must have a layer name");
        }

        if (subset == null) {
            throw new ValidationException("Request must have a subset");
        } else {
            int latLonCount = 0;
            Pattern latLonPattern = Pattern.compile("([+-]?\\d+\\.?\\d+)\\s*,\\s*([+-]?\\d+\\.?\\d+)");
            Matcher matcher = latLonPattern.matcher(subset);

            while (matcher.find()) {
                latLonCount++;
            }

            if (latLonCount != 2) {
                throw new ValidationException(String.format("Invalid latitude/longitude format for subset: %s", subset));
            }
        }

        if (email == null) {
            throw new ValidationException("Request must have a callback email");
        }
    }

    public String getRequestedMimeType(String identifier) {
        for (DocumentOutputDefinitionType output: getResponseFormOutputs()) {
            if (output.getIdentifier().getValue().equals(identifier)) {
                return output.getMimeType();
            }
        }

        return null;
    }

    private List<DocumentOutputDefinitionType> getResponseFormOutputs() {
        return request.getResponseForm().getResponseDocument().getOutput();
    }

    public String getEmail() {
        String callbackParams = getLiteralInputValue("callbackParams");

        if (callbackParams != null) {
            return callbackParams.substring(callbackParams.indexOf("=") + 1);
        } else {
            return null;
        }
    }
}
