package au.org.aodn.aws.util;

import net.opengis.ows.v_1_1_0.AnyValue;
import net.opengis.ows.v_1_1_0.CodeType;
import net.opengis.ows.v_1_1_0.LanguageStringType;
import net.opengis.wps.v_1_0_0.*;

import java.math.BigInteger;

public class JAXBUtils {

    public static CodeType getCodeType(String codeSpace, String value) {
        CodeType ct = new CodeType();
        ct.setValue(value);
        ct.setCodeSpace(codeSpace);

        return ct;
    }

    public static LanguageStringType getLanguageStringType(String lang, String value) {
        LanguageStringType titleLayer = new LanguageStringType();
        titleLayer.setLang(lang);
        titleLayer.setValue(value);
        return titleLayer;
    }

    public static InputDescriptionType getInputDescriptionType(BigInteger min, BigInteger max, CodeType ct,
                                                               LanguageStringType title, LanguageStringType ab) {

        LiteralInputType inputType = new LiteralInputType();
        inputType.setAnyValue(new AnyValue());

        InputDescriptionType idt = new InputDescriptionType();
        idt.setMinOccurs(min);
        idt.setMaxOccurs(max);
        idt.setIdentifier(ct);
        idt.setTitle(title);
        idt.setAbstract(ab);
        idt.setLiteralData(inputType);
        return idt;
    }

    public static OutputDescriptionType getOutputDescriptionType(CodeType id, LanguageStringType title, String mineOutput,
                                                                 String[] supportMines) {

        ComplexDataDescriptionType cdd = new ComplexDataDescriptionType();
        cdd.setMimeType(mineOutput);

        ComplexDataCombinationType cdc = new ComplexDataCombinationType();
        cdc.setFormat(cdd);

        ComplexDataCombinationsType support = new ComplexDataCombinationsType();

        for(String s : supportMines) {
            ComplexDataDescriptionType t = new ComplexDataDescriptionType();
            t.setMimeType(s);

            support.getFormat().add(t);
        }

        SupportedComplexDataType ct = new SupportedComplexDataType();
        ct.setDefault(cdc);
        ct.setSupported(support);

        OutputDescriptionType p = new OutputDescriptionType();
        p.setIdentifier(id);
        p.setTitle(title);
        p.setComplexOutput(ct);

        return p;
    }
}
