"use client";

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useRecoilValue } from "recoil";
import { authState } from "@/stores/auth";

/**
 * 인증 상태에 따른 스마트 리다이렉트 처리
 * 현재 페이지 컨텍스트를 고려하여 적절한 리다이렉트 수행
 */
export const AuthRedirectHandler = () => {
  const { isAuthenticated, user } = useRecoilValue(authState);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    // 로그인 페이지에서 인증된 사용자
    if (pathname === "/login" && isAuthenticated && user) {
      console.log(
        "✅ [AuthRedirectHandler] Authenticated user on login page, redirecting to home"
      );
      router.push("/home");
      return;
    }

    // 루트 페이지나 CMS 루트에서의 리다이렉트
    if ((pathname === "/" || pathname === "/cms") && !isAuthenticated) {
      router.push("/login");
      return;
    }

    if ((pathname === "/" || pathname === "/cms") && isAuthenticated) {
      router.push("/home");
      return;
    }
  }, [isAuthenticated, user, pathname, router]);

  return null;
};
