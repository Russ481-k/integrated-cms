package api.v2.common.auth.service;

public interface VerificationCodeService {
    String generateAndStoreCode(String email);

    boolean verifyCode(String email, String code);
}