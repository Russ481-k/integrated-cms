"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Box, Heading, Text, Button, VStack } from "@chakra-ui/react";

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    // 통합 CMS는 바로 관리자 로그인 페이지로 리다이렉트
    const serviceType = process.env.NEXT_PUBLIC_SERVICE_TYPE;
    if (serviceType === "integrated-cms") {
      // 3초 후 자동 리다이렉트
      const timer = setTimeout(() => {
        router.push("/cms/login");
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [router]);

  return (
    <Box
      minH="100vh"
      display="flex"
      alignItems="center"
      justifyContent="center"
      bg="gray.50"
    >
      <VStack gap={6} textAlign="center" p={8}>
        <Heading size="xl" color="blue.600">
          {process.env.NEXT_PUBLIC_SERVICE_NAME || "통합 CMS 관리"}
        </Heading>

        <Text fontSize="lg" color="gray.600" maxW="md">
          통합 CMS 관리 시스템에 오신 것을 환영합니다. 시스템 관리자와 서비스
          관리자를 위한 통합 관리 플랫폼입니다.
        </Text>

        <VStack gap={3}>
          <Text fontSize="sm" color="gray.500">
            - 서비스 관리 및 모니터링
          </Text>
          <Text fontSize="sm" color="gray.500">
            - 통합 사용자 권한 관리
          </Text>
          <Text fontSize="sm" color="gray.500">
            - 시스템 설정 및 구성
          </Text>
        </VStack>

        <Button
          colorScheme="blue"
          size="lg"
          onClick={() => router.push("/cms/login")}
        >
          관리자 로그인
        </Button>

        <Text fontSize="xs" color="gray.400">
          3초 후 자동으로 로그인 페이지로 이동합니다...
        </Text>
      </VStack>
    </Box>
  );
}
