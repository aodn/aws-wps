package au.org.emii.aggregator.overrides;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GlobalAttributeOverrides {
    private List<String> removeAttributes = new ArrayList<>();
    private List<GlobalAttributeOverride> addOrReplaceAttributes = new ArrayList<>();

    public GlobalAttributeOverrides(List<String> removeAttributes, List<GlobalAttributeOverride> addOrReplaceAttributes) {
        this.removeAttributes = removeAttributes;
        this.addOrReplaceAttributes = addOrReplaceAttributes;
    }

    public GlobalAttributeOverrides() {
        this(new ArrayList<String>(), new ArrayList<GlobalAttributeOverride>());
    }

    public List<String> getRemoveAttributes() {
        return removeAttributes;
    }

    public List<GlobalAttributeOverride> getAddOrReplaceAttributes() {
        return addOrReplaceAttributes;
    }

    public boolean isEmpty() {
        return removeAttributes.isEmpty() && addOrReplaceAttributes.isEmpty();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (!removeAttributes.isEmpty()) {
            builder.append("remove - ");
            builder.append(StringUtils.join(removeAttributes, " "));
        }

        if (!addOrReplaceAttributes.isEmpty()) {
            builder.append("override - ");
            List<String> names = new ArrayList<>();

            for (GlobalAttributeOverride override: addOrReplaceAttributes) {
                names.add(override.getName());
            }

            builder.append(StringUtils.join(names, " "));
        }

        return builder.toString();
    }
}
