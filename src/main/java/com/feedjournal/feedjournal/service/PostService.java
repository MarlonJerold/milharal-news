package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.config.HttpHelper;
import com.feedjournal.feedjournal.model.FeedItem;
import com.feedjournal.feedjournal.model.Post;
import com.feedjournal.feedjournal.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final FeedService feedService;

    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;
    private List<FeedItem> feedItems;
    private final HttpHelper httpHelper;

    public PostService(FeedService feedService, PostRepository postRepository, HttpHelper httpHelper) {
        this.feedService = feedService;
        this.feedItems = feedService.getFeed();
        this.postRepository = postRepository;
        this.httpHelper = httpHelper;
        this.objectMapper = new ObjectMapper();
    }

    public List<Post> getOpportunity() {
        String regex = "\\b(vagas|vaga|oportunidade de emprego|estamos contratando|contratamos|contratando|manda o currículo|manda o curriculo|manda o cv)\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        Set<String> existingPostUris = postRepository.findAll().stream()
                .map(Post::getUri)
                .collect(Collectors.toSet());

        List<Post> opportunityPosts = feedItems.stream()
                .map(FeedItem::getPost)
                .filter(Objects::nonNull)
                .filter(post -> !existingPostUris.contains(post.getUri()))
                .map(post -> {
                    String text = post.getText();
                    if (text == null) return null;

                    String finalText = text.toLowerCase().replaceAll("[^a-zA-Z0-9áéíóúãõ ]", " ").trim();
                    Matcher matcher = pattern.matcher(finalText);

                    if (matcher.find()) {
                        Boolean isOpportunity = isPostRelatedToQueryTest(finalText, "is_opportunity");
                        return isOpportunity ? post : null;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        postRepository.saveAll(opportunityPosts);
        existingPostUris.addAll(opportunityPosts.stream().map(Post::getUri).collect(Collectors.toSet()));

        return postRepository.findAll();
    }

    public List<Post> getTopRelevantPosts() {
        return feedItems.stream()
                .map(FeedItem::getPost)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(Post::calculateRelevance).reversed())
                .limit(50)
                .collect(Collectors.toList());
    }

    public Boolean isPostRelatedToQueryTest(String postText, String queryType) {
        try {

            String url = "https://clientgemini.onrender.com/" + queryType;

            Map<String, String> queryParams = Map.of("text", postText);
            Map<String, String> headers = Map.of("Content-Type", "application/json");

            String request = httpHelper.get(url, queryParams, headers);

            Map<String, Object> responseMap = objectMapper.readValue(request, Map.class);

            return Boolean.TRUE.equals(responseMap.get(queryType));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
