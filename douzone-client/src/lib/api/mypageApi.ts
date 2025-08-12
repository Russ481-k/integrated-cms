import { privateApi } from "./client";
import {
  MypageEnrollDto,
  MypagePaymentDto,
  MypageRenewalRequestDto,
  LockerAvailabilityDto,
  MypageRenewalResponseDto,
} from "@/types/api";
import { EnrollmentPayStatus } from "@/types/statusTypes";
import { withAuthRedirect } from "./withAuthRedirect";

// --- Common Query String Parameters (for reference, used in function signatures) ---
// interface PageParams {
//   page?: number; // 1-based
//   size?: number; // rows per page
//   sort?: string; // '+field' ASC / '-field' DESC
// }

// --- 4. Schemas (DTOs) ---

// 4.1 ProfileDto
export interface ProfileDto {
  id: number;
  name: string;
  userId: string;
  phone?: string;
  address?: string;
  email: string;
  carNo?: string;
  tempPwFlag?: boolean;
  gender?: string;
}

// 4.2 PasswordChangeDto
export interface PasswordChangeDto {
  currentPw: string;
  newPw: string;
}

// For POST /password/temp
export interface TemporaryPasswordRequestDto {
  userId: string;
}

// For GET /enroll QS
export interface GetEnrollmentsParams {
  status?: EnrollmentPayStatus | string;
  page?: number;
  size?: number;
  sort?: string;
}

// For PATCH /enroll/{id}/cancel
export interface CancelEnrollmentRequestDto {
  reason: string;
}

// For GET /payment QS
export interface GetPaymentsParams {
  page?: number;
  size?: number;
  sort?: string;
}

interface ApiError extends Error {
  status?: number;
  isNoDataAuthError?: boolean;
}

// --- API Base URL ---
const MYPAGE_API_BASE = "/mypage";

// --- API Object ---
export const mypageApi = {
  // 3.1 회원정보 (Profile)
  getProfile: withAuthRedirect(async (): Promise<ProfileDto | null> => {
    const response = await privateApi.get<ProfileDto>(
      `${MYPAGE_API_BASE}/profile`
    );
    if (!response.data || !response.data.name || !response.data.email) {
      const error = new Error("필수 프로필 정보가 없습니다.") as any;
      error.isNoDataAuthError = true;
      error.status = 401;
      throw error;
    }
    return response.data;
  }),
  updateProfile: withAuthRedirect(
    async (
      data: Partial<ProfileDto>,
      currentPassword?: string
    ): Promise<ProfileDto> => {
      const payload = currentPassword ? { ...data, currentPassword } : data;
      const response = await privateApi.patch<ProfileDto>(
        `${MYPAGE_API_BASE}/profile`,
        payload
      );
      return response.data;
    }
  ),

  // 3.2 비밀번호 (Pass & Temp)
  changePassword: withAuthRedirect(
    async (data: PasswordChangeDto): Promise<void> => {
      await privateApi.patch<void>(`${MYPAGE_API_BASE}/password`, data);
    }
  ),
  requestTemporaryPassword: withAuthRedirect(
    async (data: TemporaryPasswordRequestDto): Promise<void> => {
      await privateApi.post<void>(`${MYPAGE_API_BASE}/password/temp`, data);
    }
  ),
};

// Note: The spec mentions a response wrapper: { status, data, message }.
// The current client.ts setup with privateApi.get, .post, etc., directly returns the `data` part.
// So the Promise<DtoType> is appropriate here. If the wrapper was to be handled client-side for each call,
// the return types would need to be adjusted, e.g., Promise<ApiResponse<DtoType>> where ApiResponse includes status, data, message.
