package io.github.yeagy.tmc;

public class TmcException extends RuntimeException {
    public TmcException(String message) {
        super(message);
    }

    public TmcException(String message, Throwable cause) {
        super(message, cause);
    }
}
