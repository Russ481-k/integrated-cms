package api.v2.cms.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.v2.cms.file.domain.FileMetaDomain;

import java.util.List;

@Repository
public interface FileMetaRepository extends JpaRepository<FileMetaDomain, Long> {
    List<FileMetaDomain> findByModuleAndModuleId(String module, Long moduleId);
}