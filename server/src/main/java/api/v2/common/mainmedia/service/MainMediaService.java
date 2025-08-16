package api.v2.common.mainmedia.service;

import java.util.List;

import api.v2.common.mainmedia.dto.MainMediaRequestDto;
import api.v2.common.mainmedia.dto.MainMediaResponseDto;

public interface MainMediaService {
    MainMediaResponseDto createMainMedia(MainMediaRequestDto requestDto);

    MainMediaResponseDto getMainMedia(Long id);

    List<MainMediaResponseDto> getAllMainMedia();

    MainMediaResponseDto updateMainMedia(Long id, MainMediaRequestDto requestDto);

    void deleteMainMedia(Long id);
}