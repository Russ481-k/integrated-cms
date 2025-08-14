package api.v2.cms.admin.user.service.impl;

import lombok.RequiredArgsConstructor;
import api.v2.cms.admin.user.dto.UserMemoDto;
import api.v2.cms.admin.user.service.UserAdminService;
import api.v2.common.crud.exception.CrudResourceNotFoundException;
import api.v2.cms.user.domain.User;
import api.v2.cms.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;

    @Override
    public UserMemoDto getUserMemo(String userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new CrudResourceNotFoundException("User not found with UUID: " + userUuid));
        // Real implementation:
        return new UserMemoDto(userUuid, user.getMemo(), user.getMemoUpdatedAt(), user.getMemoUpdatedBy());
    }

    @Override
    @Transactional
    public UserMemoDto updateUserMemo(String userUuid, String memoContent, String adminId) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new CrudResourceNotFoundException("User not found with UUID: " + userUuid));

        // Real implementation:
        user.setMemo(memoContent);
        user.setMemoUpdatedAt(java.time.LocalDateTime.now());
        user.setMemoUpdatedBy(adminId); // Need to fetch admin user if storing actual admin user object
        userRepository.save(user);

        return new UserMemoDto(userUuid, user.getMemo(), user.getMemoUpdatedAt(), user.getMemoUpdatedBy());
    }

    @Override
    @Transactional
    public void deleteUserMemo(String userUuid, String adminId) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new CrudResourceNotFoundException("User not found with UUID: " + userUuid));

        // Real implementation:
        user.setMemo(null);
        user.setMemoUpdatedAt(java.time.LocalDateTime.now());
        user.setMemoUpdatedBy(adminId); // Log who deleted it
        userRepository.save(user);
    }
}