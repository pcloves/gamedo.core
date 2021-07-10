package org.gamedo.exception;

public class GameLoopException extends RuntimeException {
    private static final long serialVersionUID = -6981947980551776677L;

    public GameLoopException() {
    }

    public GameLoopException(String message) {
        super(message);
    }

    public GameLoopException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameLoopException(Throwable cause) {
        super(cause);
    }

    public GameLoopException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
