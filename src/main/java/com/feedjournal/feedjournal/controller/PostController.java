package com.feedjournal.feedjournal.controller;

import com.feedjournal.feedjournal.model.Post;
import com.feedjournal.feedjournal.service.PostService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping()
    @Cacheable(value = "getTopRelevantPosts", key = "'postItems'")
    public List<Post> getTopRelevantPosts() {
        List<Post> topRelevantPosts = postService.getTopRelevantPosts();
        return topRelevantPosts;
    }

    @GetMapping("/opportunity")
    public List<Post> getOpportunity() {
        List<Post> opportunity = postService.getOpportunity();
        return opportunity;
    }

}
