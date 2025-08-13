"use client";

import { Box } from "@chakra-ui/react";
import { useColors } from "@/styles/theme";
import { RootLayoutClient } from "@/components/layout/RootLayoutClient";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { usePathname } from "next/navigation";

export function CMSLayoutClient({ children }: { children: React.ReactNode }) {
  const colors = useColors();
  const pathname = usePathname();

  // 로그인 페이지는 AuthGuard를 적용하지 않음
  const isLoginPage = pathname === "/login";

  console.log("🏗️ [CMSLayoutClient] Rendering:", {
    pathname,
    isLoginPage,
    willApplyAuthGuard: !isLoginPage,
  });

  if (isLoginPage) {
    return (
      <Box
        minH="100vh"
        bg={colors.bg}
        color={colors.text.primary}
        transition="background-color 0.2s"
      >
        <RootLayoutClient>{children}</RootLayoutClient>
      </Box>
    );
  }

  return (
    <AuthGuard
      allowedRoles={["ADMIN", "SUPER_ADMIN", "SERVICE_ADMIN", "SITE_ADMIN"]}
      redirectTo="/login"
    >
      <Box
        minH="100vh"
        bg={colors.bg}
        color={colors.text.primary}
        transition="background-color 0.2s"
      >
        <RootLayoutClient>{children}</RootLayoutClient>
      </Box>
    </AuthGuard>
  );
}
