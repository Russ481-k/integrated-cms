package api.v2.cms.mypage.service;

import lombok.RequiredArgsConstructor;
import api.v2.common.crud.exception.CrudBusinessRuleException;
import api.v2.common.crud.exception.CrudResourceNotFoundException;
import api.v2.cms.mypage.dto.PasswordChangeDto;
import api.v2.cms.mypage.dto.ProfileDto;
import api.v2.cms.user.domain.User;
import api.v2.cms.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.security.authentication.BadCredentialsException; // No longer directly used, replaced by BusinessRuleException

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MypageProfileServiceImpl implements MypageProfileService {

    private static final Logger logger = LoggerFactory.getLogger(MypageProfileServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ProfileDto getProfile(User user) {
        User freshUser = userRepository.findById(user.getUuid())
                .orElseThrow(() -> new CrudResourceNotFoundException("User not found"));

        ProfileDto profileDto = new ProfileDto();
        profileDto.setName(freshUser.getName());
        profileDto.setUserId(freshUser.getUsername());
        profileDto.setPhone(freshUser.getPhone());
        profileDto.setAddress(freshUser.getAddress());
        profileDto.setEmail(freshUser.getEmail());
        profileDto.setCarNo(freshUser.getCarNo());
        profileDto.setGender(freshUser.getGender());
        return profileDto;
    }

    @Override
    public ProfileDto updateProfile(User authenticatedUser, ProfileDto profileDto) {
        User user = userRepository.findById(authenticatedUser.getUuid())
                .orElseThrow(() -> new CrudResourceNotFoundException("User not found"));

        try {
            user.setName(profileDto.getName());
            user.setEmail(profileDto.getEmail());
            user.setCarNo(profileDto.getCarNo());
            user.setPhone(profileDto.getPhone());
            user.setAddress(profileDto.getAddress());
            User updatedUser = userRepository.save(user);
            return getProfile(updatedUser); // Use the existing getProfile which also fetches fresh data
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: {}", authenticatedUser.getUuid(), e.getMessage(), e);
            throw new CrudBusinessRuleException("프로필 업데이트 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @Override
    public void changePassword(User authenticatedUser, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.findById(authenticatedUser.getUuid())
                .orElseThrow(() -> new CrudResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPw(), user.getPassword())) {
            throw new CrudBusinessRuleException("현재 비밀번호가 올바르지 않습니다.");
        }
        // Optional: Validate new password policy here if any
        // if (!isPasswordPolicyCompliant(passwordChangeDto.getNewPw())) {
        // throw new BusinessRuleException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        // }

        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPw()));
        if (user.isTempPwFlag()) {
            user.setTempPwFlag(false);
        }
        userRepository.save(user);
    }

    @Override
    public void issueTemporaryPassword(String userId) {
        User user = userRepository.findByUsername(userId)
                .orElseThrow(
                        () -> new CrudResourceNotFoundException("해당 아이디의 사용자를 찾을 수 없습니다."));

        try {
            String temporaryPassword = UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(temporaryPassword));
            user.setTempPwFlag(true);
            userRepository.save(user);

            logger.info("Temporary password issued for user: {}. Email: {}. Temporary Password: {}",
                    user.getUsername(), user.getEmail(), temporaryPassword);
            // TODO: 실제 이메일 발송 로직 또는 다른 알림 방식 구현
        } catch (Exception e) {
            logger.error("Error issuing temporary password for user {}: {}", userId, e.getMessage(), e);
            throw new CrudBusinessRuleException("임시 비밀번호 발급 중 오류가 발생했습니다.");
        }
    }
}