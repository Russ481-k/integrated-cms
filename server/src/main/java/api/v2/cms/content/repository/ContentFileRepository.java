package api.v2.cms.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.v2.cms.content.domain.Content;
import api.v2.cms.content.domain.ContentFile;

import java.util.List;

@Repository
public interface ContentFileRepository extends JpaRepository<ContentFile, Long> {
    List<ContentFile> findByContent(Content content);

    void deleteByContent(Content content);
}