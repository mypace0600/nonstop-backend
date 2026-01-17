# 🤖 AGENTS.md

Welcome, Agent. This repository is the backend for **Nonstop**, a university-student community platform. Follow these guidelines strictly to maintain consistency.

---

## 🛠 Build & Development Commands

This project uses **Gradle** as the build tool.

| Action | Command |
| :--- | :--- |
| **Build Project** | `./gradlew build` |
| **Run Locally** | `./gradlew bootRun` |
| **Run All Tests** | `./gradlew test` |
| **Run Single Test Class** | `./gradlew test --tests "com.app.nonstop.path.to.TestClass"` |
| **Run Single Test Method** | `./gradlew test --tests "com.app.nonstop.path.to.TestClass.testMethodName"` |
| **Clean Build** | `./gradlew clean` |
| **Generate API Docs** | Access `http://localhost:28080/swagger-ui.html` while running |

---

## 🏗 System Architecture

- **Language:** Java 17
- **Framework:** Spring Boot 3.4.12
- **Persistence:** **MyBatis 3.0.5** (⚠️ DO NOT use Spring Data JPA)
- **Database:** PostgreSQL (with ENUM types and Soft Delete)
- **Messaging:** Apache Kafka (Azure Event Hubs) & WebSocket (STOMP)
- **Security:** Spring Security, JWT, Google OAuth2
- **Infrastructure:** Azure Blob Storage (Images), Redis (Session/Rate Limit), FCM (Push)

---

## 📏 Code Style & Conventions

### 1. Project Structure
Organized by **domain**. Each domain folder should contain:
- `controller/`: REST controllers
- `service/`: Business logic interfaces and implementations
- `dto/`: Request/Response objects
- `entity/`: Database-mapped models

### 2. Naming Conventions
- **Classes:** `PascalCase` (e.g., `PostController`, `AuthService`)
- **Methods/Variables:** `camelCase` (e.g., `createPost`, `userId`)
- **Mappers:** `XxxMapper` (Interface for MyBatis)
- **DTOs:** `XxxRequestDto` for input, `XxxResponseDto` for output
- **Endpoints:** Kebab-case and versioned: `/api/v1/auth/login`

### 3. MyBatis & Persistence
- SQL queries are located in `src/main/resources/mybatis/mappers/`.
- Use snake_case in SQL and map-underscore-to-camel-case in MyBatis configuration.
- Prefer explicit Mapper interfaces over raw `SqlSession`.

### 4. Lombok Usage
- Use `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` liberally.
- Use `@Slf4j` for logging. Avoid `System.out.println`.
- Use `@RequiredArgsConstructor` for constructor injection.

### 5. Error Handling
- Use the global exception handler: `com.app.nonstop.global.common.exception.GlobalExceptionHandler`.
- Custom exceptions should extend `BusinessException` or `RuntimeException`.
- Use `ApiResponse<T>` wrapper for all REST responses.

### 6. Validation
- Use Bean Validation annotations (`@Valid`, `@NotBlank`, `@Size`, etc.) in Controller DTOs.
- Handle validation errors via `GlobalExceptionHandler`.

---

## 🧪 Testing Guidelines

- **Framework:** JUnit 5 (JUnit Jupiter)
- **Mocking:** Mockito
- **Database Testing:** Use `@MybatisTest` for mapper testing or `@SpringBootTest` for integration tests.
- **Naming:** Test methods should clearly describe the behavior (e.g., `login_Success_ReturnsToken`).

---

## 🔒 Security Guidelines

- **JWT:** Tokens are handled via `JwtTokenProvider`.
- **WebSocket:** Handled via STOMP with `WebSocketAuthInterceptor` for security.
- **Privacy:** Never log sensitive user data (passwords, tokens).
- **Graceful Degradation:** Allow non-verified users to access basic features (checked via `universityId == null`).

---

## 📡 Messaging & Real-time

- **Kafka:** Use `roomId` as the message key to ensure ordered delivery.
- **WebSocket:** Endpoint is `/ws/v1/chat`. Use STOMP headers for authentication.

---

## 📝 General Rules for Agents

1. **Check Existing Patterns:** Before adding a new domain, check `domain/chat` or `domain/community` for reference.
2. **No Blind Refactors:** When fixing bugs, do not refactor surrounding code unless it directly relates to the bug.
3. **Documentation:** Keep Swagger annotations updated in controllers.
4. **Environment:** Use `.env` file for local secrets. Never hardcode keys.
5. **Logging:** Always use `log.info` or `log.debug`. Use the `GlobalRequestLoggingFilter` to trace API flow.

---
*Created on 2026-01-15 | Version 1.0*

## Landing the Plane (Session Completion)

**When ending a work session**, follow these steps.

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Git Safety Protocol (Strict)**
   - **NEVER** automatically commit or push changes.
   - **ALWAYS** ask for explicit user permission before running `git commit` or `git push`.
   - **EXCEPTION**: Only commit if the user explicitly uses the `/commit` command or says "commit this".
5. **Clean up** - Clear stashes
6. **Hand off** - Provide context for next session

