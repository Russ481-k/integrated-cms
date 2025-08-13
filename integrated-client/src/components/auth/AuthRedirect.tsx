"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useRecoilValue } from "recoil";
import { authState } from "@/stores/auth";

export const AuthRedirect = () => {
  const { isAuthenticated } = useRecoilValue(authState);
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated) {
      router.push("/home");
    } else {
      router.push("/login");
    }
  }, [isAuthenticated, router]);

  // 리다이렉트되는 동안 아무것도 렌더링하지 않음
  return null;
};
