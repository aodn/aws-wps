package au.org.emii.util;

/**
 * Integer helper methods
 */
public class IntegerHelper {
    public static String suffix(int value) {
        int j = value % 10;
        int k = value % 100;
        if (j == 1 && k != 11) {
            return "st";
        }
        if (j == 2 && k != 12) {
            return "nd";
        }
        if (j == 3 && k != 13) {
            return "rd";
        }
        return "th";
    }

    public static boolean isInteger(String str)
    {
        if(str != null) {
            try {
                Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

}
