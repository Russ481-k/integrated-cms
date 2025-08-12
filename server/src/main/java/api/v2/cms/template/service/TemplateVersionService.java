package api.v2.cms.template.service;

import api.v2.cms.template.domain.Template;

public interface TemplateVersionService {
    Template createNewVersion(Template existingTemplate);

    Template rollbackToVersion(Template template, int versionNo);
}