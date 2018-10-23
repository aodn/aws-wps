package au.org.emii.util;

public class NumberRange {
    private final Number min;
    private final Number max;

    public NumberRange(Number min, Number max) {
        this.min = min;
        this.max = max;
    }

    public NumberRange(String min, String max) {
        this.min = Double.parseDouble(min);
        this.max = Double.parseDouble(max);
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }
}

