package au.org.emii.aggregator.au.org.emii.aggregator.config;

import au.org.aodn.aggregator.configuration.Attribute;
import au.org.aodn.aggregator.configuration.Template;
import au.org.aodn.aggregator.configuration.Templates;
import au.org.aodn.aggregator.configuration.Variable;
import au.org.aodn.aws.util.S3Utils;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.GlobalAttributeOverride;
import au.org.emii.aggregator.overrides.VariableAttributeOverride;
import au.org.emii.aggregator.overrides.VariableOverrides;
import com.amazonaws.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class AggregationOverridesReader {

    private static Logger logger = LoggerFactory.getLogger(AggregationOverridesReader.class);

    public static AggregationOverrides getAggregationOverrides(String s3Bucket, String s3Key, String s3Region, String layer)
    {
        AggregationOverrides overrides = new AggregationOverrides();

        logger.info("Loading aggregation templates file from S3.  Bucket [" + s3Bucket + "], Key [" + s3Key + "], Region [" + s3Region + "]");

        //  Read config file from S3
        String overrideTemplateDocument = null;
        try
        {
            overrideTemplateDocument = S3Utils.readS3ObjectAsString(s3Bucket, s3Key, s3Region);

            //  Unmarshall from XML
            JAXBContext context = JAXBContext.newInstance(Templates.class);
            Unmarshaller u = context.createUnmarshaller();

            //  Try and match the template to the layer - apply the specified template if a match found
            Templates templates = (Templates) u.unmarshal(new StringInputStream(overrideTemplateDocument));
            List<Template> templateList = templates.getTemplate();

            //  Config file has 'template' elements which contain 'attributes' and 'variables'
            for(Template currentTemplate : templateList)
            {
                //  The 'match' attribute on the template can contain a comma-separated list
                //  of match expressions for the template.
                StringTokenizer tokenizer = new StringTokenizer(currentTemplate.getMatch(), ",");
                boolean match = false;
                while(tokenizer.hasMoreTokens()) {
                    //  Apply the first template that matches the layer name
                    if (Pattern.matches(tokenizer.nextToken(), layer)) {
                        match = true;
                        break;
                    }
                }

                logger.info("Template match [" + match + "]: Layer [" + layer + "], Template Name [" + currentTemplate.getName() + "], Match [" + currentTemplate.getMatch() + "]");

                //  Next template
                if(!match)
                {
                    continue;
                }

                //  Convert the 'attribute' elements into GlobalAttributeOverride objects
                Template.Attributes srcAttributes = currentTemplate.getAttributes();
                if(srcAttributes != null) {
                    List<Attribute> srcAttributeList = srcAttributes.getAttribute();
                    for (Attribute currentSrcAttr : srcAttributeList) {

                        String attributeValue = null;
                        if (currentSrcAttr.getValue() != null)
                        {
                            if(currentSrcAttr.getValue().isEmpty() == false) {
                                //  Use the first value
                                attributeValue = currentSrcAttr.getValue().get(0);
                            }
                            else
                            {
                                logger.info("Attribute values empty.");
                            }
                        }
                        else
                        {
                            logger.info("Attribute value null.");
                        }

                        logger.info("Applying Attribute Override: Name [" + currentSrcAttr.getName() + "], Type [" + currentSrcAttr.getType() + "], Value [" + attributeValue + "]");

                        GlobalAttributeOverride newOverride = new GlobalAttributeOverride(currentSrcAttr.getName(), DataType.getType(currentSrcAttr.getType()), currentSrcAttr.getMatch(), attributeValue);
                        overrides.getAttributeOverrides().getAddOrReplaceAttributes().add(newOverride);
                    }
                }


                //  Convert the 'variable' elements into VariableOverrides objects
                Template.Variables srcVariables = currentTemplate.getVariables();

                if(srcVariables != null)
                {
                    List<Variable> srcVariableList = srcVariables.getVariable();
                    for (Variable currentSrcVariable : srcVariableList) {
                        //  Variables can have attributes
                        List<VariableAttributeOverride> variableAttributeOverrides = null;
                        List<Attribute> variableAttributes = currentSrcVariable.getAttribute();
                        if (variableAttributes != null && variableAttributes.size() > 0) {
                            variableAttributeOverrides = new ArrayList<>();
                            //  Attributes can have multiple values
                            for (Attribute currentVariableAttr : variableAttributes) {
                                VariableAttributeOverride newVariableAttributeOverride = new VariableAttributeOverride(currentVariableAttr.getName(), DataType.getType(currentVariableAttr.getType()), currentVariableAttr.getValue());
                                variableAttributeOverrides.add(newVariableAttributeOverride);
                            }
                        }

                        logger.info("Applying Variable Override: Name [" + currentSrcVariable.getName() + "], Type [" + currentSrcVariable.getType() + "]");
                        for(VariableAttributeOverride var : variableAttributeOverrides)
                        {
                            logger.info("    - Attribute override: Name [" + var.getName() + "], Type [" + var.getType() + "], Values [" + var.getValues() + "]");
                        }

                        VariableOverrides newOverride = new VariableOverrides(currentSrcVariable.getName(), DataType.getType(currentSrcVariable.getType()), variableAttributeOverrides);
                        overrides.getVariableOverridesList().add(newOverride);
                    }
                }
            }
        }
        catch(IOException ex)
        {
            logger.error("Unable to load aggregations override file from S3.", ex);
        }
        catch(JAXBException ex)
        {
            logger.error("Unable to unmarshall XML from aggregations override file.", ex);
        }

        return overrides;
    }

}