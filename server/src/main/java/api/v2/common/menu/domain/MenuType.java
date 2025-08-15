package api.v2.common.menu.domain;

/**
 * 메뉴 타입을 정의하는 열거형
 */
public enum MenuType {
    /**
     * 폴더형 메뉴 - 하위 메뉴를 포함할 수 있음
     */
    FOLDER,

    /**
     * 링크형 메뉴 - URL을 가짐
     */
    LINK,

    /**
     * 페이지형 메뉴 - 내부 페이지와 연결됨
     */
    PAGE,

    /**
     * 게시판형 메뉴 - 게시판과 연결됨
     */
    BOARD
}
