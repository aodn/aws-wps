package au.org.emii.aggregator.template;

import org.apache.commons.lang.text.StrSubstitutor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template for generating a string value from an existing string value using pattern
 * matching and/or a variable map
 *
 * For example,
 *
 *     Map<String, String> variables = new HashMap<>();
 *     variables.put("animal1", "fox");
 *     variables.put("animal2", "dog");
 *     Template template = new Template("The quick brown ${animal1} jumped over the lazy ${animal2}");
 *     System.out.println(template.getValue(variables));
 *
 * prints
 *
 *     The quick brown fox jumped over the lazy dog
 *
 * Also supports using captured parts of a passed string in the template using java regular expressions
 * for example as follows:
 *
 *     Map<String, String> variables = new HashMap<>();
 *     variables.put("currentDate", new Date().toString());
 *     Template template = new Template("(.*, generated on ).*", "${1}${currentDate}");
 *     System.out.println(template.getValue("This quote for Samsung TV LED6200AU was generated on 12/6/12", variables);
 *
 * prints
 *
 *     This quote for Samsung TV LED6200AU was generated on (current date)
 *
 */

public class ValueTemplate {
    private final String template;
    private final Pattern pattern;

    public ValueTemplate(Pattern pattern, String template) {
        this.pattern = pattern;
        this.template = template;
    }

    public ValueTemplate(String template) {
        this(Pattern.compile(".*"), template);
    }

    public String getValue(Map<String, String> valuesMap) {
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        return sub.replace(template);
    }

    public String getValue(String currentValue, Map<String, String> valuesMap) {
        Map<String, String> valuesMapWithCapturedGroups = new LinkedHashMap<>(valuesMap);
        Matcher matcher = pattern.matcher(currentValue);

        if (matcher.matches()) {
            for (int i = 0; i <= matcher.groupCount(); i++) {
                valuesMapWithCapturedGroups.put(Integer.toString(i), matcher.group(i));
            }
            return getValue(valuesMapWithCapturedGroups);
        } else {
            return currentValue;
        }

    }
}
