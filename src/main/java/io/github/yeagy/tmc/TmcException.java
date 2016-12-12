package io.github.yeagy.tmc;

public class TmcException extends Exception{
    public TmcException(String message) {
        super(message);
    }

    public TmcException(String message, Throwable cause) {
        super(message, cause);
    }
}
