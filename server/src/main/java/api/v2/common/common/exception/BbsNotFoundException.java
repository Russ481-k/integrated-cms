package api.v2.common.common.exception;

public class BbsNotFoundException extends RuntimeException {
    public BbsNotFoundException(Long id) {
        super("게시글을 찾을 수 없습니다. (id: " + id + ")");
    }
}