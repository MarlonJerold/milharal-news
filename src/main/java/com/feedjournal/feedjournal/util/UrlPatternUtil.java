package com.feedjournal.feedjournal.util;

import java.util.regex.Pattern;

public class UrlPatternUtil {

    private static final String URL_REGEX = "https?://[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)|[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}";

    public static Pattern getUrlPattern() {
        return Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);
    }
}
