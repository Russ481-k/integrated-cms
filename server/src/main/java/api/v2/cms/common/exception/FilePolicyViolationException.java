package api.v2.cms.common.exception;

public class FilePolicyViolationException extends RuntimeException {
    public FilePolicyViolationException(String message) {
        super(message);
    }
}