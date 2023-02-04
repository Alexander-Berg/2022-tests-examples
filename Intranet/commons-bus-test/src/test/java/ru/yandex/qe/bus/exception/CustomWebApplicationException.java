package ru.yandex.qe.bus.exception;

import javax.ws.rs.BadRequestException;

/**
 * @author rurikk
 */
public class CustomWebApplicationException extends BadRequestException {
    public CustomWebApplicationException() {
    }

    public CustomWebApplicationException(Throwable cause) {
        super(cause);
    }
}
