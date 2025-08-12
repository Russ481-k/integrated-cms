package api.v2.cms.template.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import api.v2.cms.common.dto.ApiResponseSchema;
import api.v2.cms.template.dto.TemplateDto;

public interface TemplateService {
    ApiResponseSchema<Page<TemplateDto>> getTemplates(String keyword, Pageable pageable);

    ApiResponseSchema<Page<TemplateDto>> getPublicTemplates(String keyword, Pageable pageable);

    ApiResponseSchema<TemplateDto> getPublicTemplate(Long id);

    ApiResponseSchema<TemplateDto> createTemplate(TemplateDto templateDto);

    ApiResponseSchema<TemplateDto> getTemplate(Long id);

    ApiResponseSchema<TemplateDto> updateTemplate(Long id, TemplateDto templateDto);

    ApiResponseSchema<Void> deleteTemplate(Long id);

    ApiResponseSchema<TemplateDto> cloneTemplate(Long id);

    ApiResponseSchema<TemplateDto> rollbackTemplate(Long id, Long versionId);
}