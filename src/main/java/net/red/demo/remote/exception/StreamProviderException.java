package net.red.demo.remote.exception;


import java.io.Serial;

public class StreamProviderException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 0;

    public StreamProviderException(String message) {
        super(message);
    }

    public StreamProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}