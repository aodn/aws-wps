package au.org.aodn.aws.geonetwork;


import com.amazonaws.util.StringInputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class CatalogueReader {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueReader.class);

    private static final String METADATA_PROTOCOL = "WWW:LINK-1.0-http--metadata-URL";
    private static final String CATALOGUE_SUMMARY_SEARCH_TEMPLATE = "%s/srv/eng/xml.search.summary?%s=%s&hitsPerPage=1&fast=index";
    private static final String CATALOGUE_METADATA_GET_TEMPLATE = "%s/srv/eng/xml.metadata.get?uuid=%s";

    public static final String METADATA_SUMMARY_UUID_XPATH = "//info/uuid";
    public static final String METADATA_SUMMARY_TITLE_XPATH = "//metadata/title";
    public static final String METADATA_SUMMARY_POINT_OF_TRUTH_XPATH = "//metadata/link['" + METADATA_PROTOCOL + "']";
    public static final String METADATA_SUMMARY_METADATA_ROOT_NODE_XPATH = "//metadata";


    private String catalogueUrl;
    private String layerSearchField;

    public CatalogueReader(String catalogueUrl, String layerSearchField) {
        this.catalogueUrl = catalogueUrl;
        this.layerSearchField = layerSearchField;
    }

    public String getMetadataSummaryXML(String layer) throws Exception {

        try {
            if (catalogueUrl == null || layerSearchField == null) {
                logger.error("Missing configuration: Catalogue URL [" + catalogueUrl + "], Layer search field [" + layerSearchField + "]");
                return null;
            }

            logger.info("Layer name: " + layer);

            String searchUrl = String.format(CATALOGUE_SUMMARY_SEARCH_TEMPLATE, this.catalogueUrl,
                    this.layerSearchField, layer);

            logger.info("Catalogue search URL: " + searchUrl);

            String content = doHttpGet(searchUrl);
            logger.info("Metadata response size: [" + content.length() + "] bytes");

            return content;

        } catch (Exception ex) {
            logger.error("Unable to read metadata XML.", ex);
            throw ex;
        }
    }

    public String getMetadataRecordXML(String uuid) throws Exception {

        try {
            if (catalogueUrl == null) {
                logger.error("Missing configuration: Catalogue URL [" + catalogueUrl + "]");
                return null;
            }

            logger.info("Metadata UUID: " + uuid);

            String getUrl = String.format(CATALOGUE_METADATA_GET_TEMPLATE, this.catalogueUrl, uuid);

            logger.info("Metadata GET URL: " + getUrl);

            String content = doHttpGet(getUrl);
            logger.info("Metadata get response size: [" + content.length() + "] bytes");

            return content;

        } catch (Exception ex) {
            logger.error("Unable to read metadata XML.", ex);
            throw ex;
        }
    }


    public String getMetadataPointOfTruthUrl(String xmlMetadataSummary) {
        if(xmlMetadataSummary == null) { return ""; }

        try {
            String nodeValue = getNodeValue(xmlMetadataSummary, METADATA_SUMMARY_POINT_OF_TRUTH_XPATH);

            String[] linkInfo = nodeValue.split("\\|");
            if (linkInfo.length < 3) {
                logger.error("Invalid link format in XML [{}]", xmlMetadataSummary);
                return "";
            }

            return linkInfo[2];


        } catch (Exception e) {
            logger.error("Could not retrieve metadata Point Of Truth URL from XML [{}]", xmlMetadataSummary, e);
        }

        return "";
    }

    public String getCollectionTitle(String xmlMetadataSummary) {
        if(xmlMetadataSummary == null) { return ""; }

        try {
            return getNodeValue(xmlMetadataSummary, METADATA_SUMMARY_TITLE_XPATH);
        } catch (Exception e) {
            logger.error("Could not retrieve metadata Title from XML [{}]", xmlMetadataSummary, e);
        }

        return "";
    }


    public String getUuid(String xmlMetadataSummary) {
        if(xmlMetadataSummary == null) { return ""; }

        try {
            return getNodeValue(xmlMetadataSummary, METADATA_SUMMARY_UUID_XPATH);
        } catch (Exception e) {
            logger.error("Could not retrieve metadata UUID from XML [{}]", xmlMetadataSummary, e);
        }

        return "";
    }


    //  The metadata response from geonetwork includes the metadata record and a summary element in the form:
    //  <response>
    //      <summary>...</summary>
    //      <metadata>...</metadata>
    //  </response>
    //  This method extracts the metadata record.
    public String getMetadataNodeContent(String xmlMetadataResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new StringInputStream(xmlMetadataResponse));

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(METADATA_SUMMARY_METADATA_ROOT_NODE_XPATH);
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

    private String doHttpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try (InputStream inStream = urlConnection.getInputStream();
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inStream);
             ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

            int responseCode = urlConnection.getResponseCode();
            logger.info("HTTP ResponseCode: " + responseCode);

            //  This will always be 0 for https!!
            int bytesAvailable = inStream.available();
            logger.info("Bytes available: [" + bytesAvailable + "]");

            //  Read the input stream
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = bufferedInputStream.read(buffer)) > 0) {
                byteOutputStream.write(buffer, 0, bytesRead);
            }
            byteOutputStream.flush();

            return byteOutputStream.toString();
        } catch(Exception ex) {
            logger.error("Unable to GET from URL [" + url.toString() + "]: " + ex.getMessage(), ex);
            throw ex;
        }
    }


    private String getNodeValue(String xml, String nodeXpath) throws ParserConfigurationException,
                                                                     IOException,
                                                                     SAXException,
                                                                     XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(new StringInputStream(xml));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(nodeXpath);
        NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        if (nl.getLength() == 0 || nl.item(0) == null) {
            logger.error("No node found with XPath [" + nodeXpath + "] in XML [{}]. Nodelist empty.", xml);
            return "";
        }

        String nodeValue = nl.item(0).getTextContent();

        if (nodeValue == null) {
            logger.error("No node value found with XPath [" + nodeXpath + "]. Empty node.", xml);
            return "";
        }

        return nodeValue;
    }


    public static void main(String[] args) {
        String url = "https://catalogue-portal.aodn.org.au/geonetwork";
        String layerField = "layer";
        String layer = "srs_ghrsst_l3s_1d_day_url";

        CatalogueReader reader = new CatalogueReader(url, layerField);
        try {
            String summaryXml = reader.getMetadataSummaryXML(layer);
            System.out.println("XML returned [" + summaryXml + "]");

            String uuid = reader.getUuid(summaryXml);
            System.out.println("METADATA Title          : " + reader.getCollectionTitle(summaryXml));
            System.out.println("METADATA Point of Truth : " + reader.getMetadataPointOfTruthUrl(summaryXml));
            System.out.println("METADATA UUID           : " + uuid);

            String metadataXML = reader.getMetadataRecordXML(uuid);
            System.out.println("XML returned [" + metadataXML + "]");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
