package au.org.aodn.aws.geonetwork;


import com.amazonaws.util.StringInputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class CatalogueReader {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueReader.class);

    private static final String METADATA_PROTOCOL = "WWW:LINK-1.0-http--metadata-URL";
    private static final String CATALOGUE_SEARCH_TEMPLATE = "%s/srv/eng/xml.search.summary?%s=%s&hitsPerPage=1&fast=index";

    private String catalogueUrl;
    private String layerSearchField;

    public CatalogueReader(String catalogueUrl, String layerSearchField) {
        this.catalogueUrl = catalogueUrl;
        this.layerSearchField = layerSearchField;
    }

    public String getMetadataXML(String layer) throws Exception {

        try {
            if (catalogueUrl == null || layerSearchField == null) {
                logger.error("Missing configuration: Catalogue URL [" + catalogueUrl + "], Layer search field [" + layerSearchField + "]");
                return null;
            }

            logger.info("Layer name: " + layer);

            //  Strip the imos: off the front of the layer name if it is present
            if (layer.startsWith("imos:")) {
                layer = StringUtils.removeStart(layer, "imos:");
                logger.info("Adjusted layer name: " + layer);
            }

            String searchUrl = String.format(CATALOGUE_SEARCH_TEMPLATE, this.catalogueUrl,
                    this.layerSearchField, layer);

            logger.info("Catalogue search URL: " + searchUrl);
            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL(searchUrl);

                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inStream = urlConnection.getInputStream();
                int responseCode = urlConnection.getResponseCode();
                logger.info("HTTP ResponseCode: " + responseCode);

                //  This will always be 0 for https!!
                int bytesAvailable = inStream.available();
                logger.info("Bytes available: [" + bytesAvailable + "]");

                //  Read the input stream
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];

                int bytesRead;
                while((bytesRead = bufferedInputStream.read(buffer)) > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
                baos.flush();

                String content = new String(baos.toByteArray());
                logger.info("Metadata response size: [" + content.length() + "] bytes");

                return content;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        } catch (Exception ex) {
            logger.error("Unable to read metadata XML.", ex);
            throw ex;
        }
    }


    public String getMetadataPointOfTruthUrl(String xmlMetadataRecord) {
        if(xmlMetadataRecord == null) {
            return "";
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new StringInputStream(xmlMetadataRecord));

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//metadata/link['" + METADATA_PROTOCOL + "']");
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (nl.getLength() == 0 || nl.item(0) == null) {
                logger.error("No metadata URL found in XML [{}]. Nodelist empty.", xmlMetadataRecord);
                return "";
            }

            String nodeValue = nl.item(0).getTextContent();

            if(nodeValue == null)
            {
                logger.error("No metadata URL found in XML [{}]. Empty node.", xmlMetadataRecord);
                return "";
            }

            String[] linkInfo = nodeValue.split("\\|");

            if (linkInfo.length < 3) {
                logger.error("Invalid link format in XML [{}]", xmlMetadataRecord);
                return "";
            }

            return linkInfo[2];


        } catch (Exception e) {
            logger.error("Could not retrieve metadata Point Of Truth URL from XML [{}]", xmlMetadataRecord, e);
        }

        return "";
    }

    public String getCollectionTitle(String xmlMetadataRecord) {
        if(xmlMetadataRecord == null) {
            return "";
        }

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new StringInputStream(xmlMetadataRecord));

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//metadata/title");
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (nl.getLength() == 0 || nl.item(0) == null) {
                logger.error("No metadata title found in XML [{}]. Nodelist empty.", xmlMetadataRecord);
                return "";
            }

            String nodeValue = nl.item(0).getTextContent();

            if(nodeValue == null)
            {
                logger.error("No metadata title found in XML [{}]. Empty node.", xmlMetadataRecord);
                return "";
            }

            return nodeValue;

        } catch (Exception e) {
            logger.error("Could not retrieve metadata Point Of Truth URL from XML [{}]", xmlMetadataRecord, e);
        }

        return "";
    }


    //  The metadata response from geoserver includes the metadata record and a summary element in the form:
    //  <response>
    //      <summary>...</summary>
    //      <metadata>...</metadata>
    //  </response>
    //  This method extracts the metadata record.
    public String getMetadataRecord(String xmlMetadataResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new StringInputStream(xmlMetadataResponse));

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//metadata");
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (nl.getLength() == 0 || nl.item(0) == null) {
                logger.error("No metadata title found in XML [{}]. Nodelist empty.", xmlMetadataResponse);
                return "";
            }



            //  Stream out the whole node (including children)
            StringWriter strWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(strWriter);
            Transformer xform = TransformerFactory.newInstance().newTransformer();
            xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xform.setOutputProperty(OutputKeys.INDENT, "yes");
            xform.transform(new DOMSource(nl.item(0)), streamResult);

            strWriter.flush();

            logger.info("Metadata node size [" + strWriter.toString().length() + "]");
            return strWriter.toString();

        } catch(Exception ex) {
            logger.error("Unable to extract metadata element from XML", ex);
        }

        return "";
    }


    public static void main(String[] args) {
        String url = "https://catalogue-portal.aodn.org.au/geonetwork";
        String layerField = "layer";
        String layer = "srs_ghrsst_l3s_1d_day_url";

        CatalogueReader reader = new CatalogueReader(url, layerField);
        try {
            String xml = reader.getMetadataXML(layer);
            System.out.println("XML returned [" + xml + "]");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
