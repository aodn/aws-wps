package au.org.aodn.aws.wps.request;

import net.opengis.wps.v_1_0_0.DocumentOutputDefinitionType;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.InputType;

import java.util.List;

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
