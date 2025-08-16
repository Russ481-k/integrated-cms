package api.v2.common.mypage.controller;

import api.v2.common.mypage.dto.PasswordChangeDto;
import api.v2.common.mypage.dto.ProfileDto;
import api.v2.common.mypage.service.MypageProfileService;
import api.v2.common.user.domain.User;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/mypage")
public class MypageController {

    private final MypageProfileService mypageProfileService;

    public MypageController(MypageProfileService mypageProfileService) {
        this.mypageProfileService = mypageProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileDto> getProfile(@AuthenticationPrincipal User user) {
        ProfileDto profileDto = mypageProfileService.getProfile(user);
        return ResponseEntity.ok(profileDto);
    }

    @PatchMapping("/profile")
    public ResponseEntity<ProfileDto> updateProfile(@AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileDto profileDto) {
        ProfileDto updatedProfile = mypageProfileService.updateProfile(user, profileDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordChangeDto passwordChangeDto) {
        mypageProfileService.changePassword(user, passwordChangeDto);
        return ResponseEntity.ok().build();
    }
}