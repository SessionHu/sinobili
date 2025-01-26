package org.sessx.sinobili.bili;

public class BiliException extends RuntimeException {

    public BiliException() {
        super();
    }

    public BiliException(String message) {
        super(message);
    }

    public BiliException(String message, Throwable cause) {
        super(message, cause);
    }

    public BiliException(Throwable cause) {
        super(cause);
    }

}
