# 🟦 Nonstop App Database Schema (PostgreSQL)

**Version:** Golden Master v2.2
**DBMS:** PostgreSQL
**특징:**

* ENUM 적극 활용
* Soft Delete 기반 설계
* 방향 없는 관계는 `LEAST / GREATEST` Unique Index 사용

---

## 0️⃣ ENUM Definitions

```sql
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

CREATE TYPE file_purpose AS ENUM (
  'PROFILE_IMAGE',
  'BOARD_ATTACHMENT',
  'STUDENT_ID_VERIFICATION',
  'UNIVERSITY_LOGO',
  'CHAT_IMAGE'
);

CREATE TYPE message_type AS ENUM (
  'TEXT',
  'IMAGE',
  'SYSTEM_INVITE',
  'SYSTEM_LEAVE',
  'SYSTEM_KICK'
);
```

---

## 1️⃣ Users & Authentication

### users

```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  user_role user_role NOT NULL DEFAULT 'USER',
  email VARCHAR(255),
  password VARCHAR(255),
  auth_provider auth_provider NOT NULL,
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
```

**Indexes**

```sql
CREATE UNIQUE INDEX ux_users_email
  ON users(email) WHERE email IS NOT NULL;

CREATE UNIQUE INDEX ux_users_nickname
  ON users(nickname) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_users_student_number
  ON users(student_number) WHERE student_number IS NOT NULL;

CREATE INDEX ix_users_is_verified ON users(is_verified);
```

---

### refresh_tokens

```sql
CREATE TABLE refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  token VARCHAR(512) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

---

### device_tokens

```sql
CREATE TABLE device_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  device_type VARCHAR(20) NOT NULL,
  token VARCHAR(512) NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);
```

---

### student_verification_requests

```sql
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
```

**Indexes**

```sql
CREATE UNIQUE INDEX ux_student_verification_user
  ON student_verification_requests(user_id);

CREATE INDEX ix_student_verification_status
  ON student_verification_requests(status);

CREATE INDEX ix_student_verification_created_at
  ON student_verification_requests(created_at DESC);
```

---

## 2️⃣ University

```sql
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
```

---

## 3️⃣ Semester

```sql
CREATE TABLE semesters (
  id BIGSERIAL PRIMARY KEY,
  university_id BIGINT NOT NULL REFERENCES universities(id),
  year INTEGER NOT NULL,
  type semester_type NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (university_id, year, type)
);

CREATE INDEX ix_semesters_university
  ON semesters(university_id);
```

---

## 4️⃣ Community & Board

```sql
CREATE TABLE communities (
  id BIGSERIAL PRIMARY KEY,
  university_id BIGINT NOT NULL REFERENCES universities(id),
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
```

---

## 5️⃣ Post & Comment

```sql
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
```

```sql
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
```

---

## 6️⃣ Likes

```sql
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
```

---

## 7️⃣ Chat

```sql
CREATE TABLE chat_rooms (
  id BIGSERIAL PRIMARY KEY,
  type chat_room_type NOT NULL,
  name VARCHAR(255),
  creator_id BIGINT REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE chat_room_members (
  id BIGSERIAL PRIMARY KEY,
  room_id BIGINT NOT NULL REFERENCES chat_rooms(id),
  user_id BIGINT NOT NULL REFERENCES users(id),
  last_read_message_id BIGINT REFERENCES messages(id),
  joined_at TIMESTAMP NOT NULL DEFAULT now(),
  left_at TIMESTAMP,
  UNIQUE (room_id, user_id)
);

CREATE INDEX ix_chat_room_members_user
  ON chat_room_members(user_id) WHERE left_at IS NULL;
```

```sql
CREATE TABLE one_to_one_chat_rooms (
  room_id BIGINT PRIMARY KEY REFERENCES chat_rooms(id),
  user_a_id BIGINT NOT NULL REFERENCES users(id),
  user_b_id BIGINT NOT NULL REFERENCES users(id)
);

CREATE UNIQUE INDEX ux_one_to_one_chat_rooms_pair
ON one_to_one_chat_rooms (
  LEAST(user_a_id, user_b_id),
  GREATEST(user_a_id, user_b_id)
);
```

```sql
CREATE TABLE messages (
  id BIGSERIAL PRIMARY KEY,
  chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id),
  sender_id BIGINT NOT NULL REFERENCES users(id),
  client_message_id UUID,
  type message_type NOT NULL DEFAULT 'TEXT',
  content TEXT,
  sent_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_messages_client_id
  ON messages(client_message_id) WHERE client_message_id IS NOT NULL;

CREATE INDEX ix_messages_room_sent
  ON messages(chat_room_id, sent_at DESC);

CREATE TABLE message_deletions (
  message_id BIGINT REFERENCES messages(id),
  user_id BIGINT REFERENCES users(id),
  deleted_at TIMESTAMP,
  PRIMARY KEY (message_id, user_id)
);
```

---

## 8️⃣ Timetable

```sql
CREATE TABLE time_tables (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  semester_id BIGINT NOT NULL REFERENCES semesters(id),
  title VARCHAR(255),
  is_public BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (user_id, semester_id)
);
```

```sql
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
```

---

## 9️⃣ Friend & Block

```sql
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
ON friends (
  LEAST(sender_id, receiver_id),
  GREATEST(sender_id, receiver_id)
)
WHERE deleted_at IS NULL;
```

```sql
CREATE TABLE user_blocks (
  blocker_id BIGINT REFERENCES users(id),
  blocked_id BIGINT REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  PRIMARY KEY (blocker_id, blocked_id)
);
```

---

## 🔟 Notification & Report

```sql
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
```

```sql
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
```

---

## 11️⃣ Files

```sql
CREATE TABLE files (
  id BIGSERIAL PRIMARY KEY,
  uploader_id BIGINT NOT NULL REFERENCES users(id),
  target_domain VARCHAR(100) NOT NULL, -- 'users', 'posts', 'universities'
  target_id BIGINT NOT NULL,
  purpose file_purpose NOT NULL,
  file_url VARCHAR(512) NOT NULL,
  original_file_name VARCHAR(255),
  content_type VARCHAR(100),
  file_size_bytes BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  deleted_at TIMESTAMP
);

CREATE INDEX ix_files_target ON files(target_domain, target_id);
CREATE INDEX ix_files_uploader ON files(uploader_id);
CREATE INDEX ix_files_purpose ON files(purpose);
```