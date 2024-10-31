package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.model.Feed;
import com.feedjournal.feedjournal.model.FeedItem;
import com.feedjournal.feedjournal.config.HttpHelper;
import com.feedjournal.feedjournal.model.Post;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

}
