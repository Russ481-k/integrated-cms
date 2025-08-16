"use client";

import React, { useState, useCallback } from "react";
import {
  Box,
  Button,
  Flex,
  Input,
  VStack,
  HStack,
  Text,
  Badge,
} from "@chakra-ui/react";
import { NativeSelect } from "@chakra-ui/react";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { PlusIcon } from "lucide-react";
import { ServiceSkeleton } from "./ServiceSkeleton";
import { ServiceListProps, ServiceStatus, ServiceFilters } from "../types";
import { serviceUtils } from "@/lib/api/service";
import { ServiceItem } from "./ServiceItem";

export const ServiceList: React.FC<ServiceListProps> = ({
  services,
  onEditService,
  onDeleteService,
  onMoveService,
  onAddService,
  isLoading,
  selectedServiceId,
}) => {
  const [filters, setFilters] = useState<ServiceFilters>({
    search: "",
    status: undefined,
  });

  // 필터링된 서비스 목록
  const filteredServices = React.useMemo(() => {
    let filtered = [...services];

    // 상태 필터
    if (filters.status && filters.status.length > 0) {
      filtered = filtered.filter((service) =>
        filters.status!.includes(service.status)
      );
    }

    // 검색어 필터
    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      filtered = filtered.filter(
        (service) =>
          service.serviceName.toLowerCase().includes(searchLower) ||
          service.serviceCode.toLowerCase().includes(searchLower) ||
          (service.description &&
            service.description.toLowerCase().includes(searchLower))
      );
    }

    // 정렬 (활성 상태 우선, 이름 순)
    return serviceUtils.sortServices(filtered);
  }, [services, filters]);

  // 검색어 변경 처리
  const handleSearchChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setFilters((prev) => ({
        ...prev,
        search: event.target.value,
      }));
    },
    []
  );

  // 필터 초기화
  const handleClearFilters = useCallback(() => {
    setFilters({
      search: "",
      status: undefined,
    });
  }, []);

  // 상태별 카운트 계산
  const statusCounts = React.useMemo(() => {
    const counts = { ACTIVE: 0, INACTIVE: 0, MAINTENANCE: 0 };
    services.forEach((service) => {
      counts[service.status]++;
    });
    return counts;
  }, [services]);

  // 로딩 상태
  if (isLoading) {
    return (
      <Box p={4}>
        <VStack gap={2}>
          {Array.from({ length: 5 }).map((_, index) => (
            <ServiceSkeleton key={index} />
          ))}
        </VStack>
      </Box>
    );
  }

  return (
    <Box>
      {/* 헤더 */}
      <Box borderBottom="1px" borderColor="gray.200">
        {/* 필터링 */}
        <VStack gap={2}>
          <HStack gap={1} w="full">
            <Input
              placeholder="서비스 이름, 코드로 검색..."
              value={filters.search}
              onChange={handleSearchChange}
              size="xs"
            />
            <Button colorScheme="blue" size="xs" onClick={onAddService}>
              <PlusIcon size={16} />새 서비스
            </Button>
          </HStack>
          <HStack w="full" gap={2}>
            <Flex w="full" gap={2} justify="space-between">
              <NativeSelect.Root size="sm" w={120}>
                <NativeSelect.Field
                  placeholder="전체 상태"
                  value={filters.status?.[0] || ""}
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                    const value = e.target.value;
                    setFilters((prev) => ({
                      ...prev,
                      status: value ? [value as ServiceStatus] : undefined,
                    }));
                  }}
                >
                  <option value="">전체 상태</option>
                  <option value="ACTIVE">활성 ({statusCounts.ACTIVE})</option>
                  <option value="MAINTENANCE">
                    점검중 ({statusCounts.MAINTENANCE})
                  </option>
                  <option value="INACTIVE">
                    비활성 ({statusCounts.INACTIVE})
                  </option>
                </NativeSelect.Field>
              </NativeSelect.Root>
              {/* 상태 요약 */}
              <HStack gap={2} mt={3} justify="center" align="center">
                <Badge colorScheme="green" variant="subtle">
                  활성 {statusCounts.ACTIVE}
                </Badge>
                <Badge colorScheme="orange" variant="subtle">
                  점검중 {statusCounts.MAINTENANCE}
                </Badge>
                <Badge colorScheme="gray" variant="subtle">
                  비활성 {statusCounts.INACTIVE}
                </Badge>
              </HStack>
            </Flex>
            {(filters.search || filters.status) && (
              <Button size="sm" variant="ghost" onClick={handleClearFilters}>
                초기화
              </Button>
            )}
          </HStack>
        </VStack>
      </Box>

      {/* 서비스 목록 */}
      <Box flex={1} overflow="auto">
        {filteredServices.length === 0 ? (
          <Box p={6} textAlign="center">
            <Text color="gray.500" mb={2}>
              {filters.search || filters.status
                ? "검색 조건에 맞는 서비스가 없습니다."
                : "등록된 서비스가 없습니다."}
            </Text>
            {!filters.search && !filters.status && (
              <Button
                colorScheme="blue"
                variant="outline"
                onClick={onAddService}
              >
                첫 번째 서비스 추가하기
              </Button>
            )}
          </Box>
        ) : (
          <DndProvider backend={HTML5Backend}>
            <VStack gap={1} p={2}>
              {filteredServices.map((service, index) => (
                <ServiceItem
                  key={service.serviceId}
                  service={service}
                  index={index}
                  onEditService={onEditService}
                  onDeleteService={onDeleteService}
                  onMoveService={onMoveService}
                  selectedServiceId={selectedServiceId}
                />
              ))}
            </VStack>
          </DndProvider>
        )}
      </Box>

      {/* 푸터 */}
      <Box p={3} borderTop="1px" borderColor="gray.200" bg="gray.50">
        <Text fontSize="xs" color="gray.600" textAlign="center">
          총 {services.length}개 서비스
          {filteredServices.length !== services.length &&
            ` (${filteredServices.length}개 표시)`}
        </Text>
      </Box>
    </Box>
  );
};
