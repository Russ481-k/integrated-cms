import { Box, Skeleton, VStack, HStack } from "@chakra-ui/react";

export function ServiceSkeleton() {
  return (
    <Box
      w="full"
      p={3}
      border="1px"
      borderColor="gray.200"
      borderRadius="md"
      bg="white"
    >
      <VStack gap={2} align="stretch">
        {/* 첫 번째 줄: 서비스 이름, 코드, 상태 */}
        <HStack justify="space-between" align="center">
          <HStack gap={2}>
            <Skeleton height="20px" width="120px" />
            <Skeleton height="16px" width="80px" />
          </HStack>
          <Skeleton height="16px" width="40px" />
        </HStack>

        {/* 두 번째 줄: 연결 정보 */}
        <HStack gap={4}>
          <Skeleton height="14px" width="150px" />
          <Skeleton height="14px" width="100px" />
        </HStack>

        {/* 세 번째 줄: 설명 */}
        <Skeleton height="14px" width="200px" />

        {/* 네 번째 줄: 메타 정보 */}
        <Skeleton height="12px" width="180px" />
      </VStack>
    </Box>
  );
}
