package com.juxtaflux;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {
    @Test
    public void replaceSpecialChars() throws Exception {
        assertEquals("", StringUtil.replaceSpecialChars(""));
        assertEquals("a b c", StringUtil.replaceSpecialChars("a b c"));
        assertEquals("\r\n\t abc \r\n\t", StringUtil.replaceSpecialChars("\\n\\t abc \\n\\t"));
    }

    @Test
    public void ltrim() throws Exception {
        assertEquals("", StringUtil.ltrim(""));
        assertEquals("abc", StringUtil.ltrim("abc"));
        assertEquals("abc", StringUtil.ltrim("  abc"));
        assertEquals("abc  ", StringUtil.ltrim("abc  "));
        assertEquals("abc  ", StringUtil.ltrim("  abc  "));
    }

    @Test
    public void rtrim() throws Exception {
        assertEquals("", StringUtil.rtrim(""));
        assertEquals("abc", StringUtil.rtrim("abc"));
        assertEquals("  abc", StringUtil.rtrim("  abc"));
        assertEquals("abc", StringUtil.rtrim("abc  "));
        assertEquals("  abc", StringUtil.rtrim("  abc  "));
    }

    @Test
    public void sliceDegenerate() throws Exception {
        String s = "";
        assertEquals("", StringUtil.slice(s, 5));
        assertEquals("", StringUtil.slice(s, -4));
        assertEquals("", StringUtil.slice(s, 3, 7));
        assertEquals("", StringUtil.slice(s, -8, -2));

        String t = "abc123";
        assertEquals("", StringUtil.slice(t, 4, 2));
        assertEquals("", StringUtil.slice(t, -1, -3));
    }

    @Test
    public void sliceOneIndex() throws Exception {
        String s = "0123456789";
        assertEquals("0", StringUtil.slice(s, 0));
        assertEquals("8", StringUtil.slice(s, 8));
        assertEquals("6", StringUtil.slice(s, -4));
        assertEquals("9", StringUtil.slice(s, -1));
        assertEquals("0", StringUtil.slice(s, -10));
        assertEquals("", StringUtil.slice(s, 999));  // this differs from Python, which throws an IndexError
        assertEquals("", StringUtil.slice(s, -999)); // this differs from Python, which throws an IndexError
    }

    @Test
    public void sliceTwoIndexes() throws Exception {
        String s = "0123456789";
        assertEquals("0123456789", StringUtil.slice(s, null, null));
        assertEquals("56789", StringUtil.slice(s, 5, null));
        assertEquals("01234", StringUtil.slice(s, null, 5));
        assertEquals("789", StringUtil.slice(s, -3, null));
        assertEquals("0123456", StringUtil.slice(s, null, -3));
        assertEquals("34", StringUtil.slice(s, 3, 5));
        assertEquals("012345678", StringUtil.slice(s, 0, 9));
        assertEquals("345678", StringUtil.slice(s, -7, 9));
        assertEquals("78", StringUtil.slice(s, 7, -1));
        assertEquals("45", StringUtil.slice(s, -6, -4));
    }

    @Test
    public void extractInitialWords() {
        assertEquals("clip1", StringUtil.extractInitialWords("clip1", 3));
        assertEquals("this is a", StringUtil.extractInitialWords(" 'this is a test of the emergency broadcast system'", 3));
        assertEquals("public void extractInitialWords", StringUtil.extractInitialWords("  public void extractInitialWords() {", 3));
        assertEquals("token trim equals", StringUtil.extractInitialWords(" (token.trim().equals(\"\")) {", 3));
        assertEquals("", StringUtil.extractInitialWords("   . _ * () \n  \t  ", 3));
        assertEquals("a b c", StringUtil.extractInitialWords("  a ' \"b c\" this isn't as fun as it looks! *a*b*c*", 3));
    }
}


/* Python snippet for comparison
s = '0123456789'
print s[0]
print s[8]
print s[-1]
print s[-10]
#print s[999]  # Index out of range
#print s[-999] # Index out of range
print s[:]
print s[5:]
print s[:5]
print s[-3:]
print s[:-3]
print s[3:5]
print s[0:9]
print s[-7:9]
print s[7:-1]
print s[-6:-4]
print s[5, 3]  # indexes in reverse order
*/