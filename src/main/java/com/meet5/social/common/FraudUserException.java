package com.meet5.social.common;

public class FraudUserException extends RuntimeException {
    public FraudUserException(Long userId) {
        super("User " + userId + " is flagged as fraud");
    }
}
