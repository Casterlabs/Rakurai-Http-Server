package co.casterlabs.rhs.impl;

import co.casterlabs.rhs.protocol.HttpStatus;

class HttpException extends Exception {
    private static final long serialVersionUID = 4899100353178913026L;

    public final HttpStatus status;

    public HttpException(HttpStatus reason) {
        super(reason.getStatusString());
        this.status = reason;
    }

}
