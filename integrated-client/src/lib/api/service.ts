import { privateApi } from "./client";
import { API_CONFIG, getApiUrl } from "@/lib/config/api-config";
import {
  Service,
  CreateServiceRequest,
  UpdateServiceRequest,
  ServiceApiResponse,
  SingleServiceApiResponse,
  UpdateServiceOrderRequest,
  ServiceStatus,
} from "@/app/(cms)/service/types";

// 서비스 관련 API 타입 정의
export interface ServiceApi {
  getServices: () => Promise<ServiceApiResponse>;
  getService: (serviceId: string) => Promise<SingleServiceApiResponse>;
  createService: (data: CreateServiceRequest) => Promise<SingleServiceApiResponse>;
  updateService: (
    serviceId: string,
    data: UpdateServiceRequest
  ) => Promise<SingleServiceApiResponse>;
  deleteService: (serviceId: string) => Promise<void>;
  updateServiceStatus: (serviceId: string, status: ServiceStatus) => Promise<void>;
  updateServiceOrder: (orders: UpdateServiceOrderRequest[]) => Promise<void>;
  checkServiceCodeAvailability: (serviceCode: string) => Promise<{ available: boolean }>;
}
// 서비스 API 구현
export const serviceApi: ServiceApi = {
  // 서비스 목록 조회
  getServices: async () => {
    const response = await privateApi.get<ServiceApiResponse>(
      getApiUrl.integratedCms(API_CONFIG.INTEGRATED_CMS.SERVICES)
    );
    return response.data;
  },

  // 단일 서비스 조회
  getService: async (serviceId: string) => {
    const response = await privateApi.get<SingleServiceApiResponse>(
      getApiUrl.integratedCms(`${API_CONFIG.INTEGRATED_CMS.SERVICES}/${serviceId}`)
    );
    return response.data;
  },

  // 서비스 생성
  createService: async (data: CreateServiceRequest) => {
    const response = await privateApi.post<SingleServiceApiResponse>(
      getApiUrl.integratedCms(API_CONFIG.INTEGRATED_CMS.SERVICES),
      data
    );
    return response.data;
  },

  // 서비스 수정
  updateService: async (serviceId: string, data: UpdateServiceRequest) => {
    const response = await privateApi.put<SingleServiceApiResponse>(
      getApiUrl.integratedCms(`${API_CONFIG.INTEGRATED_CMS.SERVICES}/${serviceId}`),
      data
    );
    return response.data;
  },

  // 서비스 삭제
  deleteService: async (serviceId: string) => {
    await privateApi.delete(getApiUrl.integratedCms(`${API_CONFIG.INTEGRATED_CMS.SERVICES}/${serviceId}`));
  },

  // 서비스 상태 변경
  updateServiceStatus: async (serviceId: string, status: ServiceStatus) => {
    await privateApi.put(
      getApiUrl.integratedCms(`${API_CONFIG.INTEGRATED_CMS.SERVICES}/${serviceId}/status`),
      { status }
    );
  },

  // 서비스 순서 변경 (단순화됨 - 계층구조 없음)
  updateServiceOrder: async (orders: UpdateServiceOrderRequest[]) => {
    await privateApi.put(
      getApiUrl.integratedCms(`${API_CONFIG.INTEGRATED_CMS.SERVICES}/order`),
      orders
    );
  },

  // 서비스 코드 중복 확인
  checkServiceCodeAvailability: async (serviceCode: string) => {
    const response = await privateApi.get<{ available: boolean }>(
      getApiUrl.integratedCms(`${API_CONFIG.INTEGRATED_CMS.SERVICES}/check-code/${serviceCode}`)
    );
    return response.data;
  },
};

// React Query 키 정의
export const serviceKeys = {
  all: ["services"] as const,
  lists: () => [...serviceKeys.all, "list"] as const,
  list: (filters?: { status?: ServiceStatus[]; search?: string }) =>
    [...serviceKeys.lists(), { filters }] as const,
  details: () => [...serviceKeys.all, "detail"] as const,
  detail: (serviceId: string) => [...serviceKeys.details(), serviceId] as const,
  codeCheck: (serviceCode: string) => [...serviceKeys.all, "codeCheck", serviceCode] as const,
};

// 서비스 데이터 유틸리티 함수들
export const serviceUtils = {
  // 서비스 상태별 색상 반환
  getStatusColor: (status: ServiceStatus): string => {
    switch (status) {
      case "ACTIVE":
        return "green";
      case "INACTIVE":
        return "gray";
      case "MAINTENANCE":
        return "orange";
      default:
        return "gray";
    }
  },

  // 서비스 상태별 표시 텍스트 반환
  getStatusText: (status: ServiceStatus): string => {
    switch (status) {
      case "ACTIVE":
        return "활성";
      case "INACTIVE":
        return "비활성";
      case "MAINTENANCE":
        return "점검중";
      default:
        return "알 수 없음";
    }
  },

  // 서비스 코드 검증
  validateServiceCode: (serviceCode: string): { isValid: boolean; message?: string } => {
    if (!serviceCode) {
      return { isValid: false, message: "서비스 코드를 입력해주세요." };
    }
    if (serviceCode.length < 2 || serviceCode.length > 50) {
      return { isValid: false, message: "서비스 코드는 2-50자 사이여야 합니다." };
    }
    if (!/^[a-zA-Z0-9_-]+$/.test(serviceCode)) {
      return { isValid: false, message: "서비스 코드는 영문, 숫자, 하이픈, 언더스코어만 사용할 수 있습니다." };
    }
    return { isValid: true };
  },

  // 서비스 이름 검증
  validateServiceName: (serviceName: string): { isValid: boolean; message?: string } => {
    if (!serviceName) {
      return { isValid: false, message: "서비스 이름을 입력해주세요." };
    }
    if (serviceName.length < 1 || serviceName.length > 100) {
      return { isValid: false, message: "서비스 이름은 1-100자 사이여야 합니다." };
    }
    return { isValid: true };
  },

  // URL 검증
  validateUrl: (url: string): { isValid: boolean; message?: string } => {
    if (!url) {
      return { isValid: true }; // URL은 선택사항
    }
    try {
      new URL(url);
      return { isValid: true };
    } catch {
      return { isValid: false, message: "유효한 URL 형식이 아닙니다." };
    }
  },

  // 서비스 데이터 마스킹 (민감한 정보 숨김)
  maskSensitiveData: (service: Service): Service => {
    return {
      ...service,
      dbConnectionInfo: service.dbConnectionInfo ? "••••••••" : undefined,
    };
  },

  // 서비스 목록 정렬 (활성 상태 우선, 이름 순)
  sortServices: (services: Service[]): Service[] => {
    return [...services].sort((a, b) => {
      // 상태 우선순위: ACTIVE > MAINTENANCE > INACTIVE
      const statusPriority = { ACTIVE: 3, MAINTENANCE: 2, INACTIVE: 1 };
      const statusDiff = statusPriority[a.status] - statusPriority[b.status];
      
      if (statusDiff !== 0) {
        return -statusDiff; // 내림차순으로 정렬 (높은 우선순위가 앞으로)
      }
      
      // 같은 상태라면 이름 순 정렬
      return a.serviceName.localeCompare(b.serviceName, "ko");
    });
  },
};
