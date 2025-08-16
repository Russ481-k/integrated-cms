package api.v2.common.popup.service;

import org.springframework.web.multipart.MultipartFile;

import api.v2.common.popup.dto.*;

import java.util.List;

public interface PopupService {

        List<AdminPopupRes> getPopupsForAdmin();

        PopupDto getPopup(Long popupId);

        PopupDto createPopup(PopupDataReq popupData, String contentJson, List<MultipartFile> mediaFiles,
                        String mediaLocalIds);

        PopupDto updatePopup(Long popupId, PopupUpdateReq popupUpdateReq, String contentJson,
                        List<MultipartFile> mediaFiles, String mediaLocalIds);

        void deletePopup(Long popupId);

        void updateVisibility(Long popupId, PopupVisibilityReq req);

        void updateOrder(PopupOrderReq req);

        List<PopupRes> getActivePopups();

}