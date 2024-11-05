package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.config.HttpHelper;
import com.feedjournal.feedjournal.exception.CustomHttpException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        } catch (CustomHttpException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage());
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
        Logger logger = Logger.getLogger(getClass().getName());

        try {
            Map<String, Object> responseMap = askQuestion(URLFromQueryType(queryType), postText);
            return Boolean.TRUE.equals(responseMap.get(queryType));
        } catch (CustomHttpException e) {
            logger.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }

    public Map<String, Object> askQuestion(String url, String question) throws CustomHttpException {
        Logger logger = Logger.getLogger(getClass().getName());
        try {
            return httpHelper.getStringObjectMap(url, question);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while asking the question", e);
            throw new CustomHttpException("Unexpected error while asking the question", e);
        }
    }
}