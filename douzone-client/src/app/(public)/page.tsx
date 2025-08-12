"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Box, Heading, Text, Button, VStack, HStack, Badge } from "@chakra-ui/react";

export default function Home() {
  const router = useRouter();

  return (
    <Box 
      minH="100vh" 
      display="flex" 
      alignItems="center" 
      justifyContent="center"
      bg="gradient-to-br from-green.50 to-blue.50"
    >
      <VStack spacing={6} textAlign="center" p={8}>
        <HStack spacing={3} alignItems="center">
          <Heading size="xl" color="green.600">
            {process.env.NEXT_PUBLIC_SERVICE_NAME || '더존 CMS'}
          </Heading>
          <Badge colorScheme="green" variant="subtle">서비스</Badge>
        </HStack>
        
        <Text fontSize="lg" color="gray.600" maxW="md">
          더존 스마트 워크 CMS에 오신 것을 환영합니다. 
          효율적인 콘텐츠 관리와 업무 자동화를 제공합니다.
        </Text>

        <VStack spacing={3}>
          <Text fontSize="sm" color="gray.500">
            - 스마트 콘텐츠 관리
          </Text>
          <Text fontSize="sm" color="gray.500">
            - 업무 프로세스 자동화  
          </Text>
          <Text fontSize="sm" color="gray.500">
            - 협업 도구 통합
          </Text>
        </VStack>

        <HStack spacing={4}>
          <Button 
            colorScheme="green" 
            size="lg"
            onClick={() => router.push('/cms/login')}
          >
            관리자 로그인
          </Button>
          <Button 
            variant="outline" 
            colorScheme="green"
            size="lg"
            onClick={() => router.push('/login')}
          >
            사용자 로그인
          </Button>
        </HStack>

        <Text fontSize="xs" color="gray.400">
          Service ID: douzone | API: {process.env.NEXT_PUBLIC_API_BASE_URL}
        </Text>
      </VStack>
    </Box>
  );
}
