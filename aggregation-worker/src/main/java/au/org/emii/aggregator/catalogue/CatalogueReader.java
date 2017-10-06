package au.org.emii.aggregator.catalogue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
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

    public String getMetadataUrl(String layer) {
        try {
            if (catalogueUrl == null || layerSearchField == null) {
                logger.error("Missing configuration: Catalogue URl [" + catalogueUrl + "], Layer search field [" + layerSearchField + "]");
                return "";
            }

            String searchUrl = String.format(CATALOGUE_SEARCH_TEMPLATE, this.catalogueUrl,
                    this.layerSearchField, layer);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document doc;

            try (InputStream inputStream = new URL(searchUrl).openStream()) {
                doc = factory.newDocumentBuilder().parse(inputStream);
            }

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//metadata/link['" + METADATA_PROTOCOL + "']");
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (nl.getLength() == 0) {
                logger.error("No metadata URL found for {}", layer);
                return "";
            }

            String nodeValue = nl.item(0).getTextContent();
            String[] linkInfo = nodeValue.split("\\|");

            if (linkInfo.length < 3) {
                logger.error("Invalid link format for {}", layer);
                return "";
            }

            return linkInfo[2];
        } catch (IOException|SAXException|ParserConfigurationException|XPathExpressionException e) {
            logger.error("Could not retrieve metadata URL for {} from catalogue", layer, e);
            return "";
        }
    }

}

