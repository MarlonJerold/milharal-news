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
        Map<String, String> queryParams = Map.of("text", question);
        Map<String, String> headers = Map.of("Content-Type", "application/json");

        String responseJson = get(url, queryParams, headers);

        return objectMapper.readValue(responseJson, Map.class);
    }

    public static String get(String url, Map<String, String> queryParams, Map<String, String> headers) throws IOException, InterruptedException {
        String fullUrl = buildUrlWithParams(url, queryParams);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(10));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
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