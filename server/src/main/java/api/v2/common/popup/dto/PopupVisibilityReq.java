package api.v2.common.popup.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class PopupVisibilityReq {

    @NotNull
    private Boolean isVisible;

}