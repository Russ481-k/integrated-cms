package api.v2.cms.mainmedia.service.impl;

import lombok.RequiredArgsConstructor;
import api.v2.common.crud.exception.CrudResourceNotFoundException;
import api.v2.cms.file.entity.CmsFile;
import api.v2.cms.file.service.FileService;
import api.v2.cms.mainmedia.domain.MainMedia;
import api.v2.cms.mainmedia.dto.MainMediaRequestDto;
import api.v2.cms.mainmedia.dto.MainMediaResponseDto;
import api.v2.cms.mainmedia.repository.MainMediaRepository;
import api.v2.cms.mainmedia.service.MainMediaService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MainMediaServiceImpl implements MainMediaService {

    private final MainMediaRepository mainMediaRepository;
    private final FileService fileService;

    @Value("${app.file.base-url:http://localhost:8080/media}")
    private String fileBaseUrl;

    @Override
    public MainMediaResponseDto createMainMedia(MainMediaRequestDto requestDto) {
        CmsFile cmsFile = fileService.getFile(requestDto.getFileId());

        MainMedia mainMedia = MainMedia.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .mediaType(requestDto.getMediaType())
                .displayOrder(requestDto.getDisplayOrder())
                .cmsFile(cmsFile)
                .build();

        MainMedia savedMedia = mainMediaRepository.save(mainMedia);
        return MainMediaResponseDto.from(savedMedia, fileBaseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public MainMediaResponseDto getMainMedia(Long id) {
        MainMedia mainMedia = mainMediaRepository.findById(id)
                .orElseThrow(() -> new CrudResourceNotFoundException("MainMedia not found with id: " + id));
        return MainMediaResponseDto.from(mainMedia, fileBaseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MainMediaResponseDto> getAllMainMedia() {
        return mainMediaRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(media -> MainMediaResponseDto.from(media, fileBaseUrl))
                .collect(Collectors.toList());
    }

    @Override
    public MainMediaResponseDto updateMainMedia(Long id, MainMediaRequestDto requestDto) {
        MainMedia mainMedia = mainMediaRepository.findById(id)
                .orElseThrow(() -> new CrudResourceNotFoundException("MainMedia not found with id: " + id));

        if (!mainMedia.getCmsFile().getFileId().equals(requestDto.getFileId())) {
            fileService.deleteFile(mainMedia.getCmsFile().getFileId());
            CmsFile newCmsFile = fileService.getFile(requestDto.getFileId());
            mainMedia.setCmsFile(newCmsFile);
        }

        mainMedia.update(
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getDisplayOrder());

        MainMedia updatedMedia = mainMediaRepository.save(mainMedia);
        return MainMediaResponseDto.from(updatedMedia, fileBaseUrl);
    }

    @Override
    public void deleteMainMedia(Long id) {
        MainMedia mainMedia = mainMediaRepository.findById(id)
                .orElseThrow(() -> new CrudResourceNotFoundException("MainMedia not found with id: " + id));

        fileService.deleteFile(mainMedia.getCmsFile().getFileId());
        mainMediaRepository.delete(mainMedia);
    }
}