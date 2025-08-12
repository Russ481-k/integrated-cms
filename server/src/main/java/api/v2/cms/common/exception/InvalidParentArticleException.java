package api.v2.cms.common.exception;

public class InvalidParentArticleException extends RuntimeException {
    public InvalidParentArticleException(String message) {
        super(message);
    }
}