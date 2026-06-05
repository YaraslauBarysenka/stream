package net.red.demo.common.exception;

import java.io.Serial;

public class StreamException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 0L;

    public StreamException(String message) {
        super(message);
    }

    public StreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamException(Throwable cause) {
        super(cause);
    }
}