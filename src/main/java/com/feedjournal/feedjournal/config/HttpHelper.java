package com.feedjournal.feedjournal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class HttpHelper {

    private static HttpClient httpClient = null;
    private final ObjectMapper objectMapper;

    public HttpHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }


    public Map<String, Object> getStringObjectMap(String url, String question) throws IOException, InterruptedException {
        Logger logger = Logger.getLogger(getClass().getName());

        Map<String, String> queryParams = Map.of("text", question);
        Map<String, String> headers = Map.of("Content-Type", "application/json");

        try {
            String responseJson = get(url, queryParams, headers);
            return objectMapper.readValue(responseJson, Map.class);
        } catch (IOException e) {

            logger.log(Level.SEVERE, "Failed to retrieve or parse data from URL: " + url, e);
            throw new IOException("Error retrieving or parsing data from URL: " + url, e);
        } catch (InterruptedException e) {

            logger.log(Level.WARNING, "Request interrupted while retrieving data from URL: " + url, e);
            Thread.currentThread().interrupt();
            throw new InterruptedException("Request interrupted while retrieving data from URL: " + url);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while getting data from the URL", e);
            throw new RuntimeException("Unexpected error while getting data from the URL", e);
        }
    }

    public static String get(String url, Map<String, String> queryParams, Map<String, String> headers) throws IOException, InterruptedException {
        Logger logger = Logger.getLogger(HttpHelper.class.getName());
        String fullUrl = buildUrlWithParams(url, queryParams);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(10));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {

                String errorMsg = "Error: " + response.statusCode() + " - " + response.body();
                logger.log(Level.SEVERE, errorMsg);
                throw new IOException(errorMsg);
            }
            return response.body();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException occurred while sending GET request to URL: " + fullUrl, e);
            throw new IOException("IOException while sending GET request to URL: " + fullUrl, e);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "GET request was interrupted", e);
            Thread.currentThread().interrupt();
            throw new InterruptedException("GET request interrupted for URL: " + fullUrl);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while sending GET request", e);
            throw new RuntimeException("Unexpected error while sending GET request to URL: " + fullUrl, e);
        }
    }

    private static String buildUrlWithParams(String url, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            joiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return url + "?" + joiner.toString();
    }

}