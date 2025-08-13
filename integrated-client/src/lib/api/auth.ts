import {
  LoginCredentials,
  AuthResponse,
  VerifyTokenResponse,
  User,
} from "@/types/api";
import { publicApi, privateApi } from "./client";
import { getToken, setToken, removeToken } from "../auth-utils";
import { getApiUrl } from "../config/api-config";

// React Query 키 정의
export const authKeys = {
  all: ["auth"] as const,
  user: () => [...authKeys.all, "user"] as const,
  token: () => [...authKeys.all, "token"] as const,
  me: () => [...authKeys.all, "me"] as const,
};

// 인증 관련 API 타입 정의
export interface AuthApi {
  login: (credentials: LoginCredentials) => Promise<AuthResponse>;
  verifyToken: () => Promise<User>;
  logout: () => Promise<void>;
  getMe: () => Promise<User>;
}

// 인증 API 구현
export const authApi = {
  login: async (credentials: LoginCredentials) => {
    const response = await publicApi.post<AuthResponse>(
      getApiUrl.integratedCms("/auth/login"),
      credentials
    );
    const authData = response.data.data; // 실제 데이터는 .data 안에 있습니다.

    if (authData?.accessToken) {
      // accessToken, refreshToken을 저장합니다.
      // accessTokenExpiresIn은 AuthResponse 타입에 없으므로 제거합니다.
      setToken(authData.accessToken, authData.refreshToken);
    }

    // 응답 데이터의 role 형식 통일 ("ROLE_ADMIN" -> "ADMIN")
    if (authData?.user?.role) {
      authData.user.role = authData.user.role.replace("ROLE_", "") || "USER";
    }

    return response;
  },

  logout: async () => {
    // 서버에 로그아웃 요청 (선택적)
    // await publicApi.post<void>("/auth/logout");

    // 클라이언트 측 토큰 제거
    removeToken();

    // 새로고침 제거: 상태 관리만으로 처리
    // if (typeof window !== "undefined") {
    //   window.location.reload();
    // }
  },

  verifyToken: async (): Promise<User> => {
    const response = await privateApi.get<VerifyTokenResponse>(getApiUrl.integratedCms("/auth/verify"));

    const apiData = response.data.data;

    // 백엔드에서 직접 제공하는 사용자 정보 사용
    const user: User = {
      uuid: apiData.uuid || "",
      username: apiData.username || "",
      role: apiData.role || "USER", // 백엔드에서 이미 "ROLE_" 제거됨
      name: apiData.name || apiData.username || "",
      email: apiData.email || "",
      status: apiData.status || "ACTIVE",
      createdAt: apiData.createdAt || new Date().toISOString(),
      updatedAt: apiData.updatedAt || new Date().toISOString(),
    };

    return user;
  },
};
