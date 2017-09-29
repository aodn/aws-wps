package au.org.emii.aggregator.au.org.emii.aggregator.config;

import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.dom4j.xpath.DefaultXPath;
//import org.geoserver.catalog.Catalog;
//import org.geoserver.catalog.LayerInfo;
//import org.geoserver.config.GeoServerDataDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Config {

    private Map<String, Document> configFilesDocuments = new HashMap<String, Document>();
    private Logger logger = LoggerFactory.getLogger(Config.class);
    //private GeoServerDataDirectory dataDirectory;
    protected File resourceDirectory;
    //protected Catalog catalog;
    public abstract String getDefaultConfigFile();

    public Config(File resourceDirectory) {
        this.resourceDirectory = resourceDirectory;
        //this.dataDirectory = new GeoServerDataDirectory(resourceDirectory);
        //this.catalog = catalog;
    }

    public String getConfigFilePath(String configFile) throws IOException {
        return FilenameUtils.concat(this.resourceDirectory.getAbsolutePath().toString() + "/", configFile);
    }

    private Document getDocument(String configFile) {
        SAXReader reader = new SAXReader();
        Document doc = null;

        try {
            String configFilePath = getConfigFilePath(configFile);
            if (configFilesDocuments.containsKey(configFilePath)) {
                doc = configFilesDocuments.get(configFilePath);
            } else {
                doc = reader.read(configFilePath);
                configFilesDocuments.put(configFilePath, doc);
            }
        } catch (Exception e) {
            logger.error(String.format("Could not open config file '%s': '%s'", configFile, e.getMessage()), e);
        }
        return doc;
    }

    /* Sample tiny config file:
<ncwms>
<wfsServer>http://localhost:8080/geoserver/ows</wfsServer>
<urlSubstitution key="/mnt/imos-t3/IMOS/opendap/">http://thredds.aodn.org.au/thredds/wms/IMOS/</urlSubstitution>
<urlSubstitution key="^/IMOS/">http://thredds.aodn.org.au/thredds/wms/IMOS/</urlSubstitution>
<collectionsWithTimeMismatch>^imos:srs.*</collectionsWithTimeMismatch>
</ncwms>
*/

    // Example Parameter: "/ncwms/urlSubstitution"
    public Map<String, String> getConfigMap(String xpathString, String configFile) {
        return getConfigMap(xpathString, "key", configFile);
    }

    public Map<String, String> getConfigMap(String xpathString, String attributeName, String configFile) {
        Map<String, String> returnValue = new HashMap<>();
        try {
            Document doc = getDocument(configFile);
            DefaultXPath xpath = new DefaultXPath(xpathString);

            @SuppressWarnings("unchecked")
            List<DefaultElement> list = xpath.selectNodes(doc);

            for (final DefaultElement element : list) {
                returnValue.put(element.attribute(attributeName).getText(), element.getText());
            }
        } catch (Exception e) {
            logger.warn(String.format("Could not open config file '%s': '%s'", configFile, e.getMessage()), e);
        }

        if (returnValue.size() == 0 && isLayerConfigFile(configFile)) {
            return getConfigMap(xpathString, getDefaultConfigFile());
        } else {
            return returnValue;
        }
    }

    public String getConfig(String xpathString, String configFile) {
        List<String> returnValue = getConfigList(xpathString, configFile);
        try {
            return returnValue.get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private boolean isLayerConfigFile(String configFile) {
        return getDefaultConfigFile() != null && !configFile.equals(getDefaultConfigFile());
    }

    // Example Parameter: "/ncwms/collectionsWithTimeMismatch"
    public List<String> getConfigList(String xpathString, String configFile) {
        List<String> returnValue = new ArrayList<>();

        try {
            Document doc = getDocument(configFile);
            DefaultXPath xpath = new DefaultXPath(xpathString);

            @SuppressWarnings("unchecked")
            List<DefaultElement> list = xpath.selectNodes(doc);

            for (final DefaultElement element : list) {
                returnValue.add(element.getText());
            }
        } catch (ClassCastException e) {
            logger.error(String.format("Error reading configuration file %s: '%s' does not return a list of elements", configFile, xpathString), e);
        }

        if (returnValue.size() == 0 && isLayerConfigFile(configFile)) {
            return getConfigList(xpathString, getDefaultConfigFile());
        } else {
            return returnValue;
        }
    }

    /*
    public String getLayerConfigPath(String layer, String configFileName) throws Exception {
        // lookup the layer in the catalog
        LayerInfo layerInfo = catalog.getLayerByName(layer);

        if (layerInfo == null) {
            throw new Exception(String.format("Invalid Layer: %s", layer));
        }

        // Checking config file in layer directory
        String layerDirectory = dataDirectory.get(layerInfo).dir().getAbsolutePath();
        String layerConfigFilePath = layerDirectory + "/" + configFileName;
        if (new File(layerConfigFilePath).exists()) {
            // returning relative path
            return layerConfigFilePath.replace(this.resourceDirectory.getAbsolutePath().toString()+"/","");
        }

        // Checking config file in store directory
        File storeDirectory = dataDirectory.findStoreDir(layerInfo.getResource().getStore());
        String storeConfigFilePath = storeDirectory.getAbsolutePath().toString() + "/" + configFileName;
        if (new File(storeConfigFilePath).exists()) {
            // returning relative path
            return storeConfigFilePath.replace(this.resourceDirectory.getAbsolutePath().toString()+"/","");
        }

        // If config file not present in layer or store directory return default config file
        return getDefaultConfigFile();
    }
    */
}

