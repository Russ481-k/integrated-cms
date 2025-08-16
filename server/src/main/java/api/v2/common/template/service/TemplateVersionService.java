package api.v2.common.template.service;

import api.v2.common.template.domain.Template;

public interface TemplateVersionService {
    Template createNewVersion(Template existingTemplate);

    Template rollbackToVersion(Template template, int versionNo);
}