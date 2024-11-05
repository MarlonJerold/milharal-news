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
        return "https://clientgemini.onrender.com" + queryType;
    }

    public Boolean isPostRelatedToQuery(String postText, String queryType) {
        final int MAX_RETRIES = 5;
        final int INITIAL_DELAY_MS = 2000;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> responseMap = askQuestion(URLFromQueryType(queryType), postText);
                return Boolean.TRUE.equals(responseMap.get(queryType));
            } catch (IOException e) {
                if (isQuotaExceeded(e)) {
                    try {
                        Thread.sleep(INITIAL_DELAY_MS * (1 << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    e.printStackTrace();
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private boolean isQuotaExceeded(IOException e) {
        return e.getMessage() != null && e.getMessage().contains("429");
    }

    public Map<String, Object> askQuestion(String url, String question) throws Exception {
        return httpHelper.getStringObjectMap(url, question);
    }

}
