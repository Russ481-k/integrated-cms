"use client";

import { ReactNode } from "react";
import { useRecoilValue } from "recoil";
import { authState, AppUser, useAuthActions } from "@/stores/auth";

interface PermissionGuardProps {
  children: ReactNode;
  allowedRoles?: Array<AppUser["role"]>;
}

/**
 * 권한 체크만을 담당하는 컴포넌트
 * 인증은 이미 완료된 상태에서 권한만 확인
 */
export const PermissionGuard = ({
  children,
  allowedRoles,
}: PermissionGuardProps) => {
  const { user } = useRecoilValue(authState);
  const { logout } = useAuthActions();

  // 권한 체크가 필요하지 않은 경우
  if (!allowedRoles || allowedRoles.length === 0) {
    return <>{children}</>;
  }

  // 사용자 정보가 없는 경우 (이론적으로 발생하지 않아야 함)
  if (!user) {
    return null;
  }

  // 권한이 없는 경우
  if (!allowedRoles.includes(user.role)) {
    // 권한 없음 처리 - 토스트 없이 조용히 로그아웃
    logout("/login", false);
    return null;
  }

  return <>{children}</>;
};
