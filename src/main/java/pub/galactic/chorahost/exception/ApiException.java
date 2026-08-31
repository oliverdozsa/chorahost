package pub.galactic.chorahost.exception;

import jakarta.ws.rs.core.Response;

public class ApiException extends RuntimeException {

    public final Response.Status status;

    public ApiException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(Response.Status.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(Response.Status.BAD_REQUEST, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(Response.Status.CONFLICT, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(Response.Status.FORBIDDEN, message);
    }
}
