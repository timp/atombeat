package org.atombeat.value;

import org.exist.util.CompressedWhitespace;

/**
 * This class provides helper methods and constants for handling whitespace
 */
public class Whitespace {

    private Whitespace() {}



    /**
     * Determine if a string is all-whitespace
     *
     * @param content the string to be tested
     * @return true if the supplied string contains no non-whitespace
     *     characters
     */

    public static boolean isWhite(CharSequence content) {
        if (content instanceof CompressedWhitespace) {
            return true;
        }
        final int len = content.length();
        for (int i=0; i<len;) {
            // all valid XML 1.0 whitespace characters, and only whitespace characters, are <= 0x20
            // But XML 1.1 allows non-white characters that are also < 0x20, so we need a specific test for these
            char c = content.charAt(i++);
            if (c > 32 || !C0WHITE[c]) {
                return false;
            }
        }
        return true;
    }

    private static boolean[] C0WHITE = {
        false, false, false, false, false, false, false, false,  // 0-7
        false, true, true, false, false, true, false, false,     // 8-15
        false, false, false, false, false, false, false, false,  // 16-23
        false, false, false, false, false, false, false, false,  // 24-31
        true                                                     // 32
    };

 
}
