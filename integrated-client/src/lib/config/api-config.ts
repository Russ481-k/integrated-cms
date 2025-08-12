/**
 * 중앙화된 API 설정
 * 모든 API URL을 환경변수 기반으로 관리
 */

// 기본 API URL (백엔드 서버)
const BASE_API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 서비스 타입별 API 경로 설정
const API_PATHS = {
  // 통합 CMS API (v2)
  INTEGRATED_CMS: "/api/v2/integrated-cms",
  
  // 서비스별 CMS API (v2) 
  SERVICE_CMS: "/api/v2/cms",
  
  // 기존 v1 API (하위 호환성)
  V1: "/api/v1",
  
  // 인증 API
  AUTH: "/api/v2/auth",
} as const;

// API 엔드포인트 구성
export const API_CONFIG = {
  // 기본 설정
  BASE_URL: BASE_API_URL,
  
  // 통합 CMS API
  INTEGRATED_CMS: {
    BASE: `${BASE_API_URL}${API_PATHS.INTEGRATED_CMS}`,
    MENU: "/menu",
    CONTENT: "/contents", 
    USER: "/user",
    FILE: "/file",
    TEMPLATE: "/template",
    SCHEDULE: "/schedule",
    POPUP: "/popups",
    MAIN_MEDIA: "/main-media",
    BBS: "/bbs",
    ENROLLMENTS: "/enrollments",
  },
  
  // 서비스별 CMS API
  SERVICE_CMS: {
    BASE: `${BASE_API_URL}${API_PATHS.SERVICE_CMS}`,
    getServiceUrl: (serviceId: string) => `${BASE_API_URL}${API_PATHS.SERVICE_CMS}/${serviceId}`,
  },
  
  // 인증 API
  AUTH: {
    BASE: `${BASE_API_URL}${API_PATHS.AUTH}`,
    LOGIN: "/login",
    SIGNUP: "/signup", 
    CHECK_USERNAME: "/check-username",
    VERIFY_EMAIL: "/send-verification-email",
    VERIFY_CODE: "/verify-email-code",
  },
  
  // 기존 v1 API (하위 호환성)
  V1: {
    BASE: `${BASE_API_URL}${API_PATHS.V1}`,
    CMS: "/cms",
    FILE_DOWNLOAD: "/cms/file/public/download",
    FILE_VIEW: "/cms/file/public/view",
  },
} as const;

// 헬퍼 함수들
export const getApiUrl = {
  // 통합 CMS URL 생성
  integratedCms: (endpoint: string = "") => `${API_CONFIG.INTEGRATED_CMS.BASE}${endpoint}`,
  
  // 서비스별 CMS URL 생성
  serviceCms: (serviceId: string, endpoint: string = "") => 
    `${API_CONFIG.SERVICE_CMS.getServiceUrl(serviceId)}${endpoint}`,
  
  // 인증 URL 생성
  auth: (endpoint: string = "") => `${API_CONFIG.AUTH.BASE}${endpoint}`,
  
  // v1 URL 생성 (하위 호환성)
  v1: (endpoint: string = "") => `${API_CONFIG.V1.BASE}${endpoint}`,
  
  // 파일 다운로드 URL 생성
  fileDownload: (fileId: number | string) => 
    `${API_CONFIG.V1.BASE}${API_CONFIG.V1.FILE_DOWNLOAD}/${fileId}`,
  
  // 파일 뷰 URL 생성
  fileView: (fileId: number | string) => 
    `${API_CONFIG.V1.BASE}${API_CONFIG.V1.FILE_VIEW}/${fileId}`,
};

// 환경별 설정 검증
export const validateApiConfig = () => {
  const requiredEnvVars = ['NEXT_PUBLIC_API_URL'];
  const missing = requiredEnvVars.filter(envVar => !process.env[envVar]);
  
  if (missing.length > 0) {
    console.warn(`Missing environment variables: ${missing.join(', ')}`);
  }
  
  return {
    isValid: missing.length === 0,
    missing,
    config: API_CONFIG,
  };
};

// 디버깅용 (개발 환경에서만)
if (process.env.NODE_ENV === 'development') {
  console.log('API Configuration:', {
    BASE_URL: API_CONFIG.BASE_URL,
    INTEGRATED_CMS: API_CONFIG.INTEGRATED_CMS.BASE,
    SERVICE_CMS: API_CONFIG.SERVICE_CMS.BASE,
    AUTH: API_CONFIG.AUTH.BASE,
  });
}
