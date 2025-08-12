package api.v2.cms.mainmedia.service;

import java.util.List;

import api.v2.cms.mainmedia.dto.MainMediaRequestDto;
import api.v2.cms.mainmedia.dto.MainMediaResponseDto;

public interface MainMediaService {
    MainMediaResponseDto createMainMedia(MainMediaRequestDto requestDto);

    MainMediaResponseDto getMainMedia(Long id);

    List<MainMediaResponseDto> getAllMainMedia();

    MainMediaResponseDto updateMainMedia(Long id, MainMediaRequestDto requestDto);

    void deleteMainMedia(Long id);
}