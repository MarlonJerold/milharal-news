package com.feedjournal.feedjournal.repository;

import com.feedjournal.feedjournal.model.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    boolean existsByUri(String uri);
}
