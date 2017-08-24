package au.org.emii.aggregator.overrides;

import au.org.emii.aggregator.overrides.GlobalAttributeOverrideConverter;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import ucar.ma2.DataType;

import java.util.regex.Pattern;

/**
 * Created by craigj on 1/03/17.
 */
@XStreamAlias("attribute")
@XStreamConverter(GlobalAttributeOverrideConverter.class)
public class GlobalAttributeOverride {
    private String name;
    private DataType type;
    private final Pattern pattern;
    private String value;

    public GlobalAttributeOverride(String name, DataType type, String match, String value) {
        this.name = name;
        this.type = type;
        this.pattern = match == null ? Pattern.compile(".*") : Pattern.compile(match);
        this.value = value;
    }

    public GlobalAttributeOverride(String name, String match, String value) {
        this(name, DataType.STRING, match, value);
    }

    public GlobalAttributeOverride(String name, String value) {
        this(name, DataType.STRING, ".*", value);
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return String.format("name=%s, type=%s, match=%s, value=%s", name, type, pattern.pattern(), value);
    }
}
