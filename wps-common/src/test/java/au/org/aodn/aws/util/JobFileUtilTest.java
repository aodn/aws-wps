package au.org.aodn.aws.util;

import net.opengis.ows.v_1_1_0.CodeType;
import net.opengis.ows.v_1_1_0.IdentificationType;
import net.opengis.ows.v_1_1_0.LanguageStringType;
import net.opengis.wps.v_1_0_0.InputDescriptionType;
import net.opengis.wps.v_1_0_0.ProcessDescriptionType;
import net.opengis.wps.v_1_0_0.ProcessDescriptions;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

/**
 * Due to migrate of JDK11, we need to generate the JAXB classes and hence we need test to make sure the output
 * XML is the same as before.
 */
public class JobFileUtilTest {
    /**
     * Verify the output XML do not contain extra namespace, hence string to string compare is used.
     */
    @Test
    public void testExceptionReportMarshaller() {
        String ex = JobFileUtil.getExceptionReportString("message", "code", "locator");
        assertEquals("The XML matches", ex,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ExceptionReport version=\"1.0.0\" xmlns=\"http://www.opengis.net/ows/1.1\">\n" +
                "    <Exception exceptionCode=\"code\" locator=\"locator\">\n" +
                "        <ExceptionText>message</ExceptionText>\n" +
                "    </Exception>\n" +
                "</ExceptionReport>\n");
    }

    @Test
    public void testProcessDescriptionsMarshaller() {

        ProcessDescriptionType pd = new ProcessDescriptionType();
        ProcessDescriptions pds = new ProcessDescriptions();
        pds.setLang("en-US");

        pds.getProcessDescription().add(pd);

        pd.setStoreSupported(true);
        pd.setStatusSupported(true);
        pd.setProcessVersion("1.0.0");
        pd.setIdentifier(JAXBUtils.getCodeType(null, "gs:GoGoDuck"));
        pd.setTitle(JAXBUtils.getLanguageStringType(null, "GoGoDuck"));
        pd.setAbstract(JAXBUtils.getLanguageStringType(null, "Subset and download gridded collection as NetCDF files"));

        ProcessDescriptionType.DataInputs di = new ProcessDescriptionType.DataInputs();

        di.getInput().add(
                JAXBUtils.getInputDescriptionType(
                        BigInteger.ONE,
                        BigInteger.ONE,
                        JAXBUtils.getCodeType(null, "layer"),
                        JAXBUtils.getLanguageStringType(null, "layer"),
                        JAXBUtils.getLanguageStringType(null, "WFS layer to query"))
        );

        di.getInput().add(
                JAXBUtils.getInputDescriptionType(
                        BigInteger.ONE,
                        BigInteger.ONE,
                        JAXBUtils.getCodeType(null, "subset"),
                        JAXBUtils.getLanguageStringType(null, "subset"),
                        JAXBUtils.getLanguageStringType(null, "Subset, semi-colon separated. Example: TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219")
                )
        );

        di.getInput().add(
                JAXBUtils.getInputDescriptionType(
                        BigInteger.ZERO,
                        BigInteger.ONE,
                        JAXBUtils.getCodeType(null, "callbackUrl"),
                        JAXBUtils.getLanguageStringType(null, "callbackUrl"),
                        JAXBUtils.getLanguageStringType(null, "Callback URL")
                )
        );

        di.getInput().add(
                JAXBUtils.getInputDescriptionType(
                        BigInteger.ZERO,
                        BigInteger.ONE,
                        JAXBUtils.getCodeType(null, "callbackParams"),
                        JAXBUtils.getLanguageStringType(null, "callbackParams"),
                        JAXBUtils.getLanguageStringType(null, "Parameters to append to the callback")
                )
        );
        pd.setDataInputs(di);

        ProcessDescriptionType.ProcessOutputs po = new ProcessDescriptionType.ProcessOutputs();

        po.getOutput().add(
                JAXBUtils.getOutputDescriptionType(
                    JAXBUtils.getCodeType(null, "result"),
                    JAXBUtils.getLanguageStringType(null, "result"),
                    "application/x-netcdf",
                     new String[] {"application/x-netcdf", "text/csv"}
                )
        );

        po.getOutput().add(
                JAXBUtils.getOutputDescriptionType(
                        JAXBUtils.getCodeType(null, "provenance"),
                        JAXBUtils.getLanguageStringType(null, "provenance"),
                        "application/xml",
                        new String[] {"application/xml"}
                )
        );

        pd.setProcessOutputs(po);

        String ex = JobFileUtil.createXmlDocument(pds);

        assertEquals("The XML matches", ex,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<ns3:ProcessDescriptions service=\"WPS\" version=\"1.0.0\" xml:lang=\"en-US\" xmlns:ns2=\"http://www.w3.org/1999/xlink\" xmlns:ns1=\"http://www.opengis.net/ows/1.1\" xmlns:ns3=\"http://www.opengis.net/wps/1.0.0\">\n" +
                        "    <ProcessDescription storeSupported=\"true\" statusSupported=\"true\" ns3:processVersion=\"1.0.0\">\n" +
                        "        <ns1:Identifier>gs:GoGoDuck</ns1:Identifier>\n" +
                        "        <ns1:Title>GoGoDuck</ns1:Title>\n" +
                        "        <ns1:Abstract>Subset and download gridded collection as NetCDF files</ns1:Abstract>\n" +
                        "        <DataInputs>\n" +
                        "            <Input minOccurs=\"1\" maxOccurs=\"1\">\n" +
                        "                <ns1:Identifier>layer</ns1:Identifier>\n" +
                        "                <ns1:Title>layer</ns1:Title>\n" +
                        "                <ns1:Abstract>WFS layer to query</ns1:Abstract>\n" +
                        "                <LiteralData>\n" +
                        "                    <ns1:AnyValue/>\n" +
                        "                </LiteralData>\n" +
                        "            </Input>\n" +
                        "            <Input minOccurs=\"1\" maxOccurs=\"1\">\n" +
                        "                <ns1:Identifier>subset</ns1:Identifier>\n" +
                        "                <ns1:Title>subset</ns1:Title>\n" +
                        "                <ns1:Abstract>Subset, semi-colon separated. Example: TIME,2009-01-01T00:00:00.000Z,2009-12-25T23:04:00.000Z;LATITUDE,-33.433849,-32.150743;LONGITUDE,114.15197,115.741219</ns1:Abstract>\n" +
                        "                <LiteralData>\n" +
                        "                    <ns1:AnyValue/>\n" +
                        "                </LiteralData>\n" +
                        "            </Input>\n" +
                        "            <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                        "                <ns1:Identifier>callbackUrl</ns1:Identifier>\n" +
                        "                <ns1:Title>callbackUrl</ns1:Title>\n" +
                        "                <ns1:Abstract>Callback URL</ns1:Abstract>\n" +
                        "                <LiteralData>\n" +
                        "                    <ns1:AnyValue/>\n" +
                        "                </LiteralData>\n" +
                        "            </Input>\n" +
                        "            <Input minOccurs=\"0\" maxOccurs=\"1\">\n" +
                        "                <ns1:Identifier>callbackParams</ns1:Identifier>\n" +
                        "                <ns1:Title>callbackParams</ns1:Title>\n" +
                        "                <ns1:Abstract>Parameters to append to the callback</ns1:Abstract>\n" +
                        "                <LiteralData>\n" +
                        "                    <ns1:AnyValue/>\n" +
                        "                </LiteralData>\n" +
                        "            </Input>\n" +
                        "        </DataInputs>\n" +
                        "        <ProcessOutputs>\n" +
                        "            <Output>\n" +
                        "                <ns1:Identifier>result</ns1:Identifier>\n" +
                        "                <ns1:Title>result</ns1:Title>\n" +
                        "                <ComplexOutput>\n" +
                        "                    <Default>\n" +
                        "                        <Format>\n" +
                        "                            <MimeType>application/x-netcdf</MimeType>\n" +
                        "                        </Format>\n" +
                        "                    </Default>\n" +
                        "                    <Supported>\n" +
                        "                        <Format>\n" +
                        "                            <MimeType>application/x-netcdf</MimeType>\n" +
                        "                        </Format>\n" +
                        "                        <Format>\n" +
                        "                            <MimeType>text/csv</MimeType>\n" +
                        "                        </Format>\n" +
                        "                    </Supported>\n" +
                        "                </ComplexOutput>\n" +
                        "            </Output>\n" +
                        "            <Output>\n" +
                        "                <ns1:Identifier>provenance</ns1:Identifier>\n" +
                        "                <ns1:Title>provenance</ns1:Title>\n" +
                        "                <ComplexOutput>\n" +
                        "                    <Default>\n" +
                        "                        <Format>\n" +
                        "                            <MimeType>application/xml</MimeType>\n" +
                        "                        </Format>\n" +
                        "                    </Default>\n" +
                        "                    <Supported>\n" +
                        "                        <Format>\n" +
                        "                            <MimeType>application/xml</MimeType>\n" +
                        "                        </Format>\n" +
                        "                    </Supported>\n" +
                        "                </ComplexOutput>\n" +
                        "            </Output>\n" +
                        "        </ProcessOutputs>\n" +
                        "    </ProcessDescription>\n" +
                        "</ns3:ProcessDescriptions>\n");

    }
}
