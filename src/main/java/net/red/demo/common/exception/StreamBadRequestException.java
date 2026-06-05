package net.red.demo.common.exception;

import java.io.Serial;

/**
 * Leads to BAD_REQUEST HTTP response status.
 */
public class StreamBadRequestException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 0L;

    public StreamBadRequestException(String message) {
        super(message);
    }
}