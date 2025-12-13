```
Application

com.app.nonstop
 ├── NonstopApplication.java
 │
 ├── global
 │   ├── config
 │   │   ├── SecurityConfig.java
 │   │   ├── WebSocketConfig.java
 │   │   ├── RedisConfig.java
 │   │   ├── SwaggerConfig.java
 │   │   └── MyBatisConfig.java
 │   │
 │   ├── security
 │   │   ├── jwt
 │   │   │   ├── JwtTokenProvider.java
 │   │   │   ├── JwtAuthenticationFilter.java
 │   │   │   └── JwtExceptionHandler.java
 │   │   │
 │   │   ├── oauth
 │   │   │   ├── CustomOAuth2UserService.java
 │   │   │   └── OAuth2SuccessHandler.java
 │   │   │
 │   │   └── UserPrincipal.java
 │   │
 │   ├── exception
 │   │   ├── GlobalExceptionHandler.java
 │   │   ├── ErrorCode.java
 │   │   └── BusinessException.java
 │   │
 │   ├── response
 │   │   ├── ApiResponse.java
 │   │   └── ErrorResponse.java
 │   │
 │   └── util
 │       ├── DateUtils.java
 │       └── RedisKeyUtils.java
 │
 ├── domain
 │   ├── auth
 │   │   ├── controller
 │   │   │   └── AuthController.java
 │   │   ├── service
 │   │   │   └── AuthService.java
 │   │   ├── dto
 │   │   │   ├── LoginRequest.java
 │   │   │   └── TokenResponse.java
 │   │   └── mapper
 │   │       └── AuthMapper.xml
 │   │
 │   ├── user
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
 │   │
 │   ├── community
 │   ├── board
 │   ├── post
 │   ├── comment
 │   ├── friend
 │   ├── chat
 │   │   ├── websocket
 │   │   │   ├── ChatWebSocketHandler.java
 │   │   │   └── ChatMessageHandler.java
 │   │   ├── service
 │   │   └── dto
 │   │
 │   ├── timetable
 │   ├── notification
 │   └── university
 │
 └── infra
     ├── mail
     │   └── MailService.java
     ├── push
     │   └── FcmPushService.java
     └── redis
         └── RedisService.java

```

```
mybatis

resources/
 └── mybatis
     ├── mybatis-config.xml
     └── mappers
         ├── auth
         │   └── AuthMapper.xml
         ├── user
         ├── post
         └── chat


```