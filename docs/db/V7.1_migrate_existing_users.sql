-- 기존 사용자 마이그레이션 (기존 회원은 이미 활동 중이므로 인증된 것으로 처리)
UPDATE users
SET email_verified = TRUE,
    email_verified_at = created_at
WHERE email_verified = FALSE;
