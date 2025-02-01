package co.casterlabs.rhs.protocol.exceptions;

import co.casterlabs.rhs.HttpStatus;
import lombok.ToString;

@ToString
public class HttpException extends Exception {
    private static final long serialVersionUID = 4899100353178913026L;

    public final HttpStatus status;

    public HttpException(HttpStatus reason) {
        super(reason.statusString());
        this.status = reason;
    }

}
