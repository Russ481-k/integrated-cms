import type { AppUser } from "@/stores/auth";

/**
 * 인증 상태 관리만을 담당하는 서비스
 * Recoil 상태 업데이트 로직에 집중
 */
export const authStateService = {
  /**
   * 로그인 성공 시 상태 생성
   */
  createLoginState(user: AppUser) {
    return {
      isAuthenticated: true,
      user,
      isLoading: false,
      isLoggingOut: false,
    };
  },

  /**
   * 로그아웃 시작 시 상태 생성
   */
  createLoggingOutState(currentState: any) {
    return {
      ...currentState,
      isLoggingOut: true,
    };
  },

  /**
   * 로그아웃 완료 시 상태 생성
   */
  createLoggedOutState() {
    return {
      isAuthenticated: false,
      user: null,
      isLoading: false,
      isLoggingOut: false,
    };
  },

  /**
   * 토큰 검증 성공 시 상태 생성
   */
  createSyncedState(user: AppUser) {
    return {
      isAuthenticated: true,
      user,
      isLoading: false,
      isLoggingOut: false,
    };
  },

  /**
   * 토큰 검증 실패 시 상태 생성
   */
  createUnauthenticatedState() {
    return {
      isAuthenticated: false,
      user: null,
      isLoading: false,
      isLoggingOut: false,
    };
  },
};
