package play.template2.exceptions;

public class GTRuntimeException extends GTException {

    public GTRuntimeException() {
    }

    public GTRuntimeException(String s) {
        super(s);
    }

    public GTRuntimeException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public GTRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
