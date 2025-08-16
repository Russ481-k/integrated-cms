"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  Box,
  Button,
  Flex,
  Input,
  Textarea,
  VStack,
  HStack,
  Badge,
  Text,
  Separator,
} from "@chakra-ui/react";
import { Field } from "@chakra-ui/react";
import { NativeSelect } from "@chakra-ui/react";
import { Switch } from "@chakra-ui/react";
import { Alert } from "@chakra-ui/react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toaster } from "@/components/ui/toaster";
import { CreateServiceRequest, ServiceEditorProps } from "../types";
import { serviceUtils } from "@/lib/api/service";

// 서비스 폼 스키마 (Zod 검증)
const serviceFormSchema = z.object({
  serviceCode: z
    .string()
    .min(2, "서비스 코드는 최소 2자 이상이어야 합니다.")
    .max(50, "서비스 코드는 최대 50자까지 입력할 수 있습니다.")
    .regex(
      /^[a-zA-Z0-9_-]+$/,
      "서비스 코드는 영문, 숫자, 하이픈, 언더스코어만 사용할 수 있습니다."
    ),
  serviceName: z
    .string()
    .min(1, "서비스 이름을 입력해주세요.")
    .max(100, "서비스 이름은 최대 100자까지 입력할 수 있습니다."),
  serviceDomain: z
    .string()
    .url("유효한 URL 형식을 입력해주세요.")
    .optional()
    .or(z.literal("")),
  apiBaseUrl: z
    .string()
    .url("유효한 URL 형식을 입력해주세요.")
    .optional()
    .or(z.literal("")),
  status: z.enum(["ACTIVE", "INACTIVE", "MAINTENANCE"]),
  description: z
    .string()
    .max(1000, "설명은 최대 1000자까지 입력할 수 있습니다.")
    .optional(),
});

type ServiceFormData = z.infer<typeof serviceFormSchema>;

export const ServiceEditor: React.FC<ServiceEditorProps> = ({
  service,
  onClose,
  onDelete,
  onSubmit,
  onAddService,
  isTempService = false,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);

  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isDirty, isValid },
  } = useForm<ServiceFormData>({
    resolver: zodResolver(serviceFormSchema),
    defaultValues: {
      serviceCode: "",
      serviceName: "",
      serviceDomain: "",
      apiBaseUrl: "",
      status: "ACTIVE",
      description: "",
    },
  });

  const watchedStatus = watch("status");

  // 서비스 데이터가 변경될 때 폼 초기화
  useEffect(() => {
    if (service) {
      reset({
        serviceCode: service.serviceCode || "",
        serviceName: service.serviceName || "",
        serviceDomain: service.serviceDomain || "",
        apiBaseUrl: service.apiBaseUrl || "",
        status: service.status || "ACTIVE",
        description: service.description || "",
      });
    } else {
      reset({
        serviceCode: "",
        serviceName: "",
        serviceDomain: "",
        apiBaseUrl: "",
        status: "ACTIVE",
        description: "",
      });
    }
  }, [service, reset]);

  // 폼 제출 처리
  const handleFormSubmit = useCallback(
    async (data: ServiceFormData) => {
      try {
        setIsSubmitting(true);

        // 빈 문자열을 undefined로 변환
        const submitData: CreateServiceRequest = {
          serviceCode: data.serviceCode,
          serviceName: data.serviceName,
          serviceDomain: data.serviceDomain || undefined,
          apiBaseUrl: data.apiBaseUrl || undefined,
          status: data.status,
          description: data.description || undefined,
        };

        await onSubmit(submitData);

        toaster.create({
          title: isTempService
            ? "서비스가 생성되었습니다."
            : "서비스가 수정되었습니다.",
          type: "success",
        });
      } catch (error) {
        console.error("Error submitting service:", error);
        toaster.create({
          title: isTempService
            ? "서비스 생성에 실패했습니다."
            : "서비스 수정에 실패했습니다.",
          type: "error",
        });
      } finally {
        setIsSubmitting(false);
      }
    },
    [onSubmit, isTempService]
  );

  // 서비스 삭제 처리
  const handleDelete = useCallback(async () => {
    if (!service) return;

    const confirmMessage = isTempService
      ? "임시 서비스를 취소하시겠습니까?"
      : `"${service.serviceName}" 서비스를 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`;

    if (window.confirm(confirmMessage)) {
      try {
        await onDelete(service.serviceId);
      } catch (error) {
        console.error("Error deleting service:", error);
        toaster.create({
          title: "서비스 삭제에 실패했습니다.",
          type: "error",
        });
      }
    }
  }, [service, onDelete, isTempService]);

  // 서비스가 선택되지 않은 경우
  if (!service) {
    return (
      <Box p={2} textAlign="center">
        <Text color="gray.500" mb={4}>
          편집할 서비스를 선택하거나 새 서비스를 추가해주세요.
        </Text>
        <Button colorScheme="blue" onClick={onAddService}>
          새 서비스 추가
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <form onSubmit={handleSubmit(handleFormSubmit)}>
        <VStack gap={2} align="stretch">
          {/* 헤더 */}
          <Flex justify="space-between" align="center">
            <Flex align="center" gap={3}>
              <Text fontSize="lg" fontWeight="bold">
                {isTempService ? "새 서비스 추가" : "서비스 편집"}
              </Text>
              <Badge
                colorScheme={serviceUtils.getStatusColor(service.status)}
                variant="subtle"
              >
                {serviceUtils.getStatusText(service.status)}
              </Badge>
            </Flex>
            <HStack gap={2}>
              <Button variant="ghost" size="sm" onClick={onClose}>
                취소
              </Button>
              {!isTempService && (
                <Button
                  colorScheme="red"
                  variant="ghost"
                  size="sm"
                  onClick={handleDelete}
                >
                  삭제
                </Button>
              )}
            </HStack>
          </Flex>

          {/* 임시 서비스 알림 */}
          {isTempService && (
            <Alert.Root status="info" borderRadius="md">
              <Alert.Indicator />
              <Alert.Content>
                <Alert.Title>새 서비스 추가 중</Alert.Title>
                <Alert.Description>
                  모든 필수 정보를 입력한 후 저장 버튼을 클릭해주세요.
                </Alert.Description>
              </Alert.Content>
            </Alert.Root>
          )}

          {/* 기본 정보 */}
          <VStack gap={4} align="stretch">
            <Text fontSize="md" fontWeight="semibold" color="gray.700">
              기본 정보
            </Text>

            <Field.Root invalid={!!errors.serviceCode} required>
              <Field.Label>서비스 코드</Field.Label>
              <Controller
                name="serviceCode"
                control={control}
                render={({ field }) => (
                  <Input
                    {...field}
                    placeholder="서비스 고유 코드"
                    maxLength={50}
                  />
                )}
              />
              {errors.serviceCode && (
                <Field.ErrorText>{errors.serviceCode.message}</Field.ErrorText>
              )}
            </Field.Root>

            <Field.Root invalid={!!errors.serviceName} required>
              <Field.Label>서비스 이름</Field.Label>
              <Controller
                name="serviceName"
                control={control}
                render={({ field }) => (
                  <Input
                    {...field}
                    placeholder="서비스 표시 이름"
                    maxLength={100}
                  />
                )}
              />
              {errors.serviceName && (
                <Field.ErrorText>{errors.serviceName.message}</Field.ErrorText>
              )}
            </Field.Root>

            <Field.Root invalid={!!errors.status} required>
              <Field.Label>서비스 상태</Field.Label>
              <Controller
                name="status"
                control={control}
                render={({ field }) => (
                  <NativeSelect.Root>
                    <NativeSelect.Field {...field}>
                      <option value="ACTIVE">활성</option>
                      <option value="INACTIVE">비활성</option>
                      <option value="MAINTENANCE">점검중</option>
                    </NativeSelect.Field>
                    <NativeSelect.Indicator />
                  </NativeSelect.Root>
                )}
              />
              {errors.status && (
                <Field.ErrorText>{errors.status.message}</Field.ErrorText>
              )}
            </Field.Root>
          </VStack>

          {/* 연결 정보 */}
          <VStack gap={4} align="stretch">
            <HStack justify="space-between" align="center">
              <Text fontSize="md" fontWeight="semibold" color="gray.700">
                연결 정보
              </Text>
              <Switch.Root
                checked={showAdvanced}
                onCheckedChange={(checked) => setShowAdvanced(checked.checked)}
                size="sm"
              >
                <Switch.Indicator />
                <Switch.Label>고급 설정</Switch.Label>
              </Switch.Root>
            </HStack>

            <Field.Root invalid={!!errors.serviceDomain}>
              <Field.Label>서비스 도메인</Field.Label>
              <Controller
                name="serviceDomain"
                control={control}
                render={({ field }) => (
                  <Input
                    {...field}
                    placeholder="https://example.com"
                    type="url"
                  />
                )}
              />
              {errors.serviceDomain && (
                <Field.ErrorText>
                  {errors.serviceDomain.message}
                </Field.ErrorText>
              )}
            </Field.Root>

            {showAdvanced && (
              <Field.Root invalid={!!errors.apiBaseUrl}>
                <Field.Label>API 기본 URL</Field.Label>
                <Controller
                  name="apiBaseUrl"
                  control={control}
                  render={({ field }) => (
                    <Input
                      {...field}
                      placeholder="https://api.example.com"
                      type="url"
                    />
                  )}
                />
                {errors.apiBaseUrl && (
                  <Field.ErrorText>{errors.apiBaseUrl.message}</Field.ErrorText>
                )}
              </Field.Root>
            )}
          </VStack>

          {/* 설명 */}
          <Field.Root invalid={!!errors.description}>
            <Field.Label>서비스 설명</Field.Label>
            <Controller
              name="description"
              control={control}
              render={({ field }) => (
                <Textarea
                  {...field}
                  placeholder="서비스에 대한 설명을 입력해주세요."
                  rows={3}
                  maxLength={1000}
                />
              )}
            />
            {errors.description && (
              <Field.ErrorText>{errors.description.message}</Field.ErrorText>
            )}
          </Field.Root>

          <Separator />

          {/* 액션 버튼 */}
          <HStack justify="space-between">
            <Button
              variant="outline"
              onClick={onAddService}
              disabled={isSubmitting}
            >
              새 서비스 추가
            </Button>
            <HStack>
              <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>
                취소
              </Button>
              <Button
                type="submit"
                colorScheme="blue"
                loading={isSubmitting}
                disabled={!isDirty || !isValid}
              >
                {isSubmitting
                  ? isTempService
                    ? "생성 중..."
                    : "저장 중..."
                  : isTempService
                  ? "생성"
                  : "저장"}
              </Button>
            </HStack>
          </HStack>
        </VStack>
      </form>
    </Box>
  );
};
