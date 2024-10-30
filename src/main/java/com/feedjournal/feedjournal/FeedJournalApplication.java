package com.feedjournal.feedjournal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.io.IOException;

@SpringBootApplication
@EnableCaching
public class FeedJournalApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(FeedJournalApplication.class, args);
    }

}
