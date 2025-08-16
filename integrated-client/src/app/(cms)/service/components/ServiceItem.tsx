"use client";

import React, { useRef } from "react";
import { useDrag, useDrop } from "react-dnd";
import {
  Box,
  Flex,
  Text,
  Badge,
  IconButton,
  HStack,
  VStack,
} from "@chakra-ui/react";
import { Tooltip } from "@chakra-ui/react";
import {
  EditIcon,
  TrashIcon,
  GripVerticalIcon,
  ExternalLinkIcon,
  DatabaseIcon,
  ServerIcon,
} from "lucide-react";
import { ServiceItemProps, ServiceDragItem } from "../types";
import { serviceUtils } from "@/lib/api/service";

const DRAG_TYPE = "SERVICE_ITEM";

export const ServiceItem: React.FC<ServiceItemProps> = ({
  service,
  index,
  onEditService,
  onDeleteService,
  onMoveService,
  selectedServiceId,
  isLoading = false,
}) => {
  const ref = useRef<HTMLDivElement>(null);

  // 색상 테마 (v3에서는 CSS variables 사용)
  const bgColor = "white";
  const hoverBgColor = "gray.50";
  const selectedBgColor = "blue.50";
  const borderColor = "gray.200";
  const selectedBorderColor = "blue.300";

  const isSelected = selectedServiceId === service.serviceId;
  const isTempService = service.serviceId.startsWith("temp-");

  // 드래그 앤 드롭 설정
  const [{ isDragging }, drag] = useDrag({
    type: DRAG_TYPE,
    item: (): ServiceDragItem => ({
      serviceId: service.serviceId,
      type: DRAG_TYPE,
      index,
    }),
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
    canDrag: !isTempService && !isLoading,
  });

  const [{ isOver, canDrop }, drop] = useDrop({
    accept: DRAG_TYPE,
    hover: (draggedItem: ServiceDragItem) => {
      if (draggedItem.serviceId === service.serviceId) return;

      // 임시 서비스나 로딩 중일 때는 드롭 불가
      if (isTempService || isLoading) return;
    },
    drop: (draggedItem: ServiceDragItem) => {
      if (draggedItem.serviceId === service.serviceId) return;

      // 드래그된 아이템의 인덱스와 현재 아이템의 인덱스를 비교해서 위치 결정
      const position = draggedItem.index < index ? "after" : "before";
      onMoveService(draggedItem.serviceId, service.serviceId, position);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop(),
    }),
  });

  // 드래그 앤 드롭 ref 연결
  drag(drop(ref));

  // 클릭 핸들러
  const handleClick = () => {
    onEditService(service);
  };

  const handleEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    onEditService(service);
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDeleteService(service.serviceId);
  };

  // 스타일 계산
  const getBorderColor = () => {
    if (isSelected) return selectedBorderColor;
    if (isOver && canDrop) return "blue.400";
    return borderColor;
  };

  const getBgColor = () => {
    if (isSelected) return selectedBgColor;
    if (isOver && canDrop) return "blue.100";
    return bgColor;
  };

  return (
    <Box
      ref={ref}
      w="full"
      p={3}
      bg={getBgColor()}
      border="1px"
      borderColor={getBorderColor()}
      borderRadius="md"
      cursor="pointer"
      opacity={isDragging ? 0.5 : 1}
      transition="all 0.2s"
      _hover={{
        bg: isSelected ? selectedBgColor : hoverBgColor,
        borderColor: isSelected ? selectedBorderColor : "gray.300",
      }}
      onClick={handleClick}
    >
      <Flex gap={3} align="flex-start">
        {/* 드래그 핸들 */}
        {!isTempService && (
          <Box
            cursor="grab"
            _active={{ cursor: "grabbing" }}
            color="gray.400"
            _hover={{ color: "gray.600" }}
            pt={1}
          >
            <GripVerticalIcon size={16} />
          </Box>
        )}

        {/* 서비스 정보 */}
        <VStack flex={1} align="stretch" gap={2}>
          {/* 첫 번째 줄: 서비스 이름, 코드, 상태 */}
          <Flex justify="space-between" align="center">
            <HStack gap={2}>
              <Text fontWeight="bold" fontSize="md" lineHeight="short">
                {service.serviceName}
              </Text>
              <Text
                fontSize="sm"
                color="gray.500"
                fontFamily="mono"
                bg="gray.100"
                px={2}
                py={1}
                borderRadius="sm"
              >
                {service.serviceCode}
              </Text>
              {isTempService && (
                <Badge colorScheme="yellow" variant="subtle" fontSize="xs">
                  임시
                </Badge>
              )}
            </HStack>

            <Badge
              colorScheme={serviceUtils.getStatusColor(service.status)}
              variant="subtle"
              fontSize="xs"
            >
              {serviceUtils.getStatusText(service.status)}
            </Badge>
          </Flex>

          {/* 두 번째 줄: 도메인 및 연결 정보 */}
          {(service.serviceDomain || service.apiBaseUrl) && (
            <HStack gap={4} fontSize="sm" color="gray.600">
              {service.serviceDomain && (
                <HStack gap={1}>
                  <ExternalLinkIcon size={12} />
                  <Text>{service.serviceDomain}</Text>
                </HStack>
              )}
              {service.apiBaseUrl && (
                <HStack gap={1}>
                  <ServerIcon size={12} />
                  <Text>{service.apiBaseUrl}</Text>
                </HStack>
              )}
              {service.dbConnectionInfo && (
                <HStack gap={1}>
                  <DatabaseIcon size={12} />
                  <Text>DB 연결됨</Text>
                </HStack>
              )}
            </HStack>
          )}

          {/* 세 번째 줄: 설명 */}
          {service.description && (
            <Text
              fontSize="sm"
              color="gray.600"
              lineClamp={2}
              css={{
                display: "-webkit-box",
                WebkitLineClamp: 2,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
              }}
            >
              {service.description}
            </Text>
          )}

          {/* 네 번째 줄: 메타 정보 */}
          <Text fontSize="xs" color="gray.400">
            생성: {new Date(service.createdAt).toLocaleDateString("ko-KR")}
            {service.updatedAt !== service.createdAt && (
              <>
                {" • "}
                수정: {new Date(service.updatedAt).toLocaleDateString("ko-KR")}
              </>
            )}
          </Text>
        </VStack>

        {/* 액션 버튼 */}
        <VStack gap={1}>
          <Tooltip.Root>
            <Tooltip.Trigger asChild>
              <IconButton
                aria-label="편집"
                size="sm"
                variant="ghost"
                colorScheme="blue"
                onClick={handleEdit}
              >
                <EditIcon size={14} />
              </IconButton>
            </Tooltip.Trigger>
            <Tooltip.Content>편집</Tooltip.Content>
          </Tooltip.Root>

          <Tooltip.Root>
            <Tooltip.Trigger asChild>
              <IconButton
                aria-label={isTempService ? "취소" : "삭제"}
                size="sm"
                variant="ghost"
                colorScheme="red"
                onClick={handleDelete}
                loading={isLoading}
              >
                <TrashIcon size={14} />
              </IconButton>
            </Tooltip.Trigger>
            <Tooltip.Content>{isTempService ? "취소" : "삭제"}</Tooltip.Content>
          </Tooltip.Root>
        </VStack>
      </Flex>
    </Box>
  );
};
