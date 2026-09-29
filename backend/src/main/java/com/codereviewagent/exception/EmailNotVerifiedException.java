package com.codereviewagent.exception;

public class EmailNotVerifiedException extends AuthException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
