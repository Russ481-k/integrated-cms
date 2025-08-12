
"use client"; // 클라이언트 컴포넌트로 전환

import React, { useState, useEffect, useMemo } from "react";
import {
  Box,
  Tabs,
  Container,
  Heading,
  Input,
  Button,
  Stack,
  VStack,
  HStack,
  Text,
  Fieldset,
  Field,
} from "@chakra-ui/react";
import { useRouter, useSearchParams } from "next/navigation";
import { mypageApi, ProfileDto } from "@/lib/api/mypageApi";
import { toaster } from "@/components/ui/toaster";
import {
  PasswordInput,
  PasswordStrengthMeter,
} from "@/components/ui/password-input";
import { Tooltip } from "@/components/ui/tooltip";
import { CheckCircle2Icon, XCircleIcon } from "lucide-react";

const initialPasswordCriteria = {
  minLength: false,
  lowercase: false,
  number: false,
  allowedSpecialChar: false,
  noOtherSpecialChars: false,
};

const PasswordTooltipChecklistItem = ({
  label,
  isMet,
}: {
  label: string;
  isMet: boolean;
}) => (
  <HStack gap={2}>
    <Box color={isMet ? "green.400" : "red.400"}>
      {isMet ? <CheckCircle2Icon size={14} /> : <XCircleIcon size={14} />}
    </Box>
    <Text fontSize="xs" color={isMet ? "green.400" : "red.400"}>
      {label}
    </Text>
  </HStack>
);

const getApiErrorMessage = (error: any, defaultMessage: string): string => {
  if (error && error.response && error.response.data) {
    const data = error.response.data;
    if (data.validationErrors) {
      if (
        Array.isArray(data.validationErrors) &&
        data.validationErrors.length > 0
      ) {
        return data.validationErrors.join("\\n");
      } else if (
        typeof data.validationErrors === "object" &&
        Object.keys(data.validationErrors).length > 0
      ) {
        return Object.values(data.validationErrors).join("\\n");
      }
    }
    if (data.message && typeof data.message === "string") {
      return data.message;
    }
  }
  if (error && error.message && typeof error.message === "string") {
    return error.message;
  }
  return defaultMessage;
};

export default function MyPage() {
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [newPw, setNewPw] = useState("");
  const [newPwConfirm, setNewPwConfirm] = useState("");
  const [currentPw, setCurrentPw] = useState("");
  const [profilePw, setProfilePw] = useState("");
  const router = useRouter();
  const searchParams = useSearchParams();

  const [passwordCriteriaMet, setPasswordCriteriaMet] = useState(
    initialPasswordCriteria
  );
  const [newPasswordStrength, setNewPasswordStrength] = useState(0);
  const [isPasswordTooltipVisible, setIsPasswordTooltipVisible] =
    useState(false);
  const [passwordsMatch, setPasswordsMatch] = useState(true);

  // Tab management with URL sync
  const initialTabFromQuery = searchParams.get("tab");
  const [activeTab, setActiveTab] = useState(() => {
    if (initialTabFromQuery === "회원정보_수정") {
      return "회원정보_수정";
    }
    return "회원정보_수정"; // Default tab
  });

  const [dataLoaded, setDataLoaded] = useState({
    profile: false,
  });

  const handleTabChange = (details: { value: string }) => {
    const newTab = details.value;
    setActiveTab(newTab);

    const newUrl = new URL(window.location.href);
    if (newTab === "회원정보_수정") {
      newUrl.searchParams.delete("tab");
    } else {
      newUrl.searchParams.set("tab", newTab);
    }

    window.history.replaceState({}, "", newUrl.toString());
  };

  useEffect(() => {
    let localUserData: any = null;

    async function fetchUserData() {
      try {
        setIsLoading(true);

        if (typeof window !== "undefined") {
          const authUserStr = localStorage.getItem("auth_user");
          if (authUserStr) {
            try {
              localUserData = JSON.parse(authUserStr);

              setProfile((prevProfile) => {
                if (prevProfile) {
                  return {
                    ...prevProfile,
                    userId: localUserData.username || prevProfile.userId,
                    name: localUserData.name || prevProfile.name,
                    email: localUserData.email || prevProfile.email,
                    phone: localUserData.phone || prevProfile.phone || "",
                    address: localUserData.address || prevProfile.address || "",
                    carNo: localUserData.carNo || prevProfile.carNo || "",
                    gender: prevProfile.gender,
                  };
                } else {
                  return {
                    id: 0, // Placeholder, ensure ProfileDto allows this or handle null better
                    userId: localUserData.username || "",
                    name: localUserData.name || "",
                    email: localUserData.email || "",
                    phone: localUserData.phone || "",
                    address: localUserData.address || "",
                    carNo: localUserData.carNo || "",
                  } as ProfileDto;
                }
              });
            } catch (e) {
              console.error("Error parsing auth_user from localStorage:", e);
            }
          }
        }

        const profileData = await mypageApi.getProfile();

        if (
          profileData &&
          typeof profileData === "object" &&
          profileData.userId
        ) {
          setProfile((prevProfile) => ({
            ...(prevProfile || {}),
            id: profileData.id,
            userId: profileData.userId,
            name: profileData.name,
            phone:
              profileData.phone !== undefined
                ? profileData.phone
                : prevProfile?.phone ?? "",
            address:
              profileData.address !== undefined
                ? profileData.address
                : prevProfile?.address ?? "",
            email:
              profileData.email !== undefined
                ? profileData.email
                : prevProfile?.email ?? "",
            carNo:
              profileData.carNo !== undefined
                ? profileData.carNo
                : prevProfile?.carNo ?? "",
            gender:
              profileData.gender !== undefined
                ? profileData.gender
                : prevProfile?.gender,
          }));
        } else {
          if (localUserData && (!profile || !profile.userId)) {
            setProfile((prevProfile) => {
              if (prevProfile) {
                return {
                  ...prevProfile,
                  userId: localUserData.username || prevProfile.userId,
                  name: localUserData.name || prevProfile.name,
                  email: localUserData.email || prevProfile.email,
                  phone: localUserData.phone || prevProfile.phone || "",
                  address: localUserData.address || prevProfile.address || "",
                  carNo: localUserData.carNo || prevProfile.carNo || "",
                  gender: prevProfile.gender,
                };
              } else {
                return {
                  id: 0, // Placeholder
                  userId: localUserData.username || "",
                  name: localUserData.name || "",
                  email: localUserData.email || "",
                  phone: localUserData.phone || "",
                  address: localUserData.address || "",
                  carNo: localUserData.carNo || "",
                } as ProfileDto;
              }
            });
          }
        }

        setDataLoaded((prev) => ({ ...prev, profile: true }));
      } catch (error) {
        console.error(
          "[Mypage] Failed to load user data (in catch block):",
          error
        );

        toaster.create({
          title: "데이터 로딩 중 오류 발생",
          description: getApiErrorMessage(
            error,
            "마이페이지 정보 중 일부를 불러오는데 실패했습니다. 문제가 지속되면 문의해주세요."
          ),
          type: "error",
        });
      } finally {
        setIsLoading(false);
      }
    }

    fetchUserData();
  }, []);

  useEffect(() => {
    const tabFromQuery = searchParams.get("tab");
    if (tabFromQuery === "회원정보_수정" && activeTab !== "회원정보_수정") {
      setActiveTab("회원정보_수정");
    } else if (
      tabFromQuery === "비밀번호_변경" &&
      activeTab !== "비밀번호_변경"
    ) {
      setActiveTab("비밀번호_변경");
    }
  }, [searchParams, activeTab]);

  const validateNewPasswordCriteria = (password: string) => {
    const criteria = {
      minLength: password.length >= 8,
      lowercase: /[a-z]/.test(password),
      number: /[0-9]/.test(password),
      allowedSpecialChar: /[!@#$%^&*()]/.test(password),
      noOtherSpecialChars: /^[a-zA-Z0-9!@#$%^&*()]*$/.test(password),
    };
    setPasswordCriteriaMet(criteria);

    const calculatedStrengthScore = [
      criteria.minLength,
      criteria.lowercase,
      criteria.number,
      criteria.allowedSpecialChar,
    ].filter(Boolean).length;
    setNewPasswordStrength(calculatedStrengthScore);

    return Object.values(criteria).every(Boolean);
  };

  const handleNewPasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newPasswordValue = e.target.value;
    setNewPw(newPasswordValue);
    validateNewPasswordCriteria(newPasswordValue);
    if (newPwConfirm) {
      setPasswordsMatch(newPasswordValue === newPwConfirm);
    }
  };

  const handleNewPasswordConfirmChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const newConfirmPasswordValue = e.target.value;
    setNewPwConfirm(newConfirmPasswordValue);
    setPasswordsMatch(newPw === newConfirmPasswordValue);
  };

  const handleProfileUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;

    if (!profilePw.trim()) {
      toaster.create({
        title: "비밀번호 필요",
        description: "회원정보 변경을 위해 현재 비밀번호를 입력해주세요.",
        type: "error",
      });
      return;
    }

    try {
      await mypageApi.updateProfile(profile, profilePw);

      setProfilePw("");

      toaster.create({
        title: "정보 변경 완료",
        description: "회원정보가 성공적으로 변경되었습니다.",
        type: "success",
      });
    } catch (error) {
      console.error("Failed to update profile:", error);
      toaster.create({
        title: "정보 변경 실패",
        description: getApiErrorMessage(
          error,
          "회원정보 변경에 실패했습니다. 입력 내용을 확인하거나 비밀번호를 확인해주세요."
        ),
        type: "error",
      });
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!currentPw.trim()) {
      toaster.create({
        title: "현재 비밀번호 필요",
        description: "현재 비밀번호를 입력해주세요.",
        type: "error",
      });
      return;
    }

    if (newPw !== newPwConfirm) {
      toaster.create({
        title: "비밀번호 불일치",
        description: "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.",
        type: "error",
      });
      return;
    }

    const isNewPasswordValid = validateNewPasswordCriteria(newPw);
    if (!isNewPasswordValid) {
      toaster.create({
        title: "유효하지 않은 새 비밀번호",
        description:
          "새 비밀번호가 모든 조건을 충족하지 않습니다. 다시 확인해주세요.",
        type: "error",
      });
      setIsPasswordTooltipVisible(true);
      return;
    }

    try {
      await mypageApi.changePassword({
        currentPw,
        newPw,
      });

      setCurrentPw("");
      setNewPw("");
      setNewPwConfirm("");

      toaster.create({
        title: "비밀번호 변경 완료",
        description: "비밀번호가 성공적으로 변경되었습니다.",
        type: "success",
      });
    } catch (error) {
      console.error("Failed to change password:", error);
      toaster.create({
        title: "비밀번호 변경 실패",
        description: getApiErrorMessage(
          error,
          "비밀번호 변경 중 오류가 발생했습니다. 현재 비밀번호를 확인하거나 입력값을 확인해주세요."
        ),
        type: "error",
      });
    }
  };

  const passwordTooltipContent = useMemo(
    () => (
      <VStack align="start" gap={0.5}>
        <PasswordTooltipChecklistItem
          label="8자 이상"
          isMet={passwordCriteriaMet.minLength}
        />
        <PasswordTooltipChecklistItem
          label="영문 소문자 포함"
          isMet={passwordCriteriaMet.lowercase}
        />
        <PasswordTooltipChecklistItem
          label="숫자 포함"
          isMet={passwordCriteriaMet.number}
        />
        <PasswordTooltipChecklistItem
          label="특수문자 (!@#$%^&*()) 포함"
          isMet={passwordCriteriaMet.allowedSpecialChar}
        />
        <PasswordTooltipChecklistItem
          label="다른 종류의 특수문자 사용 불가"
          isMet={passwordCriteriaMet.noOtherSpecialChars}
        />
      </VStack>
    ),
    [passwordCriteriaMet]
  );

  return (
    <Container maxW="1600px" py={8}>
      <Heading as="h1" mb={8} fontSize="3xl">
        마이페이지
      </Heading>

      <Tabs.Root
        value={activeTab}
        onValueChange={handleTabChange}
        variant="line"
        colorPalette="blue"
      >
        <Tabs.List mb={6}>
          <Tabs.Trigger value="회원정보_수정">회원정보 수정</Tabs.Trigger>
          <Tabs.Trigger value="비밀번호_변경">비밀번호 변경</Tabs.Trigger>
        </Tabs.List>

        <Tabs.Content value="회원정보_수정">
          {isLoading ? (
            <Box textAlign="center" p={8}>
              <Text>로딩 중...</Text>
            </Box>
          ) : profile ? (
            <Box
              as="form"
              onSubmit={handleProfileUpdate}
              py={4}
              maxW="672px"
              mx="auto"
            >
              <Fieldset.Root>
                <Fieldset.Content>
                  <Field.Root>
                    <Field.Label>이름</Field.Label>
                    <Input
                      value={profile.name || ""}
                      readOnly
                      bg="gray.100"
                      placeholder="이름"
                    />
                  </Field.Root>

                  <Field.Root>
                    <Field.Label>아이디</Field.Label>
                    <Input
                      value={profile.userId || ""}
                      readOnly
                      bg="gray.100"
                    />
                  </Field.Root>

                  <Field.Root>
                    <Field.Label>핸드폰 번호</Field.Label>
                    <Input
                      value={profile.phone || ""}
                      onChange={(e) =>
                        setProfile({ ...profile, phone: e.target.value })
                      }
                      placeholder="핸드폰 번호를 입력해주세요 (ex: 123-1234-1234)"
                    />
                  </Field.Root>

                  <Field.Root>
                    <Field.Label>주소</Field.Label>
                    <Input
                      value={profile.address || ""}
                      onChange={(e) =>
                        setProfile({ ...profile, address: e.target.value })
                      }
                      placeholder="주소를 입력해주세요"
                    />
                  </Field.Root>

                  <Field.Root>
                    <Field.Label>이메일</Field.Label>
                    <Input
                      value={profile.email || ""}
                      onChange={(e) =>
                        setProfile({ ...profile, email: e.target.value })
                      }
                      placeholder="이메일을 입력해주세요"
                    />
                  </Field.Root>

                  <Field.Root>
                    <Field.Label>차량번호</Field.Label>
                    <Input
                      value={profile.carNo || ""}
                      onChange={(e) =>
                        setProfile({ ...profile, carNo: e.target.value })
                      }
                      placeholder="차량번호를 입력해주세요"
                    />
                  </Field.Root>

                  <Field.Root mt={4}>
                    <Field.Label>본인 확인을 위한 비밀번호</Field.Label>
                    <Input
                      type="password"
                      value={profilePw}
                      onChange={(e) => setProfilePw(e.target.value)}
                      placeholder="회원정보 변경을 위해 현재 비밀번호를 입력해주세요"
                      required
                    />
                  </Field.Root>
                </Fieldset.Content>

                <Box textAlign="center" mt={4}>
                  <Button type="submit" colorPalette="orange" size="md" px={8}>
                    정보변경
                  </Button>
                </Box>
              </Fieldset.Root>
            </Box>
          ) : (
            <Box textAlign="center" p={8}>
              <Text>사용자 정보를 불러올 수 없습니다.</Text>
            </Box>
          )}
        </Tabs.Content>

        <Tabs.Content value="비밀번호_변경">
          <Box
            as="form"
            onSubmit={handlePasswordChange}
            py={4}
            maxW="672px"
            mx="auto"
          >
            <Fieldset.Root>
              <Fieldset.Content>
                <Box p={4} bg="gray.50" borderRadius="md" mb={4}>
                  <Text fontSize="sm">
                    <strong>현재 비밀번호를 입력하세요.</strong>
                    <br />
                    안전을 위해 새 비밀번호를 설정해주세요.
                    <br />
                    비밀번호는 최소 6자 이상이며, 영문+숫자+특수문자를 포함해야
                    합니다.
                  </Text>
                </Box>

                <Field.Root>
                  <Field.Label>아이디</Field.Label>
                  <Input value={profile?.userId || ""} readOnly bg="gray.100" />
                </Field.Root>

                <Field.Root>
                  <Field.Label>현재 비밀번호</Field.Label>
                  <PasswordInput
                    value={currentPw}
                    onChange={(e) => setCurrentPw(e.target.value)}
                    placeholder="현재 비밀번호를 입력해주세요"
                  />
                </Field.Root>

                <Field.Root>
                  <Field.Label>새 비밀번호</Field.Label>
                  <Stack w="full">
                    <Tooltip
                      content={passwordTooltipContent}
                      open={isPasswordTooltipVisible}
                      positioning={{ placement: "bottom-start" }}
                      contentProps={{
                        bg: "white",
                        color: "gray.800",
                        _dark: {
                          bg: "gray.700",
                          color: "whiteAlpha.900",
                        },
                        mt: 2,
                        p: 3,
                        fontSize: "sm",
                        borderRadius: "md",
                        boxShadow: "md",
                        zIndex: "tooltip",
                      }}
                    >
                      <PasswordInput
                        value={newPw}
                        onChange={handleNewPasswordChange}
                        onFocus={() => setIsPasswordTooltipVisible(true)}
                        onBlur={() => setIsPasswordTooltipVisible(false)}
                        placeholder="새로운 비밀번호를 입력해주세요"
                      />
                    </Tooltip>
                    {newPw.length > 0 && (
                      <PasswordStrengthMeter
                        value={newPasswordStrength}
                        max={4}
                      />
                    )}
                  </Stack>
                </Field.Root>

                <Field.Root
                  invalid={!passwordsMatch && newPwConfirm.length > 0}
                >
                  <Field.Label>새 비밀번호 확인</Field.Label>
                  <PasswordInput
                    value={newPwConfirm}
                    onChange={handleNewPasswordConfirmChange}
                    placeholder="새로운 비밀번호를 한번 더 입력해주세요"
                  />
                  {!passwordsMatch && newPwConfirm.length > 0 && (
                    <Field.ErrorText>
                      비밀번호가 일치하지 않습니다.
                    </Field.ErrorText>
                  )}
                </Field.Root>
              </Fieldset.Content>

              <Box textAlign="center" mt={4}>
                <Button type="submit" colorPalette="orange" size="md" px={8}>
                  정보변경
                </Button>
              </Box>
            </Fieldset.Root>
          </Box>
        </Tabs.Content>
      </Tabs.Root>
    </Container>
  );
}
