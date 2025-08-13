import { type LoginCredentials } from "@/types/api";
import { authApi } from "@/lib/api/auth";
import { 
  setToken, 
  removeToken, 
  getToken, 
  getUser, 
  USER_KEY 
} from "@/lib/auth-utils";
import type { AppUser } from "@/stores/auth";

/**
 * 인증 API 호출만을 담당하는 서비스
 * 토큰 관리와 API 통신에 집중
 */
export const authApiService = {
  /**
   * 로그인 API 호출 및 토큰 저장
   */
  async login(credentials: LoginCredentials): Promise<AppUser> {
    const response = await authApi.login(credentials);
    
    if (!response.data.success || !response.data.data?.accessToken) {
      throw new Error(
        response.data.message || "로그인 정보가 올바르지 않습니다."
      );
    }

    const { accessToken, refreshToken, user: apiUser } = response.data.data;
    const appUser: AppUser = { ...apiUser };

    setToken(accessToken, refreshToken, 3600, appUser);
    
    return appUser;
  },

  /**
   * 로그아웃 API 호출
   */
  async logout(): Promise<void> {
    try {
      await authApi.logout();
    } finally {
      removeToken();
    }
  },

  /**
   * 토큰 검증 및 사용자 정보 조회
   */
  async verifyToken(): Promise<AppUser | null> {
    const token = getToken();
    if (!token) {
      return null;
    }

    try {
      const userFromApi = await authApi.verifyToken();
      const appUser: AppUser = { ...userFromApi };
      localStorage.setItem(USER_KEY, JSON.stringify(appUser));
      return appUser;
    } catch (error) {
      removeToken();
      throw error;
    }
  },

  /**
   * 현재 저장된 토큰 확인
   */
  hasValidToken(): boolean {
    return !!getToken();
  },

  /**
   * 로컬 저장소에서 사용자 정보 조회
   */
  getStoredUser(): AppUser | null {
    return getUser();
  },
};
