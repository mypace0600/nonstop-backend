# 🟦 Nonstop App ERD

**Golden Master v2.1**

---

## 📌 Enum Definitions

```sql
Enum auth_provider        { EMAIL, GOOGLE }
Enum friend_status        { WAITING, ACCEPTED, REJECTED, BLOCKED }
Enum board_type           { GENERAL, NOTICE, QNA, ANONYMOUS }
Enum notification_type    { FRIEND_REQUEST, FRIEND_ACCEPT, POST_LIKE, COMMENT_LIKE, NEW_COMMENT, NEW_REPLY, CHAT_MESSAGE, ANNOUNCEMENT }
Enum semester_type        { FIRST, SECOND, SUMMER, WINTER }
Enum report_target_type   { POST, COMMENT, USER, CHAT_MESSAGE }
Enum report_reason_type   { SPAM, ABUSE, SEXUAL, HATE, ILLEGAL, PRIVACY, IMPERSONATION, ETC }
Enum report_status        { PENDING, REVIEWED, ACTION_TAKEN, REJECTED }
Enum chat_room_type       { ONE_TO_ONE, GROUP }
Enum verification_method  { EMAIL_DOMAIN, MANUAL_REVIEW, STUDENT_ID_PHOTO }
Enum user_role            { USER, ADMIN, MANAGER }
```

---

## 1️⃣ User & Authentication

### users

| 컬럼명                 | 타입                  | 설명                     |
| ------------------- | ------------------- |------------------------|
| id                  | bigint PK           | 사용자 ID                 |
| role                | user_role           | USER / ADMIN / MANAGER |
| email               | varchar             | 이메일 (nullable)         |
| password            | varchar             | 비밀번호                   |
| auth_provider       | auth_provider       | 로그인 방식                 |
| nickname            | varchar(30)         | 닉네임                    |
| student_number      | varchar             | 학번                     |
| university_id       | bigint FK           | 대학(nullable)           |
| major_id            | bigint FK           | 전공(nullable)           |
| profile_image_url   | varchar             | 프로필 이미지                |
| introduction        | text                | 자기소개                   |
| preferred_language  | varchar(5)          | 언어                     |
| is_active           | boolean             | 활성 여부                  |
| is_verified         | boolean             | 대학생 인증 여부              |
| verification_method | verification_method | 인증 방식                  |
| last_login_at       | timestamp           | 마지막 로그인                |
| created_at          | timestamp           | 생성일                    |
| updated_at          | timestamp           | 수정일                    |
| deleted_at          | timestamp           | 탈퇴 (Soft Delete)       |

**Indexes**

* email (unique, not null)
* nickname (unique, deleted_at IS NULL)
* student_number (unique, not null)
* is_verified

---

### refresh_tokens

| 컬럼명        | 타입             | 설명            |
| ---------- | -------------- | ------------- |
| id         | bigint PK      |               |
| user_id    | bigint FK      |               |
| token      | varchar UNIQUE | Refresh Token |
| expires_at | timestamp      | 만료 시각         |
| revoked_at | timestamp      | 무효화 시각        |
| created_at | timestamp      | 생성일           |

---

### device_tokens

| 컬럼명         | 타입             | 설명            |
| ----------- | -------------- | ------------- |
| id          | bigint PK      |               |
| user_id     | bigint FK      |               |
| device_type | varchar(20)    | iOS / Android |
| token       | varchar UNIQUE | FCM 토큰        |
| is_active   | boolean        | 활성 여부         |
| created_at  | timestamp      |               |
| updated_at  | timestamp      |               |

---

### student_verification_requests

| 컬럼명           | 타입            | 설명      |
| ------------- | ------------- | ------- |
| id            | bigint PK     |         |
| user_id       | bigint FK     |         |
| image_url     | varchar       | 학생증 이미지 |
| status        | report_status | 인증 상태   |
| reject_reason | varchar       | 반려 사유   |
| reviewed_by   | bigint FK     | 관리자     |
| reviewed_at   | timestamp     | 처리 시각   |
| created_at    | timestamp     |         |
| updated_at    | timestamp     |         |

**Indexes**

* user_id (unique)
* status
* created_at DESC

---

## 2️⃣ University

### universities

| 컬럼명            | 타입             | 설명  |
| -------------- | -------------- | --- |
| id             | bigint PK      |     |
| name           | varchar UNIQUE | 대학명 |
| region         | varchar        | 지역  |
| logo_image_url | varchar        | 로고  |
| created_at     | timestamp      |     |

---

### university_email_domains

| 컬럼명           | 타입        |
| ------------- | --------- |
| id            | bigint PK |
| university_id | bigint FK |
| domain        | varchar   |

**Unique**: (university_id, domain)

---

### majors

| 컬럼명           | 타입        |
| ------------- | --------- |
| id            | bigint PK |
| university_id | bigint FK |
| name          | varchar   |

**Unique**: (university_id, name)

---

## 3️⃣ Community & Board

### communities

| 컬럼명           | 타입        |
| ------------- | --------- |
| id            | bigint PK |
| university_id | bigint FK |
| name          | varchar   |
| description   | text      |
| icon          | varchar   |
| is_anonymous  | boolean   |
| sort_order    | int       |
| created_at    | timestamp |

---

### boards

| 컬럼명          | 타입         |
| ------------ | ---------- |
| id           | bigint PK  |
| community_id | bigint FK  |
| name         | varchar    |
| type         | board_type |
| is_secret    | boolean    |
| created_at   | timestamp  |

---

## 4️⃣ Post & Comment

### posts

| 컬럼명          | 타입           |
| ------------ | ------------ |
| id           | bigint PK    |
| board_id     | bigint FK    |
| user_id      | bigint FK    |
| title        | varchar(150) |
| content      | text         |
| view_count   | bigint       |
| is_anonymous | boolean      |
| is_secret    | boolean      |
| deleted_at   | timestamp    |
| created_at   | timestamp    |
| updated_at   | timestamp    |

---

### comments

| 컬럼명              | 타입        |
| ---------------- | --------- |
| id               | bigint PK |
| post_id          | bigint FK |
| user_id          | bigint FK |
| upper_comment_id | bigint FK |
| content          | text      |
| type             | varchar   |
| is_anonymous     | boolean   |
| depth            | int       |
| deleted_at       | timestamp |
| created_at       | timestamp |
| updated_at       | timestamp |

---

### Likes

* **user_post_likes** (PK: user_id + post_id, soft delete)
* **user_comment_likes** (PK: user_id + comment_id, soft delete)

---

## 5️⃣ Chat

### chat_rooms

| 컬럼명        | 타입             |
| ---------- | -------------- |
| id         | bigint PK      |
| type       | chat_room_type |
| name       | varchar        |
| creator_id | bigint FK      |
| created_at | timestamp      |
| updated_at | timestamp      |

---

### one_to_one_chat_rooms

| 컬럼명       | 타입        |
| --------- | --------- |
| room_id   | bigint PK |
| user_a_id | bigint FK |
| user_b_id | bigint FK |

**Unique**

```
least(user_a_id, user_b_id),
greatest(user_a_id, user_b_id)
```

---

### messages

| 컬럼명          | 타입        |
| ------------ | --------- |
| id           | bigint PK |
| chat_room_id | bigint FK |
| sender_id    | bigint FK |
| content      | text      |
| sent_at      | timestamp |

---

### message_deletions

| 컬럼명        | 타입        |
| ---------- | --------- |
| message_id | bigint FK |
| user_id    | bigint FK |
| deleted_at | timestamp |

---

## 6️⃣ Timetable

### time_tables

| 컬럼명         | 타입        |
| ----------- | --------- |
| id          | bigint PK |
| user_id     | bigint FK |
| semester_id | bigint FK |
| title       | varchar   |
| is_public   | boolean   |
| created_at  | timestamp |

**Unique**: (user_id, semester_id)

---

### time_table_entries

| 컬럼명           | 타입        |
| ------------- | --------- |
| id            | bigint PK |
| time_table_id | bigint FK |
| subject_name  | varchar   |
| professor     | varchar   |
| day_of_week   | varchar   |
| start_time    | time      |
| end_time      | time      |
| place         | varchar   |
| color         | varchar   |

---

## 7️⃣ Friend & Block

### friends

| 컬럼명         | 타입            |
| ----------- | ------------- |
| id          | bigint PK     |
| sender_id   | bigint FK     |
| receiver_id | bigint FK     |
| status      | friend_status |
| created_at  | timestamp     |
| updated_at  | timestamp     |
| deleted_at  | timestamp     |

**Unique**

```
least(sender_id, receiver_id),
greatest(sender_id, receiver_id),
deleted_at
```

---

### user_blocks

| 컬럼명        | 타입        |
| ---------- | --------- |
| blocker_id | bigint FK |
| blocked_id | bigint FK |
| created_at | timestamp |

---

## 8️⃣ Notification & Report

### notifications

| 컬럼명            | 타입                |
| -------------- | ----------------- |
| id             | bigint PK         |
| user_id        | bigint FK         |
| actor_id       | bigint            |
| actor_nickname | varchar           |
| type           | notification_type |
| post_id        | bigint FK         |
| comment_id     | bigint FK         |
| chat_room_id   | bigint FK         |
| message        | text              |
| is_read        | boolean           |
| created_at     | timestamp         |

---

### reports

| 컬럼명         | 타입                 |
| ----------- | ------------------ |
| id          | bigint PK          |
| reporter_id | bigint FK          |
| target_type | report_target_type |
| target_id   | bigint             |
| reason      | report_reason_type |
| description | text               |
| status      | report_status      |
| handled_by  | bigint FK          |
| handled_at  | timestamp          |
| created_at  | timestamp          |
| updated_at  | timestamp          |

---