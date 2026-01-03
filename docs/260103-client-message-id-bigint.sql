-- client_message_id UUID -> BIGINT 변환
-- 기존 UUID 데이터가 있다면 먼저 백업 필요

-- 1. 기존 인덱스 삭제
DROP INDEX IF EXISTS ux_messages_client_id;

-- 2. 기존 컬럼 삭제 후 새 컬럼 추가
ALTER TABLE messages DROP COLUMN IF EXISTS client_message_id;
ALTER TABLE messages ADD COLUMN client_message_id BIGINT;

-- 3. 새 인덱스 생성
CREATE UNIQUE INDEX ux_messages_client_id
    ON messages(client_message_id) WHERE client_message_id IS NOT NULL;
