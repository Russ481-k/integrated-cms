package api.v2.common.content.exception;

public class ContentNotFoundException extends RuntimeException {
    public ContentNotFoundException(Long contentId) {
        super("Content not found with id: " + contentId);
    }
}