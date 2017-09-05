package au.org.emii.geoserver.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.org.emii.geoserver.client.SubsetParameters.SubsetParameter;

public class SubsetParameters extends HashMap<String, SubsetParameter> {
    public class SubsetParameter {
        public String start;
        public String end;

        public SubsetParameter(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    public SubsetParameters(String subset) {
        super();
        for (String part : subset.split(";")) {
            String[] subsetParts = part.split(",");
            put(subsetParts[0], new SubsetParameter(subsetParts[1], subsetParts[2]));
        }
    }

    // Simple copy ctor
    public SubsetParameters(SubsetParameters sp) {
        super(sp);
    }

}
