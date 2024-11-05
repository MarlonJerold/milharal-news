package com.feedjournal.feedjournal.exception;

public class CustomHttpException extends Exception {
    public CustomHttpException(String message, Exception e) {
        super(message);
    }
}