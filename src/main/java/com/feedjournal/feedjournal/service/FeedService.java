package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.model.Feed;
import com.feedjournal.feedjournal.model.FeedItem;
import com.feedjournal.feedjournal.config.HttpHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class FeedService {

    private final HttpHelper httpHelper;

    public FeedService(HttpHelper httpHelper) {
        this.httpHelper = httpHelper;
    }

    public List<FeedItem> getFeed() {
        List<String> feedIds = Arrays.asList(
                "at://did:plc:mup34dteco2xkrzq4xxkkz7h/app.bsky.feed.generator/aaak3fykvnfik",
                "at://did:plc:st5jaaeijn273nmlg56wuktw/app.bsky.feed.generator/aaapf55qisvwa"
        );

        String feedUrl = "https://public.api.bsky.app/xrpc/app.bsky.feed.getFeed";

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            var futures = feedIds.stream()
                    .map(feedId -> executor.submit(() -> fetchFeedItems(feedUrl, feedId)))
                    .collect(Collectors.toList());

            return futures.stream()
                    .flatMap(future -> {
                        try {
                            return future.get().stream();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to fetch feed items", e);
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    private List<FeedItem> fetchFeedItems(String feedUrl, String feedId) {
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
    }
}