package com.juxtaflux;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.substring;

/** General string manipulation static functions */
public class StringUtil {
    public static String replaceSpecialChars(String s) {
        s = s.replace("\\n", System.lineSeparator());
        s = s.replace("\\t", "\t");
        s = s.replace("\\r", "\r");
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
    /** Given an arbitrary string, extract the first few words */
    public static String extractInitialWords(String s, int wordCount) {
        s = s.substring(0, Math.min(100, s.length()));
        s = s.replaceAll("[^a-zA-Z0-9 ]", " "); // replace everything but alphanumerics and spaces with spaces
        s = s.trim();
        String[] words = StringUtils.split(s); // merges consecutive delimiters
        List<String> stuff = Arrays.asList(words);
        stuff = stuff.subList(0, Math.min(wordCount, stuff.size()));
        return String.join(" ", stuff);
    }
}



