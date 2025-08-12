package api.v2.cms.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import api.v2.cms.user.domain.UserMemo;

import java.util.Optional;

@Repository
public interface UserMemoRepository extends JpaRepository<UserMemo, Long> {
    Optional<UserMemo> findByUserUuid(String userUuid);
}