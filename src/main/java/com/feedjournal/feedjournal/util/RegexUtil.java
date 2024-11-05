package com.feedjournal.feedjournal.util;

import com.feedjournal.feedjournal.service.IAInteractionService;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexUtil {

    private final IAInteractionService iaInteractionService;

    public RegexUtil(IAInteractionService iaInteractionService) {
        this.iaInteractionService = iaInteractionService;
    }

    public Pattern createPatternFromKeyIA(String postText, String key) {
        List<String> keywords = iaInteractionService.generateStringListFromQueryTypeIA(postText, key);
        String regex = buildRegexFromKeywords(keywords);
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public static String buildRegexFromKeywords(List<String> keywords) {

        String joinedKeywords = keywords.stream()
                .map(keyword -> keyword.replaceAll("[\\[\\](){}?*+.$|^]", "\\\\$0"))
                .collect(Collectors.joining("|"));

        return "\\b(" + joinedKeywords + ")\\b";
    }
}
