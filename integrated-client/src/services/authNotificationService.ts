import { toaster } from "@/components/ui/toaster";

/**
 * 인증 관련 알림을 담당하는 서비스
 * UI 피드백 로직을 auth 로직에서 분리
 */
export const authNotificationService = {
  /**
   * 로그인 성공 알림
   */
  showLoginSuccess: () => {
    toaster.create({
      title: "로그인 성공",
      description: "환영합니다!",
      type: "success",
    });
  },

  /**
   * 로그인 실패 알림
   */
  showLoginError: (message: string) => {
    toaster.create({
      title: "로그인 실패",
      description: message,
      type: "error",
    });
  },

  /**
   * 로그아웃 성공 알림
   */
  showLogoutSuccess: () => {
    toaster.create({
      title: "로그아웃",
      description: "성공적으로 로그아웃되었습니다.",
      type: "success",
    });
  },

  /**
   * 권한 없음 알림
   */
  showUnauthorized: (message: string = "접근 권한이 없습니다.") => {
    toaster.create({
      title: "접근 불가",
      description: message,
      type: "error",
    });
  },
};
