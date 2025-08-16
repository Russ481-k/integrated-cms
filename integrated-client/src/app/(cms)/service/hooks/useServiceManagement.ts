import { useState, useCallback } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toaster } from "@/components/ui/toaster";
import { serviceApi, serviceKeys } from "@/lib/api/service";
import {
  Service,
  CreateServiceRequest,
  UpdateServiceRequest,
  ServiceStatus,
  ServiceFilters,
} from "../types";

/**
 * 서비스 관리를 위한 커스텀 훅
 * 메뉴 관리 훅과 달리 계층구조가 없는 단순한 CRUD 관리
 */
export const useServiceManagement = (filters?: ServiceFilters) => {
  const queryClient = useQueryClient();
  const [selectedService, setSelectedService] = useState<Service | null>(null);
  const [tempService, setTempService] = useState<Service | null>(null);
  const [loadingServiceId, setLoadingServiceId] = useState<string | null>(null);

  // 서비스 목록 조회
  const {
    data: servicesResponse,
    isLoading: isServicesLoading,
    error: servicesError,
    refetch: refetchServices,
  } = useQuery({
    queryKey: serviceKeys.list(filters),
    queryFn: async () => {
      const response = await serviceApi.getServices();
      return response;
    },
    staleTime: 5 * 60 * 1000, // 5분간 캐시 유지
  });

  const services = servicesResponse?.data || [];

  // 서비스 생성 뮤테이션
  const createServiceMutation = useMutation({
    mutationFn: serviceApi.createService,
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: serviceKeys.lists() });
      setSelectedService(response.data);
      setTempService(null);
      toaster.create({
        title: "서비스가 성공적으로 생성되었습니다.",
        type: "success",
      });
    },
    onError: (error: any) => {
      console.error("Error creating service:", error);
      toaster.create({
        title: "서비스 생성에 실패했습니다.",
        description: error?.response?.data?.message || error.message,
        type: "error",
      });
    },
  });

  // 서비스 수정 뮤테이션
  const updateServiceMutation = useMutation({
    mutationFn: ({ serviceId, data }: { serviceId: string; data: UpdateServiceRequest }) =>
      serviceApi.updateService(serviceId, data),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: serviceKeys.lists() });
      setSelectedService(response.data);
      toaster.create({
        title: "서비스가 성공적으로 수정되었습니다.",
        type: "success",
      });
    },
    onError: (error: any) => {
      console.error("Error updating service:", error);
      toaster.create({
        title: "서비스 수정에 실패했습니다.",
        description: error?.response?.data?.message || error.message,
        type: "error",
      });
    },
  });

  // 서비스 삭제 뮤테이션
  const deleteServiceMutation = useMutation({
    mutationFn: serviceApi.deleteService,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: serviceKeys.lists() });
      setSelectedService(null);
      toaster.create({
        title: "서비스가 성공적으로 삭제되었습니다.",
        type: "success",
      });
    },
    onError: (error: any) => {
      console.error("Error deleting service:", error);
      toaster.create({
        title: "서비스 삭제에 실패했습니다.",
        description: error?.response?.data?.message || error.message,
        type: "error",
      });
    },
  });

  // 서비스 상태 변경 뮤테이션
  const updateStatusMutation = useMutation({
    mutationFn: ({ serviceId, status }: { serviceId: string; status: ServiceStatus }) =>
      serviceApi.updateServiceStatus(serviceId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: serviceKeys.lists() });
      toaster.create({
        title: "서비스 상태가 성공적으로 변경되었습니다.",
        type: "success",
      });
    },
    onError: (error: any) => {
      console.error("Error updating service status:", error);
      toaster.create({
        title: "서비스 상태 변경에 실패했습니다.",
        description: error?.response?.data?.message || error.message,
        type: "error",
      });
    },
  });

  // 임시 서비스 생성 함수
  const handleAddService = useCallback(() => {
    const newTempService: Service = {
      serviceId: `temp-${Date.now()}`, // 임시 ID
      serviceCode: "",
      serviceName: "새 서비스",
      status: "ACTIVE",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    setTempService(newTempService);
    setSelectedService(newTempService);
  }, []);

  // 서비스 편집 함수
  const handleEditService = useCallback(
    (service: Service) => {
      if (tempService && tempService.serviceId !== service.serviceId) {
        // 다른 임시 서비스가 있는 경우 경고
        if (window.confirm("새 서비스 추가가 취소됩니다. 계속하시겠습니까?")) {
          setTempService(null);
          setSelectedService(service);
        }
      } else {
        setSelectedService(service);
      }
    },
    [tempService]
  );

  // 서비스 삭제 함수
  const handleDeleteService = useCallback(
    async (serviceId: string) => {
      try {
        setLoadingServiceId(serviceId);

        if (tempService && tempService.serviceId === serviceId) {
          // 임시 서비스 삭제
          setTempService(null);
          setSelectedService(null);
        } else {
          // 실제 서비스 삭제
          await deleteServiceMutation.mutateAsync(serviceId);
        }
      } finally {
        setLoadingServiceId(null);
      }
    },
    [deleteServiceMutation, tempService]
  );

  // 서비스 저장 함수
  const handleSubmitService = useCallback(
    async (serviceData: CreateServiceRequest) => {
      try {
        const serviceId = tempService ? undefined : selectedService?.serviceId;

        if (serviceId) {
          // 기존 서비스 수정
          setLoadingServiceId(serviceId);
          await updateServiceMutation.mutateAsync({
            serviceId,
            data: serviceData,
          });
        } else {
          // 새 서비스 생성
          await createServiceMutation.mutateAsync(serviceData);
        }
      } catch (error) {
        console.error("Error saving service:", error);
        throw error;
      } finally {
        setLoadingServiceId(null);
      }
    },
    [createServiceMutation, updateServiceMutation, selectedService, tempService]
  );

  // 서비스 상태 변경 함수
  const handleUpdateServiceStatus = useCallback(
    async (serviceId: string, status: ServiceStatus) => {
      try {
        setLoadingServiceId(serviceId);
        await updateStatusMutation.mutateAsync({ serviceId, status });
      } finally {
        setLoadingServiceId(null);
      }
    },
    [updateStatusMutation]
  );

  // 에디터 닫기 함수
  const handleCloseEditor = useCallback(() => {
    if (tempService) {
      setTempService(null);
    }
    setSelectedService(null);
  }, [tempService]);

  return {
    // 상태
    services,
    selectedService,
    tempService,
    loadingServiceId,
    isLoading: isServicesLoading,
    error: servicesError,

    // 액션
    handleAddService,
    handleEditService,
    handleDeleteService,
    handleSubmitService,
    handleUpdateServiceStatus,
    handleCloseEditor,
    refetchServices,

    // 선택 관리
    setSelectedService,
    setTempService,

    // 뮤테이션 상태
    isCreating: createServiceMutation.isPending,
    isUpdating: updateServiceMutation.isPending,
    isDeleting: deleteServiceMutation.isPending,
    isUpdatingStatus: updateStatusMutation.isPending,
  };
};
