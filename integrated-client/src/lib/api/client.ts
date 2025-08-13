import axios, {
  AxiosError,
  AxiosInstance,
  InternalAxiosRequestConfig,
} from "axios";
import {
  getToken,
  removeToken,
  getRefreshToken,
  setToken,
} from "../auth-utils";
import { API_CONFIG, getApiUrl } from "@/lib/config/api-config";

// 통합 CMS용 API 기본 URL 설정
const BASE_URL = API_CONFIG.BASE_URL;

// 기본 API 클라이언트 설정
const createApiClient = (needsAuth: boolean): AxiosInstance => {
  const client = axios.create({
    baseURL: BASE_URL,
    headers: {
      "Content-Type": "application/json",
    },
    withCredentials: true,
  });

  if (needsAuth) {
    client.interceptors.request.use(
      (config) => {
        const accessToken = getToken();
        if (accessToken) {
          config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & {
          _retry?: boolean;
        };

        if (error.response?.status === 401 && !originalRequest._retry) {
          console.log("🚨 [API Client] 401 Unauthorized detected:", {
            url: originalRequest.url,
            method: originalRequest.method,
            hasRefreshToken: !!getRefreshToken(),
            timestamp: new Date().toISOString(),
          });

          originalRequest._retry = true;
          const refreshToken = getRefreshToken();

          if (!refreshToken) {
            console.log("❌ [API Client] No refresh token available, forcing logout");
            removeToken();
            if (typeof window !== "undefined") {
              // 통합 CMS는 모든 API가 CMS 관련이므로 항상 /login 사용
              const loginUrl = "/login";
              console.log("🔄 [API Client] Redirecting to:", `${loginUrl}?error=session_expired`);
              window.location.href = `${loginUrl}?error=session_expired`;
            }
            return Promise.reject(new Error("No refresh token available."));
          }

          try {
            const reissueResponse = await axios.post(
              `${getApiUrl.integratedCms("/auth/reissue")}`,
              {},
              {
                headers: { Authorization: `Bearer ${refreshToken}` },
                withCredentials: true,
              }
            );

            const {
              accessToken,
              refreshToken: newRefreshToken,
              accessTokenExpiresIn,
            } = reissueResponse.data;

            setToken(accessToken, newRefreshToken, accessTokenExpiresIn);

            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            }

            return client(originalRequest);
          } catch (reissueError) {
            removeToken();
            if (typeof window !== "undefined") {
              // 통합 CMS는 모든 API가 CMS 관련이므로 항상 /login 사용
              const loginUrl = "/login";
              window.location.href = `${loginUrl}?error=session_expired`;
            }
            return Promise.reject(reissueError);
          }
        }
        return Promise.reject(error);
      }
    );
  }

  return client;
};

// Export API clients
export const publicApi = createApiClient(false);
export const privateApi = createApiClient(true);

// API 메서드 생성 함수
const createApiMethods = (client: AxiosInstance) => ({
  get: async <T>(endpoint: string, config?: InternalAxiosRequestConfig) => {
    const response = await client.get<any>(endpoint, config);
    // 백엔드 응답이 { success, data, message } 형태인 경우
    if (
      response.data &&
      typeof response.data === "object" &&
      "data" in response.data
    ) {
      return response.data.data as T;
    }
    return response.data as T;
  },
  post: async <T, D = unknown>(
    endpoint: string,
    data?: D,
    config?: InternalAxiosRequestConfig
  ) => {
    const response = await client.post<any>(endpoint, data, config);
    // 백엔드 응답이 { success, data, message } 형태인 경우
    if (
      response.data &&
      typeof response.data === "object" &&
      "data" in response.data
    ) {
      return response.data.data as T;
    }
    return response.data as T;
  },
  put: async <T, D = unknown>(
    endpoint: string,
    data?: D,
    config?: InternalAxiosRequestConfig
  ) => {
    const response = await client.put<any>(endpoint, data, config);
    // 백엔드 응답이 { success, data, message } 형태인 경우
    if (
      response.data &&
      typeof response.data === "object" &&
      "data" in response.data
    ) {
      return response.data.data as T;
    }
    return response.data as T;
  },
  patch: async <T, D = unknown>(
    endpoint: string,
    data?: D,
    config?: InternalAxiosRequestConfig
  ) => {
    const response = await client.patch<any>(endpoint, data, config);
    // 백엔드 응답이 { success, data, message } 형태인 경우
    if (
      response.data &&
      typeof response.data === "object" &&
      "data" in response.data
    ) {
      return response.data.data as T;
    }
    return response.data as T;
  },
  delete: async <T>(endpoint: string, config?: InternalAxiosRequestConfig) => {
    const response = await client.delete<any>(endpoint, config);
    // 백엔드 응답이 { success, data, message } 형태인 경우
    if (
      response.data &&
      typeof response.data === "object" &&
      "data" in response.data
    ) {
      return response.data.data as T;
    }
    return response.data as T;
  },
});

// Export API methods
export const publicApiMethods = createApiMethods(publicApi);
export const privateApiMethods = createApiMethods(privateApi);
