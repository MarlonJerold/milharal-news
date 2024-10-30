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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/service")
public class Controller {

    private final HttpHelper httpHelper;

    public Controller(HttpHelper httpHelper) {
        this.httpHelper = httpHelper;
    }

    @GetMapping
    @Cacheable(value = "feed_cache", key = "'feedItems'")
    public List<FeedItem> getFeed() {
        List<String> feedIds = Arrays.asList(
                "at://did:plc:mup34dteco2xkrzq4xxkkz7h/app.bsky.feed.generator/aaak3fykvnfik",
                "at://did:plc:st5jaaeijn273nmlg56wuktw/app.bsky.feed.generator/aaapf55qisvwa"
        );

        List<FeedItem> feedItems = new ArrayList<>();
        String feedUrl = "https://public.api.bsky.app/xrpc/app.bsky.feed.getFeed";

        for (String feedId : feedIds) {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("feed", feedId);

            try {
                String response = httpHelper.get(feedUrl, queryParams, null);
                ObjectMapper mapper = new ObjectMapper();
                Feed feed = mapper.readValue(response, Feed.class);

                feedItems.addAll(feed.getFeedItems());

                while (feed.cursor != null) {
                    queryParams.put("cursor", feed.cursor);
                    response = httpHelper.get(feedUrl, queryParams, null);
                    feed = mapper.readValue(response, Feed.class);
                    feedItems.addAll(feed.getFeedItems());
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Erro ao obter o feed para o feedId " + feedId + ": " + e.getMessage());
            }
        }

        return feedItems;
    }

    @CircuitBreaker(name = "backendService", fallbackMethod = "fallbackMethod")
    @GetMapping("/RelevantPosts")
    public List<Post> getTopRelevantPosts() {
        List<FeedItem> feedItems;

        try {
            feedItems = getFeed();
        } catch (Exception e) {
            return fallbackMethod(e);
        }

        return feedItems.stream()
                .map(FeedItem::getPost)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(Post::calculateRelevance).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    public List<Post> fallbackMethod(Throwable t) {
        System.err.println("Circuit Breaker acionado: " + t.getMessage());
        return Collections.emptyList();
    }

}
