package com.shortTo.urlshortener.exception;


public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String message) {
        super(message);
    }
}
