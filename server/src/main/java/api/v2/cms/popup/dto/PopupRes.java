package api.v2.cms.popup.dto;

import lombok.Builder;
import lombok.Data;
import api.v2.cms.popup.domain.Popup;

@Data
@Builder
public class PopupRes {

    private Long id;
    private String title;
    private String content;
    private Integer displayOrder;

    public static PopupRes from(Popup entity) {
        return PopupRes.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}