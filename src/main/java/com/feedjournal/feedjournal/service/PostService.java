package com.feedjournal.feedjournal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedjournal.feedjournal.config.HttpHelper;
import com.feedjournal.feedjournal.model.FeedItem;
import com.feedjournal.feedjournal.model.Post;
import com.feedjournal.feedjournal.repository.PostRepository;
import com.feedjournal.feedjournal.util.RegexUtil;
import com.feedjournal.feedjournal.util.UrlPatternUtil;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final FeedService feedService;

    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;
    private final IAInteractionService iaInteractionService;
    private List<FeedItem> feedItems;
    private final HttpHelper httpHelper;

    public PostService(FeedService feedService, PostRepository postRepository, IAInteractionService iaInteractionService, HttpHelper httpHelper) {
        this.feedService = feedService;
        this.feedItems = feedService.getFeed();
        this.postRepository = postRepository;
        this.iaInteractionService = iaInteractionService;
        this.httpHelper = httpHelper;
        this.objectMapper = new ObjectMapper();
    }

    public List<Post> getPostByMessage(String postText) {

        Pattern pattern = generateKeywords(postText, 10);

        List<Post> matchingPosts = new ArrayList<>();

        for (FeedItem feedItem : feedItems) {
            Post post = feedItem.getPost();
            if (post != null && post.getText() != null && pattern.matcher(post.getText()).find()) {
                matchingPosts.add(post);
            }
        }
        return matchingPosts;
    }

    public Pattern generateKeywords(String postText, int numberOfGenerations) {
        RegexUtil regexUtil = new RegexUtil(iaInteractionService);
        Pattern initialPattern = regexUtil.createPatternFromKeyIA(postText, "keywords");
        String regexString = initialPattern.pattern();

        String text = "Quero que você pegue essas palavras: " + regexString +
                " quero que sejam palavras chaves para treinar um algorítmo a respeito do gosto em espécifico de um usuário com base nessas perguntas, então imagine que seja um grafos com 2 camadas, uma palavra ligaria a outra, mesmo não sendo 100% de certeza de relação.";

        Pattern lastGeneratedPattern = initialPattern;

        for (int i = 0; i < numberOfGenerations; i++) {

            lastGeneratedPattern = regexUtil.createPatternFromKeyIA(text, "keywords");

            text += " adicionando mais variações com a geração " + (i + 1);
        }

        return lastGeneratedPattern;
    }

    public List<Post> getPostByMessages(String postText) {
        List<Post> relevantPosts = getTopRelevantPosts();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<Post>> futures = relevantPosts.stream()
                    .filter(Objects::nonNull)
                    .map(post -> executor.submit(() -> {
                        if (postText == null) return null;

                        String finalText = postText.toLowerCase().replaceAll("[^a-zA-Z0-9áéíóúãõ ]", " ").trim();
                        return iaInteractionService.isPostRelatedToQuery(finalText, "question") ? post : null;
                    }))
                    .collect(Collectors.toList());

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to process post", e);
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
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
                        Boolean isOpportunity = iaInteractionService.isPostRelatedToQuery(finalText, "is_opportunity");
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
                .limit(100)
                .collect(Collectors.toList());
    }

    public List<Post> getPostGithub() {
        return feedItems.stream()
                .map(FeedItem::getPost)
                .filter(Objects::nonNull)
                .filter(post -> {
                    String text = post.getText();
                    if (text == null) return false;

                    Matcher matcher = UrlPatternUtil.getUrlPattern().matcher(text);
                    while (matcher.find()) {
                        String url = matcher.group();
                        if (url.contains("github.com")) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
}
