package org.kosign.chatbotapi.utils;


import java.text.Normalizer;

public class StringUtils extends org.apache.commons.lang3.StringUtils {
    private static final String ZERO_WIDTH_SPACE = "\u200B";
    private static final String NON_BREAKING_SPACE = "\u00A0";
    public static boolean isNotNullOrEmpty(String ...strs) {
        for (String str : strs) {
            if (str == null || str.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    public static String isNullOrEmptyOrElse(String str, String defaultValue) {
        return isNotNullOrEmpty(str) ? str : defaultValue;
    }
    public static String insertZeroWidthSpaces(String address) {
        if (address == null) {
            return null;
        }
        return address
                .replaceAll("\\b", ZERO_WIDTH_SPACE)  // word boundaries
                .replaceAll("(?<=\\d)\\.(?=\\d)", "." + ZERO_WIDTH_SPACE)  // between numbers with dots (e.g., B.0022)
                .replaceAll("(?<=\\w)-(?=\\w)", "-" + ZERO_WIDTH_SPACE)   // between hyphenated words
                .replace(" ", " " + ZERO_WIDTH_SPACE)  // spaces
//                .replace(",", ZERO_WIDTH_SPACE)  // commas
                .replace("_", ZERO_WIDTH_SPACE)
                .replace(" ", NON_BREAKING_SPACE + ZERO_WIDTH_SPACE);
    }
    public static String cleanupAddress(String address) {
        if (address == null) {
            return null;
        }
        return address
                .replaceAll("\\s+", " ")  // multiple spaces to single space
                .trim();
    }

    public static String removeDiacritics(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
