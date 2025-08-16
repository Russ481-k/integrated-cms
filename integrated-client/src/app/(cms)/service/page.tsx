"use client";

import { Badge, Box, Flex, Heading, NativeSelect } from "@chakra-ui/react";
import React from "react";
import { GridSection } from "@/components/ui/grid-section";
import { useColors } from "@/styles/theme";
import { Toaster } from "@/components/ui/toaster";
import { Main } from "@/components/layout/view/Main";

import { ServiceList } from "./components/ServiceList";
import { ServiceEditor } from "./components/ServiceEditor";
import { useServiceManagement } from "./hooks/useServiceManagement";

export default function ServiceManagementPage() {
  const colors = useColors();

  // 서비스 관리 훅 사용
  const {
    services,
    selectedService,
    tempService,
    loadingServiceId,
    isLoading,
    handleAddService,
    handleEditService,
    handleDeleteService,
    handleSubmitService,
    handleCloseEditor,
  } = useServiceManagement();

  // 서비스 순서 변경 핸들러 (현재는 구현하지 않음)
  const handleMoveService = (
    draggedServiceId: string,
    targetServiceId: string,
    position: "before" | "after"
  ) => {
    console.log("Service move:", {
      draggedServiceId,
      targetServiceId,
      position,
    });
    // TODO: 서비스 순서 변경 API 구현 후 연결
  };

  // 서비스 관리 페이지 레이아웃 정의
  const serviceLayout = [
    {
      id: "header",
      x: 0,
      y: 0,
      w: 12,
      h: 1,
      isStatic: true,
      isHeader: true,
    },
    {
      id: "serviceList",
      x: 0,
      y: 1,
      w: 3,
      h: 5,
      title: "서비스 목록",
      subtitle: "등록된 서비스들을 관리할 수 있습니다.",
    },
    {
      id: "serviceEditor",
      x: 0,
      y: 6,
      w: 3,
      h: 6,
      title: "서비스 편집",
      subtitle: "서비스의 상세 정보를 수정할 수 있습니다.",
    },
    {
      id: "preview",
      x: 3,
      y: 1,
      w: 9,
      h: 11,
      title: "서비스 현황",
      subtitle: "서비스 상태 및 설정 정보 미리보기입니다.",
    },
  ];

  return (
    <Box bg={colors.bg} minH="100vh" w="full" position="relative">
      <Box w="full">
        <GridSection initialLayout={serviceLayout}>
          {/* 헤더 */}
          <Flex justify="space-between" align="center" h="36px">
            <Flex align="center" gap={2} px={2}>
              <Heading
                size="lg"
                color={colors.text.primary}
                letterSpacing="tight"
              >
                서비스 관리
              </Heading>
              <Badge
                bg={colors.secondary.light}
                color={colors.secondary.default}
                px={2}
                py={1}
                borderRadius="md"
                fontSize="xs"
                fontWeight="bold"
              >
                통합 관리자
              </Badge>
            </Flex>
            <Flex>
              <NativeSelect.Root>
                <NativeSelect.Field>
                  <option value="all">전체 서비스</option>
                  <option value="active">활성 서비스</option>
                  <option value="inactive">비활성 서비스</option>
                </NativeSelect.Field>
              </NativeSelect.Root>
            </Flex>
          </Flex>

          {/* 서비스 목록 */}
          <Box>
            <ServiceList
              services={services}
              onEditService={handleEditService}
              onDeleteService={handleDeleteService}
              onMoveService={handleMoveService}
              onAddService={handleAddService}
              isLoading={isLoading}
              selectedServiceId={selectedService?.serviceId}
            />
          </Box>

          {/* 서비스 편집기 */}
          <Box>
            <ServiceEditor
              service={selectedService}
              onClose={handleCloseEditor}
              onDelete={handleDeleteService}
              onSubmit={handleSubmitService}
              onAddService={handleAddService}
              isTempService={!!tempService}
            />
          </Box>

          {/* 미리보기 */}
          <Box>
            <Main menus={[]} isPreview={true} currentPage="service-preview">
              <Box p={6}>
                <Heading size="md" mb={4}>
                  서비스 현황 대시보드
                </Heading>

                {selectedService ? (
                  <Box>
                    <Heading size="sm" mb={2}>
                      선택된 서비스: {selectedService.serviceName}
                    </Heading>
                    <Box bg="gray.50" p={4} borderRadius="md">
                      <div>
                        <strong>서비스 코드:</strong>{" "}
                        {selectedService.serviceCode}
                      </div>
                      <div>
                        <strong>상태:</strong> {selectedService.status}
                      </div>
                      {selectedService.serviceDomain && (
                        <div>
                          <strong>도메인:</strong>{" "}
                          {selectedService.serviceDomain}
                        </div>
                      )}
                      {selectedService.apiBaseUrl && (
                        <div>
                          <strong>API URL:</strong> {selectedService.apiBaseUrl}
                        </div>
                      )}
                      {selectedService.description && (
                        <div>
                          <strong>설명:</strong> {selectedService.description}
                        </div>
                      )}
                    </Box>
                  </Box>
                ) : (
                  <Box textAlign="center" color="gray.500">
                    서비스를 선택하면 상세 정보가 표시됩니다.
                  </Box>
                )}
              </Box>
            </Main>
          </Box>
        </GridSection>
      </Box>
      <Toaster />
    </Box>
  );
}
