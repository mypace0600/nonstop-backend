# ERD – Nonstop App

## 1. Enum Definitions

### 1.1 Authentication Provider

```text
auth_provider
- EMAIL
- GOOGLE
```

### 1.2 Friend Status

```text
friend_status
- WAITING
- ACCEPTED
- BLOCKED
- REJECTED
```

### 1.3 Board Type

```text
board_type
- GENERAL
- NOTICE
- QNA
- ANONYMOUS
```

### 1.4 Notification Type

```text
notification_type
- FRIEND_REQUEST
- FRIEND_ACCEPT
- POST_LIKE
- COMMENT_LIKE
- NEW_COMMENT
- NEW_REPLY
- CHAT_MESSAGE
- ANNOUNCEMENT
```

### 1.5 Semester Type

```text
semester_type
- FIRST
- SECOND
- SUMMER
- WINTER
```

---

## 2. User & Authentication Domain

### 2.1 users

| Column         | Type             | Description                                   |
| -------------- | ---------------- | --------------------------------------------- |
| id             | bigint (PK)      | User ID                                       |
| email          | varchar (Unique) | User email                                    |
| password       | varchar          | Encrypted password (nullable for OAuth users) |
| nickname       | varchar          | Display name                                  |
| auth_provider  | enum             | EMAIL / GOOGLE                                |
| student_number | varchar (Unique) | Student number                                |
| university_id  | bigint (FK)      | University                                    |
| major_id       | bigint (FK)      | Major                                         |
| profile_image  | varchar          | Profile image URL                             |
| introduction   | text             | Self introduction                             |
| is_active      | boolean          | Active status                                 |
| is_verified    | boolean          | University email verified                     |
| last_login_at  | timestamp        | Last login time                               |
| created_at     | timestamp        | Created time                                  |
| updated_at     | timestamp        | Updated time                                  |
| deleted_at     | timestamp        | Soft delete                                   |

---

### 2.2 refresh_tokens

| Column     | Type             | Description   |
| ---------- | ---------------- | ------------- |
| id         | bigint (PK)      | Token ID      |
| user_id    | bigint (FK)      | User          |
| token      | varchar (Unique) | Refresh token |
| expires_at | timestamp        | Expiration    |
| revoked_at | timestamp        | Revoked time  |
| created_at | timestamp        | Issued time   |

---

### 2.3 device_tokens

| Column      | Type             | Description      |
| ----------- | ---------------- | ---------------- |
| id          | bigint (PK)      | Device token ID  |
| user_id     | bigint (FK)      | User             |
| token       | varchar (Unique) | FCM device token |
| device_type | varchar          | iOS / Android    |
| is_active   | boolean          | Active status    |
| created_at  | timestamp        | Created time     |
| updated_at  | timestamp        | Updated time     |

---

## 3. University Domain

### 3.1 universities

| Column     | Type             | Description     |
| ---------- | ---------------- | --------------- |
| id         | bigint (PK)      | University ID   |
| name       | varchar (Unique) | University name |
| region     | varchar          | Region          |
| logo_image | varchar          | Logo image      |
| created_at | timestamp        | Created time    |

---

### 3.2 university_email_domains

| Column        | Type        | Description  |
| ------------- | ----------- | ------------ |
| id            | bigint (PK) | ID           |
| university_id | bigint (FK) | University   |
| domain        | varchar     | Email domain |

> Unique constraint: `(university_id, domain)`

---

### 3.3 majors

| Column        | Type        | Description |
| ------------- | ----------- | ----------- |
| id            | bigint (PK) | Major ID    |
| university_id | bigint (FK) | University  |
| name          | varchar     | Major name  |

> Unique constraint: `(university_id, name)`

---

## 4. Community & Board Domain

### 4.1 communities

| Column        | Type        | Description       |
| ------------- | ----------- | ----------------- |
| id            | bigint (PK) | Community ID      |
| university_id | bigint (FK) | University        |
| name          | varchar     | Community name    |
| description   | text        | Description       |
| icon          | varchar     | Icon image        |
| is_anonymous  | boolean     | Anonymous allowed |
| sort_order    | integer     | Sorting order     |
| created_at    | timestamp   | Created time      |

---

### 4.2 boards

| Column       | Type        | Description  |
| ------------ | ----------- | ------------ |
| id           | bigint (PK) | Board ID     |
| community_id | bigint (FK) | Community    |
| name         | varchar     | Board name   |
| type         | enum        | Board type   |
| is_secret    | boolean     | Secret board |
| created_at   | timestamp   | Created time |

---

## 5. Post & Comment Domain

### 5.1 posts

| Column       | Type        | Description    |
| ------------ | ----------- | -------------- |
| id           | bigint (PK) | Post ID        |
| board_id     | bigint (FK) | Board          |
| user_id      | bigint (FK) | Author         |
| title        | varchar     | Title          |
| content      | text        | Content        |
| view_count   | bigint      | View count     |
| is_anonymous | boolean     | Anonymous post |
| is_secret    | boolean     | Secret post    |
| is_deleted   | boolean     | Soft delete    |
| deleted_at   | timestamp   | Deleted time   |
| created_at   | timestamp   | Created time   |
| updated_at   | timestamp   | Updated time   |

---

### 5.2 post_images

| Column     | Type        | Description  |
| ---------- | ----------- | ------------ |
| id         | bigint (PK) | Image ID     |
| post_id    | bigint (FK) | Post         |
| image_url  | varchar     | Image URL    |
| sort_order | integer     | Order        |
| created_at | timestamp   | Created time |

---

### 5.3 comments

| Column           | Type        | Description    |
| ---------------- | ----------- | -------------- |
| id               | bigint (PK) | Comment ID     |
| post_id          | bigint (FK) | Post           |
| user_id          | bigint (FK) | Author         |
| upper_comment_id | bigint (FK) | Parent comment |
| content          | text        | Content        |
| is_anonymous     | boolean     | Anonymous      |
| is_deleted       | boolean     | Soft delete    |
| deleted_at       | timestamp   | Deleted time   |
| created_at       | timestamp   | Created time   |
| updated_at       | timestamp   | Updated time   |

---

### 5.4 comment_images

| Column     | Type        | Description  |
| ---------- | ----------- | ------------ |
| id         | bigint (PK) | Image ID     |
| comment_id | bigint (FK) | Comment      |
| image_url  | varchar     | Image URL    |
| sort_order | integer     | Order        |
| created_at | timestamp   | Created time |

---

### 5.5 Likes

#### user_post_likes

| user_id | post_id | created_at |
| ------- | ------- | ---------- |

#### user_comment_likes

| user_id | comment_id | created_at |
| ------- | ---------- | ---------- |

---

## 6. Friend Domain

### 6.1 friends

| Column      | Type        | Description        |
| ----------- | ----------- | ------------------ |
| id          | bigint (PK) | Friend relation ID |
| sender_id   | bigint (FK) | Request sender     |
| receiver_id | bigint (FK) | Request receiver   |
| status      | enum        | Friend status      |
| created_at  | timestamp   | Created time       |
| updated_at  | timestamp   | Updated time       |

---

## 7. Chat Domain

### 7.1 chat_rooms

| Column     | Type        | Description  |
| ---------- | ----------- | ------------ |
| id         | bigint (PK) | Chat room ID |
| created_at | timestamp   | Created time |
| updated_at | timestamp   | Updated time |

---

### 7.2 chat_room_users

| Column               | Type            | Description       |
| -------------------- | --------------- | ----------------- |
| chat_room_id         | bigint (PK, FK) | Chat room         |
| user_id              | bigint (PK, FK) | User              |
| last_read_message_id | bigint (FK)     | Last read message |
| unread_count         | integer         | Unread messages   |
| joined_at            | timestamp       | Joined time       |

---

### 7.3 messages

| Column       | Type        | Description  |
| ------------ | ----------- | ------------ |
| id           | bigint (PK) | Message ID   |
| chat_room_id | bigint (FK) | Chat room    |
| sender_id    | bigint (FK) | Sender       |
| content      | text        | Message      |
| sent_at      | timestamp   | Sent time    |
| is_deleted   | boolean     | Deleted flag |

---

## 8. Timetable Domain

### 8.1 semesters

| Column        | Type        | Description   |
| ------------- | ----------- | ------------- |
| id            | bigint (PK) | Semester ID   |
| university_id | bigint (FK) | University    |
| year          | integer     | Year          |
| semester      | enum        | Semester type |
| start_date    | date        | Start date    |
| end_date      | date        | End date      |

---

### 8.2 time_tables

| Column      | Type        | Description  |
| ----------- | ----------- | ------------ |
| id          | bigint (PK) | Timetable ID |
| user_id     | bigint (FK) | User         |
| semester_id | bigint (FK) | Semester     |
| title       | varchar     | Title        |
| is_public   | boolean     | Public       |
| created_at  | timestamp   | Created time |

---

### 8.3 time_table_entries

| Column        | Type        | Description |
| ------------- | ----------- | ----------- |
| id            | bigint (PK) | Entry ID    |
| time_table_id | bigint (FK) | Timetable   |
| subject_name  | varchar     | Subject     |
| professor     | varchar     | Professor   |
| day_of_week   | varchar     | Day         |
| start_time    | time        | Start       |
| end_time      | time        | End         |
| place         | varchar     | Location    |
| color         | varchar     | UI Color    |

---

## 9. Notification Domain

### 9.1 notifications

| Column       | Type        | Description       |
| ------------ | ----------- | ----------------- |
| id           | bigint (PK) | Notification ID   |
| user_id      | bigint (FK) | Target user       |
| actor_id     | bigint (FK) | Action user       |
| type         | enum        | Notification type |
| post_id      | bigint (FK) | Related post      |
| comment_id   | bigint (FK) | Related comment   |
| chat_room_id | bigint (FK) | Related chat      |
| message      | text        | Message           |
| is_read      | boolean     | Read flag         |
| created_at   | timestamp   | Created time      |

---

## 10. Relationships Summary

* User → University / Major
* University → Communities → Boards → Posts → Comments
* User ↔ Friend (Self-referencing)
* User ↔ ChatRoom (via chat_room_users)
* User → Timetable → Entries
* User → Notifications
* User → Refresh Tokens / Device Tokens

---