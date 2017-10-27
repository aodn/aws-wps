package au.org.emii.wps.util;

import net.opengis.ows.v_1_1_0.CodeType;
import net.opengis.wps.v_1_0_0.DataInputsType;
import net.opengis.wps.v_1_0_0.DataType;
import net.opengis.wps.v_1_0_0.DocumentOutputDefinitionType;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.InputType;
import net.opengis.wps.v_1_0_0.LiteralDataType;
import net.opengis.wps.v_1_0_0.ResponseDocumentType;
import net.opengis.wps.v_1_0_0.ResponseFormType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ExecuteRequestBuilder {

    private String identifier;
    private Map<String, String> inputs;
    private Map<String, String> outputs;

    public ExecuteRequestBuilder() {
        inputs = new LinkedHashMap<>();
        outputs = new LinkedHashMap<>();
    }

    public ExecuteRequestBuilder identifer(String value) {
        identifier = value;
        return this;
    }

    public ExecuteRequestBuilder input(String name, String value) {
        inputs.put(name, value);
        return this;
    }

    public ExecuteRequestBuilder output(String name, String mimeType) {
        outputs.put(name, mimeType);
        return this;
    }

    public Execute build() {
        Execute request = new Execute();
        request.setIdentifier(createCodeType(identifier));

        // Add inputs

        DataInputsType dataInputsType = new DataInputsType();

        for (Entry<String, String> input: inputs.entrySet()) {
            InputType inputType = new InputType();
            inputType.setIdentifier(createCodeType(input.getKey()));
            DataType dataType = new DataType();
            LiteralDataType literalDataType = new LiteralDataType();
            literalDataType.setValue(input.getValue());
            dataType.setLiteralData(literalDataType);
            inputType.setData(dataType);
            dataInputsType.getInput().add(inputType);
        }

        request.setDataInputs(dataInputsType);

        // Add requested outputs

        ResponseFormType responseFormType = new ResponseFormType();
        ResponseDocumentType responseDocumentType = new ResponseDocumentType();
        responseDocumentType.setStatus(true);
        responseDocumentType.setStoreExecuteResponse(true);

        for (Entry<String, String> output: outputs.entrySet()) {
            DocumentOutputDefinitionType documentOutputDefinitionType = new DocumentOutputDefinitionType();
            documentOutputDefinitionType.setAsReference(true);
            documentOutputDefinitionType.setMimeType(output.getValue());
            documentOutputDefinitionType.setIdentifier(createCodeType(output.getKey()));
            responseDocumentType.getOutput().add(documentOutputDefinitionType);
        }

        responseFormType.setResponseDocument(responseDocumentType);
        request.setResponseForm(responseFormType);

        return request;
    }

    private CodeType createCodeType(String identifier) {
        CodeType identifierCodeType = new CodeType();
        identifierCodeType.setValue(identifier);
        return identifierCodeType;
    }
}
