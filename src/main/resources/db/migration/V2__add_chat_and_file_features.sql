-- ===================================================================
-- V2: 채팅 기능 강화 및 파일 테이블 추가
-- ===================================================================

-- 1. file_purpose ENUM에 CHAT_IMAGE 추가
ALTER TYPE file_purpose ADD VALUE IF NOT EXISTS 'CHAT_IMAGE';

-- 2. message_type ENUM 신규 생성
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'message_type') THEN
        CREATE TYPE message_type AS ENUM (
            'TEXT',
            'IMAGE',
            'SYSTEM_INVITE',
            'SYSTEM_LEAVE',
            'SYSTEM_KICK'
        );
    END IF;
END$$;

-- 3. chat_room_members 테이블 추가 (그룹 채팅 멤버십 + 읽음 처리)
CREATE TABLE IF NOT EXISTS chat_room_members (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES chat_rooms(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    last_read_message_id BIGINT,
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    left_at TIMESTAMP,
    UNIQUE (room_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_chat_room_members_user
    ON chat_room_members(user_id) WHERE left_at IS NULL;

-- 4. messages 테이블에 컬럼 추가
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS client_message_id UUID,
    ADD COLUMN IF NOT EXISTS type message_type NOT NULL DEFAULT 'TEXT';

CREATE UNIQUE INDEX IF NOT EXISTS ux_messages_client_id
    ON messages(client_message_id) WHERE client_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_messages_room_sent
    ON messages(chat_room_id, sent_at DESC);

-- 5. chat_room_members의 last_read_message_id FK 추가
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_chat_room_members_last_read'
        AND table_name = 'chat_room_members'
    ) THEN
        ALTER TABLE chat_room_members
            ADD CONSTRAINT fk_chat_room_members_last_read
            FOREIGN KEY (last_read_message_id) REFERENCES messages(id);
    END IF;
END$$;

-- 6. files 테이블 추가
CREATE TABLE IF NOT EXISTS files (
    id BIGSERIAL PRIMARY KEY,
    uploader_id BIGINT NOT NULL REFERENCES users(id),
    target_domain VARCHAR(100) NOT NULL,
    target_id BIGINT NOT NULL,
    purpose file_purpose NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    original_file_name VARCHAR(255),
    content_type VARCHAR(100),
    file_size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_files_target ON files(target_domain, target_id);
CREATE INDEX IF NOT EXISTS ix_files_uploader ON files(uploader_id);
CREATE INDEX IF NOT EXISTS ix_files_purpose ON files(purpose);
