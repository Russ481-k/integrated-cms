"use client";

import React, { useEffect, ReactNode } from "react";
import { useRecoilValue } from "recoil";
import { authState, AppUser } from "@/stores/auth";
import { usePathname, useRouter } from "next/navigation";
import { Spinner, Flex, Box } from "@chakra-ui/react";
import { PermissionGuard } from "./PermissionGuard";

interface AuthGuardProps {
  children: ReactNode;
  allowedRoles?: Array<AppUser["role"]>;
  redirectTo?: string;
}

export const AuthGuard = ({
  children,
  allowedRoles,
  redirectTo = "/login",
}: AuthGuardProps) => {
  const { user, isAuthenticated, isLoading, isLoggingOut } =
    useRecoilValue(authState);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    // 로딩 중이거나 로그아웃 중이면 아무것도 하지 않음
    if (isLoading || isLoggingOut) {
      return;
    }

    // 인증되지 않은 경우
    if (!isAuthenticated) {
      const publicPaths = [
        "/login",
        "/signup",
        "/find-credentials/id",
        "/find-credentials/password",
      ];

      if (!publicPaths.includes(pathname) && pathname !== redirectTo) {
        router.push(redirectTo);
      }
      return;
    }

    // 인증되었지만 사용자 정보가 없는 경우 (안전장치)
    if (!user) {
      router.push(redirectTo);
      return;
    }
  }, [
    isLoading,
    isLoggingOut,
    isAuthenticated,
    user,
    pathname,
    redirectTo,
    router,
  ]);

  if (isLoading) {
    return (
      <Flex justify="center" align="center" minH={"100vh"}>
        <Box textAlign="center">
          <Spinner size="xl" color="blue.500" mb={4} />
        </Box>
      </Flex>
    );
  }

  // 인증되지 않은 경우 children을 렌더링하지 않음
  if (!isAuthenticated) {
    return null;
  }

  // 인증된 경우 권한 체크를 위해 PermissionGuard로 래핑
  return (
    <PermissionGuard allowedRoles={allowedRoles}>{children}</PermissionGuard>
  );
};
