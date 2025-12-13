# Product Requirements Document (PRD): **Nonstop App**

---

## 1. Overview

**Nonstop App** is a university community mobile application designed to help students connect, share information, and manage their academic life efficiently.

The application provides features such as authentication, university-based communities, boards, chat, friend management, timetable management, and real-time notifications.

---

## 2. User Personas

### 2.1 New Student
* Wants to get information about the university
* Wants to find friends and communities
* Needs help with courses, majors, and campus life

### 2.2 Existing Student
* Wants to share knowledge and experience
* Actively participates in boards and discussions
* Manages personal schedule and timetables
* Uses chat and notifications frequently

---

## 3. Functional Requirements

## 3.1 Authentication

### 3.1.1 Authentication Strategy
* The system shall use **JWT (JSON Web Token)** based authentication for all login methods.
* Two types of tokens shall be issued:
  * **Access Token**: Short-lived, sent via `Authorization: Bearer {accessToken}` header for HTTP APIs
  * **Refresh Token**: Long-lived, stored securely on client and server-side for validation/revocation
* All protected APIs shall require a valid Access Token.
* Authentication flow (email/password or Google OAuth) must end with issuing the same format JWT tokens.

### 3.1.2 Email & Password Authentication
* Users shall sign up with email, password (bcrypt), nickname, university (optional).
* Duplication checks for email and nickname.
* Login issues Access + Refresh Token.
* Logout invalidates Refresh Token server-side.

### 3.1.3 Google OAuth 2.0 Authentication
* Preferred mobile flow: Google Sign-In SDK → credential → POST /api/v1/auth/google
* Backend verifies credential, creates/finds user (`authProvider: "google"`), issues same JWT tokens.
* New users created with `universityId: null`, no password.
* Additional info (nickname, university) collected post-login if missing.

### 3.1.4 Token Refresh & Payload
* Access Token contains: user_id, email, nickname, universityId (nullable), authProvider, iat, exp
* Refresh endpoint validates server-side Refresh Token and issues new tokens.
* Password endpoints forbidden for Google users.

### 3.1.5 University Handling
* When universityId is null, restricted features (communities, boards, timetables) return limited/empty data with `universityRequired: true` flag.
* Client shows non-intrusive prompt to select university.

## 3.2 User Management
* View/update profile (including university selection and preferred language via PATCH /users/me)
* Change password (email users only)
* Delete account

## 3.3 University Information
* Select university anytime after login
* View university details and majors list

## 3.4 Community & Boards
* View university communities and boards (limited if universityId null)

## 3.5 Posts & Comments
* CRUD, like, report, pagination, search, nested comments

## 3.7 Friends
* Send/accept/reject/cancel/block requests

## 3.8 Chat
* 1:1 rooms, real-time messaging via WebSocket
* WebSocket auth: `wss://api.nonstop.app/ws/v1/chat?access_token={accessToken}`
* Server closes invalid/expired connections (code 4001)
* Reconnect on token refresh

## 3.9 Timetable
* Per-semester management, entries, public sharing (limited if universityId null)

## 3.10 Notifications
* Push notifications via **Firebase Cloud Messaging (FCM)** for announcements, chat, comments
* Server-triggered only (internal endpoints)
* In-app notification list with read marking

## 3.11 System
* Preferred language set via profile update

---

## 4. API Endpoint Summary

### Authentication
| Method | URI                               | Description                          |
|--------|-----------------------------------|--------------------------------------|
| POST   | /api/v1/auth/signup               | Register new user (email/password)   |
| POST   | /api/v1/auth/google               | Google OAuth login (send credential)|
| GET    | /api/v1/policy/list               | Policy Inquiry                       |
| POST   | /api/v1/policy/consent            | Policy consent                       |
| GET    | /api/v1/auth/email/check          | Email duplicate check                |
| GET    | /api/v1/auth/nickname/check       | Nickname duplicate check             |
| POST   | /api/v1/auth/login                | Email/PW login (Issue token)         |
| POST   | /api/v1/auth/logout               | Invalidate Refresh Token             |
| POST   | /api/v1/auth/refresh              | Refresh Access Token                 |
| POST   | /api/v1/auth/email/send           | Send university email verification code |
| GET    | /api/v1/auth/email/verify         | Handle email verification link click |

### User & Device
| Method | URI                               | Description                                      |
|--------|-----------------------------------|--------------------------------------------------|
| GET    | /api/v1/users/me                  | Get My Info                                      |
| PATCH  | /api/v1/users/me                  | Update My Info (university, language, etc.)      |
| PATCH  | /api/v1/users/me/password         | Change Password                                  |
| DELETE | /api/v1/users/me                  | Delete Account                                   |
| POST   | /api/v1/devices/fcm-token         | Register or update FCM device token (called on app launch/login) |

### University
| Method | URI                               | Description       |
|--------|-----------------------------------|-------------------|
| GET    | /api/v1/universities              | University List   |
| GET    | /api/v1/universities/{id}         | University Detail |
| GET    | /api/v1/universities/{id}/majors  | Major List        |

### Community
| Method | URI                                       | Description   |
|--------|-------------------------------------------|---------------|
| GET    | /api/v1/communities                       | Community List|
| GET    | /api/v1/communities/{communityId}/boards  | Board List    |

### Post
| Method | URI                                           | Description      |
|--------|-----------------------------------------------|------------------|
| GET    | /api/v1/boards/{boardId}/posts                | Post List        |
| GET    | /api/v1/boards/{boardId}/posts/{searchText}   | Post search      |
| POST   | /api/v1/boards/{boardId}/posts                | Create Post      |
| GET    | /api/v1/posts/{postId}                        | Post Detail      |
| PATCH  | /api/v1/posts/{postId}                        | Update Post      |
| DELETE | /api/v1/posts/{postId}                        | Delete Post      |
| POST   | /api/v1/posts/report/{postId}                 | Report the post  |
| POST   | /api/v1/posts/{postId}/like                   | Like Post        |

### Comment
| Method | URI                                  | Description       |
|--------|--------------------------------------|-------------------|
| GET    | /api/v1/posts/{postId}/comments      | Comment List      |
| POST   | /api/v1/posts/{postId}/comments      | Create Comment    |
| PATCH  | /api/v1/comments/{commentId}         | Update Comment    |
| DELETE | /api/v1/comments/{commentId}         | Delete Comment    |
| POST   | /api/v1/comments/like/{commentId}    | Like Comment      |
| POST   | /api/v1/comments/report/{commentId}  | Report Comment    |

### Friend
| Method | URI                                       | Description         |
|--------|-------------------------------------------|---------------------|
| GET    | /api/v1/friends                           | Friend List         |
| POST   | /api/v1/friends/request                   | Request Friend      |
| GET    | /api/v1/friends/requests                  | Request List        |
| POST   | /api/v1/friends/requests/{requestId}/accept | Accept Request    |
| POST   | /api/v1/friends/requests/{requestId}/reject | Reject Request    |
| DELETE | /api/v1/friends/requests/{requestId}       | Cancel Request      |
| POST   | /api/v1/friends/block                     | Block User          |

### Chat
| Method | URI                                     | Description                              |
|--------|-----------------------------------------|------------------------------------------|
| GET    | /api/v1/chat/rooms                      | Chat Room List                           |
| POST   | /api/v1/chat/rooms                      | Create Room                              |
| DELETE | /api/v1/chat/rooms/{roomId}             | Leave / Delete 1:1 chat room             |
| GET    | /api/v1/chat/rooms/{roomId}/messages    | Message History (via REST)               |
| WS     | /ws/v1/chat?access_token={accessToken}  | Real-time messaging and read receipts    |

### Timetable
| Method | URI                                      | Description         |
|--------|------------------------------------------|---------------------|
| GET    | /api/v1/semesters                        | Semester List       |
| GET    | /api/v1/timetables                       | Timetable List      |
| POST   | /api/v1/timetables                       | Create Timetable    |
| GET    | /api/v1/timetables/{id}                  | Timetable Detail    |
| PATCH  | /api/v1/timetables/{id}                  | Update Config       |
| DELETE | /api/v1/timetables/{id}                  | Delete Timetable    |
| POST   | /api/v1/timetables/{id}/entries          | Add Entry           |
| PATCH  | /api/v1/timetables/entries/{entryId}     | Update Entry        |
| DELETE | /api/v1/timetables/entries/{entryId}     | Delete Entry        |
| GET    | /api/v1/timetables/public                | Public Timetables   |

### Notification (Client-facing)
| Method | URI                               | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | /api/v1/notifications             | My notification list               |
| PATCH  | /api/v1/notifications/{id}/read   | Mark notification as read          |
| PATCH  | /api/v1/notifications/read-all    | Mark all notifications as read     |

### Internal Push Triggers (Not exposed to clients)
| Method | URI                     | Description                                      |
|--------|-------------------------|--------------------------------------------------|
| POST   | /api/v1/push/notice     | (Internal/Admin) Send announcement push via FCM  |
| POST   | /api/v1/push/chat       | (Internal) Trigger chat message push             |
| POST   | /api/v1/push/comment    | (Internal) Trigger comment/interaction push      |