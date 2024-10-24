package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.model.Feed;
import com.feedjournal.feedjournal.model.FeedItem;
import com.feedjournal.feedjournal.config.HttpHelper;
import com.feedjournal.feedjournal.model.Post;
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
    public List<FeedItem> getFeed() throws IOException, InterruptedException {
        List<String> feedIds = Arrays.asList(
                "at://did:plc:mup34dteco2xkrzq4xxkkz7h/app.bsky.feed.generator/aaak3fykvnfik",
                "at://did:plc:st5jaaeijn273nmlg56wuktw/app.bsky.feed.generator/aaapf55qisvwa"
        );

        List<FeedItem> feedItems = new ArrayList<>();
        String feedUrl = "https://public.api.bsky.app/xrpc/app.bsky.feed.getFeed";

        for (String feedId : feedIds) {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("feed", feedId);

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
        }

        return feedItems;
    }

    @GetMapping("/RelevantPotopsts")
    public List<Post> getTopRelevantPosts() throws IOException, InterruptedException {
        List<FeedItem> feedItems = getFeed();

        return feedItems.stream()
                .map(FeedItem::getPost)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(Post::calculateRelevance).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

}
