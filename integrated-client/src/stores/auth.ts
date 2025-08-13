import { atom, useSetRecoilState, useRecoilValue } from "recoil";
import { type LoginCredentials, User as ApiUser } from "@/types/api";
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useCallback } from "react";
import { authNotificationService } from "@/services/authNotificationService";
import { authApiService } from "@/services/authApiService";
import { authStateService } from "@/services/authStateService";

export interface AppUser extends ApiUser {
  // 필요시 확장 가능
}

interface AuthState {
  isAuthenticated: boolean;
  user: AppUser | null;
  isLoading: boolean;
  isLoggingOut: boolean; // 로그아웃 진행 중 플래그 추가
}

export const authState = atom<AuthState>({
  key: "authState",
  default: {
    isAuthenticated: false,
    user: null,
    isLoading: false, // 로딩 상태를 바로 false로 시작
    isLoggingOut: false,
  },
});

export const useAuthActions = () => {
  const setAuth = useSetRecoilState(authState);
  const currentAuth = useRecoilValue(authState);
  const queryClient = useQueryClient();
  const router = useRouter();

  const login = async (credentials: LoginCredentials) => {
    try {
      // API 서비스를 통한 로그인 처리
      const appUser = await authApiService.login(credentials);
      
      // 상태 서비스를 통한 상태 업데이트
      setAuth(authStateService.createLoginState(appUser));

      console.log("✅ [Auth] Login successful, auth state updated:", {
        isAuthenticated: true,
        user: appUser.username,
        role: appUser.role
      });

      // 알림 서비스를 통한 성공 알림
      authNotificationService.showLoginSuccess();
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message ||
        error.message ||
        "로그인 중 오류가 발생했습니다.";
      
      // 상태 서비스를 통한 실패 상태 업데이트
      setAuth(authStateService.createLoggedOutState());
      
      // 알림 서비스를 통한 에러 알림
      authNotificationService.showLoginError(errorMessage);
      
      // 에러를 다시 던져서 컴포넌트 레벨에서 처리할 수 있도록 함
      throw error;
    }
  };

  const logout = async (redirectPath?: string, showToast: boolean = true) => {
    // 상태 서비스를 통한 로그아웃 시작 상태 설정
    setAuth(authStateService.createLoggingOutState(currentAuth));
    
    try {
      // API 서비스를 통한 로그아웃 처리
      await authApiService.logout();
    } catch (error) {
      // API 호출 실패해도 로컬 로그아웃은 진행
    } finally {
      // 상태 서비스를 통한 로그아웃 완료 상태 설정
      setAuth(authStateService.createLoggedOutState());
      queryClient.clear();

      const finalRedirectPath = redirectPath || "/login";
      setTimeout(() => {
        router.push(finalRedirectPath);
      }, 50);
      
      if (showToast) {
        setTimeout(() => {
          authNotificationService.showLogoutSuccess();
        }, 100);
      }
    }
  };

  const syncAuthState = useCallback(async () => {
    try {
      // API 서비스를 통한 토큰 검증
      const appUser = await authApiService.verifyToken();
      
      if (appUser) {
        // 상태 서비스를 통한 인증된 상태 설정
        setAuth(authStateService.createSyncedState(appUser));
      } else {
        // 상태 서비스를 통한 미인증 상태 설정
        setAuth(authStateService.createUnauthenticatedState());
      }
    } catch (error) {
      // 상태 서비스를 통한 미인증 상태 설정
      setAuth(authStateService.createUnauthenticatedState());
    }
  }, [setAuth]);

  return {
    login,
    logout,
    syncAuthState,
  };
}; 