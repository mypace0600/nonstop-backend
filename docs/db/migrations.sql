-- ===================================================================
-- Nonstop Database Migrations
-- ===================================================================
-- 이 파일은 데이터베이스 마이그레이션 히스토리를 통합한 문서입니다.
-- 실제 마이그레이션은 Flyway 등의 도구를 사용하세요.
--
-- Version History:
--   V1 (2025-12-20): 초기 스키마 생성
--   V2 (2025-12-26): 채팅 기능 강화, 파일 테이블 추가
--   V3 (2026-01-03): client_message_id UUID → BIGINT 변환
-- ===================================================================


-- ###################################################################
-- V1: 초기 스키마 (2025-12-20)
-- ###################################################################

-- ===================================================================
-- ENUM Definitions
-- ===================================================================
CREATE TYPE auth_provider AS ENUM ('EMAIL', 'GOOGLE');
CREATE TYPE friend_status AS ENUM ('WAITING', 'ACCEPTED', 'REJECTED', 'BLOCKED');
CREATE TYPE board_type AS ENUM ('GENERAL', 'NOTICE', 'QNA', 'ANONYMOUS');
CREATE TYPE notification_type AS ENUM (
  'FRIEND_REQUEST',
  'FRIEND_ACCEPT',
  'POST_LIKE',
  'COMMENT_LIKE',
  'NEW_COMMENT',
  'NEW_REPLY',
  'CHAT_MESSAGE',
  'ANNOUNCEMENT'
);
CREATE TYPE semester_type AS ENUM ('FIRST', 'SECOND', 'SUMMER', 'WINTER');
CREATE TYPE report_target_type AS ENUM ('POST', 'COMMENT', 'USER', 'CHAT_MESSAGE');
CREATE TYPE report_reason_type AS ENUM (
  'SPAM', 'ABUSE', 'SEXUAL', 'HATE', 'ILLEGAL', 'PRIVACY', 'IMPERSONATION', 'ETC'
);
CREATE TYPE report_status AS ENUM ('PENDING', 'REVIEWED', 'ACTION_TAKEN', 'REJECTED');
CREATE TYPE chat_room_type AS ENUM ('ONE_TO_ONE', 'GROUP');
CREATE TYPE verification_method AS ENUM ('EMAIL_DOMAIN', 'MANUAL_REVIEW', 'STUDENT_ID_PHOTO');
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN', 'MANAGER');
CREATE TYPE comment_type AS ENUM ('COMMENT', 'REPLY');

-- ===================================================================
-- Users & Authentication
-- ===================================================================
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  user_role user_role NOT NULL DEFAULT 'USER',
  email VARCHAR(255),
  password VARCHAR(255),
  auth_provider auth_provider NOT NULL,
  provider_id VARCHAR(255),
  nickname VARCHAR(30) NOT NULL,
  student_number VARCHAR(50),
  university_id BIGINT,
  major_id BIGINT,
  profile_image_url VARCHAR(512),
  introduction TEXT,
  preferred_language VARCHAR(5),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  is_verified BOOLEAN NOT NULL DEFAULT FALSE,
  verification_method verification_method,
  last_login_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX ux_users_email
  ON users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX ux_users_nickname
  ON users(nickname) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_users_student_number
  ON users(student_number) WHERE student_number IS NOT NULL;
CREATE INDEX ix_users_is_verified ON users(is_verified);

CREATE TABLE refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  token VARCHAR(512) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE device_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  device_type VARCHAR(20) NOT NULL,
  token VARCHAR(512) NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE student_verification_requests (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  image_url VARCHAR(512) NOT NULL,
  status report_status NOT NULL DEFAULT 'PENDING',
  reject_reason VARCHAR(255),
  reviewed_by BIGINT REFERENCES users(id),
  reviewed_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_student_verification_user
  ON student_verification_requests(user_id);
CREATE INDEX ix_student_verification_status
  ON student_verification_requests(status);
CREATE INDEX ix_student_verification_created_at
  ON student_verification_requests(created_at DESC);

-- ===================================================================
-- University
-- ===================================================================
CREATE TABLE universities (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  region VARCHAR(100),
  logo_image_url VARCHAR(512),
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE university_email_domains (
  id BIGSERIAL PRIMARY KEY,
  university_id BIGINT NOT NULL REFERENCES universities(id),
  domain VARCHAR(255) NOT NULL,
  UNIQUE (university_id, domain)
);

CREATE TABLE majors (
  id BIGSERIAL PRIMARY KEY,
  university_id BIGINT NOT NULL REFERENCES universities(id),
  name VARCHAR(255) NOT NULL,
  UNIQUE (university_id, name)
);

-- ===================================================================
-- Semester
-- ===================================================================
CREATE TABLE semesters (
  id BIGSERIAL PRIMARY KEY,
  university_id BIGINT NOT NULL REFERENCES universities(id),
  year INTEGER NOT NULL,
  type semester_type NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (university_id, year, type)
);

CREATE INDEX ix_semesters_university ON semesters(university_id);

-- ===================================================================
-- Community & Board
-- ===================================================================
CREATE TABLE communities (
  id BIGSERIAL PRIMARY KEY,
  university_id BIGINT REFERENCES universities(id),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  icon VARCHAR(255),
  is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE boards (
  id BIGSERIAL PRIMARY KEY,
  community_id BIGINT NOT NULL REFERENCES communities(id),
  name VARCHAR(255) NOT NULL,
  type board_type NOT NULL,
  is_secret BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- ===================================================================
-- Post & Comment
-- ===================================================================
CREATE TABLE posts (
  id BIGSERIAL PRIMARY KEY,
  board_id BIGINT NOT NULL REFERENCES boards(id),
  user_id BIGINT NOT NULL REFERENCES users(id),
  title VARCHAR(150),
  content TEXT,
  view_count BIGINT NOT NULL DEFAULT 0,
  is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
  is_secret BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE comments (
  id BIGSERIAL PRIMARY KEY,
  post_id BIGINT NOT NULL REFERENCES posts(id),
  user_id BIGINT NOT NULL REFERENCES users(id),
  upper_comment_id BIGINT REFERENCES comments(id),
  content TEXT NOT NULL,
  type comment_type NOT NULL,
  is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
  depth INT NOT NULL DEFAULT 0,
  deleted_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- ===================================================================
-- Likes
-- ===================================================================
CREATE TABLE user_post_likes (
  user_id BIGINT REFERENCES users(id),
  post_id BIGINT REFERENCES posts(id),
  deleted_at TIMESTAMP,
  PRIMARY KEY (user_id, post_id)
);

CREATE TABLE user_comment_likes (
  user_id BIGINT REFERENCES users(id),
  comment_id BIGINT REFERENCES comments(id),
  deleted_at TIMESTAMP,
  PRIMARY KEY (user_id, comment_id)
);

-- ===================================================================
-- Chat
-- ===================================================================
CREATE TABLE chat_rooms (
  id BIGSERIAL PRIMARY KEY,
  type chat_room_type NOT NULL,
  name VARCHAR(255),
  creator_id BIGINT REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE one_to_one_chat_rooms (
  room_id BIGINT PRIMARY KEY REFERENCES chat_rooms(id),
  user_a_id BIGINT NOT NULL REFERENCES users(id),
  user_b_id BIGINT NOT NULL REFERENCES users(id)
);

CREATE UNIQUE INDEX ux_one_to_one_chat_rooms_pair
  ON one_to_one_chat_rooms (LEAST(user_a_id, user_b_id), GREATEST(user_a_id, user_b_id));

CREATE TABLE messages (
  id BIGSERIAL PRIMARY KEY,
  chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id),
  sender_id BIGINT NOT NULL REFERENCES users(id),
  content TEXT,
  sent_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE message_deletions (
  message_id BIGINT REFERENCES messages(id),
  user_id BIGINT REFERENCES users(id),
  deleted_at TIMESTAMP,
  PRIMARY KEY (message_id, user_id)
);

-- ===================================================================
-- Timetable
-- ===================================================================
CREATE TABLE time_tables (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  semester_id BIGINT NOT NULL REFERENCES semesters(id),
  title VARCHAR(255),
  is_public BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (user_id, semester_id)
);

CREATE TABLE time_table_entries (
  id BIGSERIAL PRIMARY KEY,
  time_table_id BIGINT NOT NULL REFERENCES time_tables(id),
  subject_name VARCHAR(255),
  professor VARCHAR(255),
  day_of_week VARCHAR(20),
  start_time TIME,
  end_time TIME,
  place VARCHAR(255),
  color VARCHAR(50)
);

-- ===================================================================
-- Friend & Block
-- ===================================================================
CREATE TABLE friends (
  id BIGSERIAL PRIMARY KEY,
  sender_id BIGINT NOT NULL REFERENCES users(id),
  receiver_id BIGINT NOT NULL REFERENCES users(id),
  status friend_status NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX ux_friends_pair_active
  ON friends (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id))
  WHERE deleted_at IS NULL;

CREATE TABLE user_blocks (
  blocker_id BIGINT REFERENCES users(id),
  blocked_id BIGINT REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  PRIMARY KEY (blocker_id, blocked_id)
);

-- ===================================================================
-- Notification & Report
-- ===================================================================
CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  actor_id BIGINT,
  actor_nickname VARCHAR(30),
  type notification_type NOT NULL,
  post_id BIGINT REFERENCES posts(id),
  comment_id BIGINT REFERENCES comments(id),
  chat_room_id BIGINT REFERENCES chat_rooms(id),
  message TEXT,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reports (
  id BIGSERIAL PRIMARY KEY,
  reporter_id BIGINT NOT NULL REFERENCES users(id),
  target_type report_target_type NOT NULL,
  target_id BIGINT NOT NULL,
  reason report_reason_type NOT NULL,
  description TEXT,
  status report_status NOT NULL DEFAULT 'PENDING',
  handled_by BIGINT REFERENCES users(id),
  handled_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);


-- ###################################################################
-- V2: 채팅 기능 강화 및 파일 테이블 추가 (2025-12-26)
-- ###################################################################

-- file_purpose ENUM 생성
CREATE TYPE file_purpose AS ENUM (
  'PROFILE_IMAGE',
  'BOARD_ATTACHMENT',
  'STUDENT_ID_VERIFICATION',
  'UNIVERSITY_LOGO',
  'CHAT_IMAGE'
);

-- message_type ENUM 생성
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

-- chat_room_members 테이블 추가 (그룹 채팅 멤버십 + 읽음 처리)
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

-- messages 테이블에 컬럼 추가
ALTER TABLE messages
  ADD COLUMN IF NOT EXISTS client_message_id UUID,
  ADD COLUMN IF NOT EXISTS type message_type NOT NULL DEFAULT 'TEXT';

CREATE UNIQUE INDEX IF NOT EXISTS ux_messages_client_id
  ON messages(client_message_id) WHERE client_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_messages_room_sent
  ON messages(chat_room_id, sent_at DESC);

-- chat_room_members의 last_read_message_id FK 추가
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

-- files 테이블 추가
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


-- ###################################################################
-- V3: client_message_id UUID → BIGINT 변환 (2026-01-03)
-- ###################################################################
-- 기존 UUID 데이터가 있다면 먼저 백업 필요

-- 기존 인덱스 삭제
DROP INDEX IF EXISTS ux_messages_client_id;

-- 기존 컬럼 삭제 후 새 컬럼 추가
ALTER TABLE messages DROP COLUMN IF EXISTS client_message_id;
ALTER TABLE messages ADD COLUMN client_message_id BIGINT;

-- 새 인덱스 생성
CREATE UNIQUE INDEX ux_messages_client_id
  ON messages(client_message_id) WHERE client_message_id IS NOT NULL;
