package api.v2.common.mainmedia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.v2.common.mainmedia.domain.MainMedia;

import java.util.List;

@Repository
public interface MainMediaRepository extends JpaRepository<MainMedia, Long> {
    List<MainMedia> findAllByOrderByDisplayOrderAsc();
}