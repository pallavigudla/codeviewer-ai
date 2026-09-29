package com.codereviewagent.exception;

public class InvalidOtpException extends AuthException {
    public InvalidOtpException(String message) {
        super(message);
    }
}
