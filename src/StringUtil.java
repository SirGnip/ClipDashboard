import static org.apache.commons.lang3.StringUtils.substring;

/** General string manipulation static functions */
public class StringUtil {
    public static String replaceSpecialChars(String s) {
        s = s.replace("\\n", "\n");
        s = s.replace("\\t", "\t");
        return s;
    }
    // Reference: http://stackoverflow.com/a/15567181
    public static String ltrim(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }
    public static String rtrim(String s) {
        int i = s.length()-1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return s.substring(0,i+1);
    }
    /** Implements Python-ish slice functionality */
    public static String slice(String s, int start) {
        Integer endIdx = start + 1;
        if (endIdx == 0) {
            endIdx = s.length();
        }
        return substring(s, start, endIdx);
    }
    public static String slice(String s, Integer start, Integer end) {
        if (start == null) {
            start = 0;
        }
        if (end == null) {
            end = s.length();
        }
        return substring(s, start, end);
    }
}
