package org.gamedo.exception;

public class GamedoException extends RuntimeException
{
    private static final long serialVersionUID = 1205212301714968157L;

    public GamedoException() {
    }

    public GamedoException(String message) {
        super(message);
    }

    public GamedoException(String message, Throwable cause) {
        super(message, cause);
    }

    public GamedoException(Throwable cause) {
        super(cause);
    }

    public GamedoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
