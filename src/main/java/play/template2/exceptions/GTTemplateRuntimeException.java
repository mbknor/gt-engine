package play.template2.exceptions;

/**
 * Special exception that should only be thrown when the error is a direct
 * consequence of a template-error. Eg. inside a java-method called from genrated-list-code.
 *
 * When this exception passes out, we fix the stacktrace so that the first element is in the template itself.
 */
public class GTTemplateRuntimeException extends GTException {

    public GTTemplateRuntimeException() {
    }

    public GTTemplateRuntimeException(String s) {
        super(s);
    }

    public GTTemplateRuntimeException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public GTTemplateRuntimeException(Throwable throwable) {
        super(throwable);
    }
}
