package au.org.emii.aggregator.config;

import au.org.aodn.aggregator.configuration.Attribute;
import au.org.aodn.aggregator.configuration.Template;
import au.org.aodn.aggregator.configuration.Templates;
import au.org.aodn.aggregator.configuration.Variable;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.overrides.VariableAttributeOverride;
import au.org.emii.aggregator.overrides.VariableOverrides;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class AggregationOverridesReader {

    private static Logger logger = LoggerFactory.getLogger(AggregationOverridesReader.class);

    public static AggregationOverrides getAggregationOverrides(String url, String layer) throws AggregationException {
        AggregationOverrides overrides = new AggregationOverrides();

        logger.info("Loading aggregation templates file from url: " + url);

        //  Read config file from S3
        String overrideTemplateDocument = null;
        try  (InputStream inputStream = new URL(url).openStream()) {
            overrideTemplateDocument =  IOUtils.toString(inputStream);

            //  Unmarshall from XML
            JAXBContext context = JAXBContext.newInstance(Templates.class);
            Unmarshaller u = context.createUnmarshaller();

            //  Try and match the template to the layer - apply the specified template if a match found
            Templates templates = (Templates) u.unmarshal(new StringInputStream(overrideTemplateDocument));
            List<Template> templateList = templates.getTemplate();

            //  Config file has 'template' elements which contain 'attributes' and 'variables'
            for (Template currentTemplate : templateList) {
                //  The 'match' attribute on the template can contain a comma-separated list
                //  of match expressions for the template.
                StringTokenizer tokenizer = new StringTokenizer(currentTemplate.getMatch(), ",");
                boolean match = false;
                while (tokenizer.hasMoreTokens()) {
                    //  Apply the first template that matches the layer name
                    if (Pattern.matches(tokenizer.nextToken(), layer)) {
                        match = true;
                        break;
                    }
                }


                //  Next template
                if (match) {
                    logger.info("Template match: Layer [" + layer + "], Template Name [" + currentTemplate.getName() + "], Match [" + currentTemplate.getMatch() + "]");

                    //  Convert the 'attribute' elements into GlobalAttributeOverride objects
                    Template.Attributes srcAttributes = currentTemplate.getAttributes();
                    if (srcAttributes != null) {
                        List<Attribute> srcAttributeList = srcAttributes.getAttribute();
                        for (Attribute currentSrcAttr : srcAttributeList) {

                            String attributeValue = null;
                            if (currentSrcAttr.getValue() != null) {
                                if (currentSrcAttr.getValue().isEmpty() == false) {
                                    //  Use the first value
                                    attributeValue = currentSrcAttr.getValue().get(0);
                                } else {
                                    logger.info("Attribute values empty.");
                                }
                            } else {
                                logger.info("Attribute value null.");
                            }

                            logger.info("[] Applying Attribute Override: Name [" + currentSrcAttr.getName() + "], Type [" + currentSrcAttr.getType() + "], Value [" + attributeValue + "]");

                            GlobalAttributeOverride newOverride = new GlobalAttributeOverride(currentSrcAttr.getName(), DataType.getType(currentSrcAttr.getType()), currentSrcAttr.getMatch(), attributeValue);
                            overrides.getAttributeOverrides().getAddOrReplaceAttributes().add(newOverride);
                        }
                    }


                    //  Convert the 'variable' elements into VariableOverrides objects
                    Template.Variables srcVariables = currentTemplate.getVariables();

                    if (srcVariables != null) {
                        List<Variable> srcVariableList = srcVariables.getVariable();
                        for (Variable currentSrcVariable : srcVariableList) {
                            //  Variables can have attributes
                            List<VariableAttributeOverride> variableAttributeOverrides = new ArrayList<>();
                            List<Attribute> variableAttributes = currentSrcVariable.getAttribute();
                            if (variableAttributes != null && variableAttributes.size() > 0) {

                                //  Attributes can have multiple values
                                for (Attribute currentVariableAttr : variableAttributes) {
                                    VariableAttributeOverride newVariableAttributeOverride = new VariableAttributeOverride(currentVariableAttr.getName(), DataType.getType(currentVariableAttr.getType()), currentVariableAttr.getValue());
                                    variableAttributeOverrides.add(newVariableAttributeOverride);
                                }
                            }

                            logger.info("[] Applying Variable Override: Name [" + currentSrcVariable.getName() + "], Type [" + currentSrcVariable.getType() + "]");
                            if (variableAttributeOverrides != null) {
                                for (VariableAttributeOverride var : variableAttributeOverrides) {
                                    logger.info("    - Attribute override: Name [" + var.getName() + "], Type [" + var.getType() + "], Values [" + var.getValues() + "]");
                                }
                            }

                            VariableOverrides newOverride = null;

                            if (currentSrcVariable.getType() == null) {
                                newOverride = new VariableOverrides(currentSrcVariable.getName());
                            } else {
                                newOverride = new VariableOverrides(currentSrcVariable.getName(), DataType.getType(currentSrcVariable.getType()), variableAttributeOverrides);
                            }

                            overrides.getVariableOverridesList().add(newOverride);
                        }
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            String message = "Unable to download aggregation override configuration file from specified URL : " + url;
            logger.error(message, ex);
            throw new AggregationException(message, ex);
        } catch (JAXBException ex) {
            String message = "Unable to unmarshall aggregation override configuration file.";
            logger.error(message, ex);
            throw new AggregationException(message, ex);
        }

        return overrides;
    }

}
