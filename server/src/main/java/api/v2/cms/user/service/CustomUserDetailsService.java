package api.v2.cms.user.service;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomUserDetailsService extends UserDetailsService {
    // UserDetailsService의 loadUserByUsername 메서드를 상속받음
}