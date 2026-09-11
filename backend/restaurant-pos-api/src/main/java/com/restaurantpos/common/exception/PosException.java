package com.restaurantpos.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all POS business logic exceptions.
 */
@Getter
public class PosException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public PosException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static PosException notFound(String message) {
        return new PosException(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static PosException notFound(String entity, Object id) {
        return new PosException(
                entity + " not found with id: " + id,
                entity.toUpperCase().replace(" ", "_") + "_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static PosException badRequest(String message) {
        return new PosException(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }

    public static PosException badRequest(String message, String errorCode) {
        return new PosException(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    public static PosException conflict(String message, String errorCode) {
        return new PosException(message, errorCode, HttpStatus.CONFLICT);
    }

    public static PosException forbidden(String message) {
        return new PosException(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public static PosException unauthorized(String message) {
        return new PosException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    public static PosException internalError(String message) {
        return new PosException(message, "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
