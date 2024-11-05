package com.feedjournal.feedjournal.controller;

import com.feedjournal.feedjournal.model.Post;
import com.feedjournal.feedjournal.service.PostService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/github")
    public List<Post> getPostGithub() {
        return postService.getPostGithub();
    }

    @GetMapping()
    public List<Post> getTopRelevantPosts() {
        List<Post> topRelevantPosts = postService.getTopRelevantPosts();
        return topRelevantPosts;
    }

    @GetMapping("/message")
    public List<Post> getPostByMessage(@RequestParam String message) {
        return postService.getPostByMessages(message);
    }

    @GetMapping("/opportunity")
    public List<Post> getOpportunity() {
        List<Post> opportunity = postService.getOpportunity();
        return opportunity;
    }

}
