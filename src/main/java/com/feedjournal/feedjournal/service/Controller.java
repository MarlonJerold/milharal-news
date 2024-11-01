package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.model.Feed;
import com.feedjournal.feedjournal.model.FeedItem;
import com.feedjournal.feedjournal.config.HttpHelper;
import com.feedjournal.feedjournal.model.Post;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/service")
public class Controller {

    private final HttpHelper httpHelper;

    public Controller(HttpHelper httpHelper) {
        this.httpHelper = httpHelper;
    }

    @GetMapping
    public List<FeedItem> getFeed() {
        List<String> feedIds = Arrays.asList(
                "at://did:plc:mup34dteco2xkrzq4xxkkz7h/app.bsky.feed.generator/aaak3fykvnfik",
                "at://did:plc:st5jaaeijn273nmlg56wuktw/app.bsky.feed.generator/aaapf55qisvwa"
        );

        String feedUrl = "https://public.api.bsky.app/xrpc/app.bsky.feed.getFeed";

        List<CompletableFuture<List<FeedItem>>> futures = feedIds.stream()
                .map(feedId -> CompletableFuture.supplyAsync(() -> {
                    List<FeedItem> feedItems = new ArrayList<>();
                    Map<String, String> queryParams = new HashMap<>();
                    queryParams.put("feed", feedId);

                    int attempts = 0;

                    while (attempts < 3) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            String response = httpHelper.get(feedUrl, queryParams, null);
                            Feed feed = mapper.readValue(response, Feed.class);
                            feedItems.addAll(feed.getFeedItems());

                            while (feed.cursor != null) {
                                queryParams.put("cursor", feed.cursor);
                                response = httpHelper.get(feedUrl, queryParams, null);
                                feed = mapper.readValue(response, Feed.class);
                                feedItems.addAll(feed.getFeedItems());
                            }
                            break;
                        } catch (IOException | InterruptedException e) {
                            System.err.println("Erro ao obter o feed para o feedId " + feedId + ": " + e.getMessage());
                            attempts++;
                            if (attempts >= 3) {
                                System.err.println("Número máximo de tentativas atingido para feedId: " + feedId);
                            }
                        }
                    }
                    return feedItems;
                }))
                .collect(Collectors.toList());

        List<FeedItem> feedItems = futures.stream()
                .flatMap(future -> future.join().stream())
                .collect(Collectors.toList());

        return feedItems;
    }

    @GetMapping("/RelevantPosts")
    @Cacheable(value = "getTopRelevantPosts", key = "'postItems'")
    public List<Post> getTopRelevantPosts() {
        List<FeedItem> feedItems = getFeed();

        return feedItems.stream()
                .map(FeedItem::getPost)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(Post::calculateRelevance).reversed())
                .limit(50)
                .collect(Collectors.toList());
    }

    @GetMapping("/opportunity")
    public List<Post> getOpportunity() {
        List<FeedItem> feedItems = getFeed();

        String regex = "\\b(vagas|vaga|oportunidade de emprego|estamos contratando|contratamos|contratando|manda o currículo|manda o curriculo|manda o cv)\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        return feedItems.stream()
                .map(FeedItem::getPost)
                .filter(Objects::nonNull)
                .filter(post -> {
                    String text = post.getText();
                    if (text == null) return false;

                    String finalText = text.toLowerCase().replaceAll("[^a-zA-Z0-9áéíóúãõ ]", " ").trim();
                    Matcher matcher = pattern.matcher(finalText);

                    if (matcher.find()) {
                        return isJobOpportunity(finalText);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public boolean isJobOpportunity(String postText) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            String encodedText = URLEncoder.encode(postText, StandardCharsets.UTF_8);

            String url = "https://clientgemini.onrender.com/verify_opportunity?text=" + encodedText;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> responseBody = mapper.readValue(response.body(), Map.class);

            return Boolean.TRUE.equals(responseBody.get("is_opportunity"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
