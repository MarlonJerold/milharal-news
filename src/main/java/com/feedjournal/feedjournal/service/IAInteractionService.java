package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.config.HttpHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IAInteractionService {

    private final ObjectMapper objectMapper;
    private final HttpHelper httpHelper;

    public IAInteractionService(ObjectMapper objectMapper, HttpHelper httpHelper) {
        this.objectMapper = objectMapper;
        this.httpHelper = httpHelper;
    }

    public List<String> generateStringListFromQueryTypeIA(String postText, String queryType) {
        try {
            Map<String, Object> responseMap = askQuestion(URLFromQueryType(queryType), postText);
            return (List<String>) responseMap.getOrDefault(queryType, List.of());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static String URLFromQueryType(String queryType) {
        if (queryType == null || queryType.isEmpty()) {
            throw new IllegalArgumentException("queryType must not be null or empty");
        }
        return "https://clientgemini.onrender.com/" + queryType;
    }

    public Boolean isPostRelatedToQuery(String postText, String queryType) {
        try {

            Map<String, Object> responseMap = askQuestion(URLFromQueryType(queryType), postText);
            return Boolean.TRUE.equals(responseMap.get(queryType));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> askQuestion(String url, String question) throws Exception {
        return httpHelper.getStringObjectMap(url, question);
    }

}
