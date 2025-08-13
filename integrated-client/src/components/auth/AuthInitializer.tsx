"use client";

import { useEffect, useRef } from "react";
import { useAuthActions } from "@/stores/auth";

export const AuthInitializer = () => {
  const { syncAuthState } = useAuthActions();
  const hasInitialized = useRef(false);

  useEffect(() => {
    // 앱 시작 시 한 번만 인증 상태 동기화 (모든 페이지에서)
    if (!hasInitialized.current) {
      hasInitialized.current = true;
      syncAuthState();
    }
  }, []); // 빈 배열로 한 번만 실행

  return null;
};
