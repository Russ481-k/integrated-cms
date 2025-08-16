import React from "react";

// 서비스 상태 타입 정의
export type ServiceStatus = "ACTIVE" | "INACTIVE" | "MAINTENANCE";

// 서비스 기본 인터페이스 (DB 스키마 기반)
export interface Service {
  serviceId: string; // UUID
  serviceCode: string; // 중복 불가 서비스 코드
  serviceName: string; // 서비스 이름
  serviceDomain?: string; // 서비스 도메인
  apiBaseUrl?: string; // API 기본 URL
  dbConnectionInfo?: string; // 암호화된 DB 접속 정보 (마스킹 처리)
  status: ServiceStatus; // 서비스 상태
  description?: string; // 서비스 설명
  config?: Record<string, any>; // JSON 설정 정보
  createdBy?: string; // 생성자 ID
  createdIp?: string; // 생성 IP
  createdAt: string; // 생성 시각
  updatedBy?: string; // 수정자 ID
  updatedIp?: string; // 수정 IP
  updatedAt: string; // 수정 시각
}

// 서비스 생성/수정용 타입 (읽기 전용 필드 제외)
export type CreateServiceRequest = Omit<
  Service,
  "serviceId" | "createdBy" | "createdIp" | "createdAt" | "updatedBy" | "updatedIp" | "updatedAt"
>;

export type UpdateServiceRequest = Partial<CreateServiceRequest>;

// 드래그 앤 드롭용 인터페이스 (단순화 - 계층구조 없음)
export interface ServiceDragItem {
  serviceId: string;
  type: string;
  index: number;
}

// 서비스 아이템 컴포넌트 props
export interface ServiceItemProps {
  service: Service;
  onEditService: (service: Service) => void;
  onDeleteService: (serviceId: string) => void;
  onMoveService: (
    draggedServiceId: string,
    targetServiceId: string,
    position: "before" | "after"
  ) => void;
  index: number;
  selectedServiceId?: string;
  isLoading?: boolean;
}

// 서비스 목록 컴포넌트 props
export interface ServiceListProps {
  services: Service[];
  onEditService: (service: Service) => void;
  onDeleteService: (serviceId: string) => void;
  onMoveService: (
    serviceId: string,
    targetServiceId: string,
    position: "before" | "after"
  ) => void;
  onAddService: () => void;
  isLoading: boolean;
  selectedServiceId?: string;
  onServiceFilter?: (filteredServices: Service[]) => void;
}

// 서비스 에디터 컴포넌트 props
export interface ServiceEditorProps {
  service: Service | null;
  onClose: () => void;
  onDelete: (serviceId: string) => void;
  onSubmit: (serviceData: CreateServiceRequest) => Promise<void>;
  onAddService: () => void;
  isTempService?: boolean;
}

// 서비스 필터링 옵션
export interface ServiceFilters {
  status?: ServiceStatus[];
  search?: string;
}

// API 응답 타입
export interface ServiceApiResponse {
  data: Service[];
  message?: string;
  success: boolean;
}

export interface SingleServiceApiResponse {
  data: Service;
  message?: string;
  success: boolean;
}

// 서비스 순서 변경 요청
export interface UpdateServiceOrderRequest {
  serviceId: string;
  targetServiceId: string;
  position: "before" | "after";
}
