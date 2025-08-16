package api.v2.common.common.exception;

public class FilePolicyViolationException extends RuntimeException {
    public FilePolicyViolationException(String message) {
        super(message);
    }
}