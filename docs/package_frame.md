```
Application

com.app.nonstop
 ├── global
 │   ├── config
 │   │   ├── SecurityConfig.java      // URL별 권한 제어 (Graceful Degradation)
 │   │   ├── WebSocketConfig.java     // /ws/chat 엔드포인트 설정
 │   │   ├── RedisConfig.java         // RedisTemplate 설정
 │   │   ├── WebMvcConfig.java        // CORS 등 설정
 │   │   └── AzureBlobConfig.java     // (New) Azure Blob Storage 설정
 │   │
 │   ├── security
 │   │   ├── jwt
 │   │   │   ├── JwtTokenProvider.java
 │   │   │   └── JwtAuthenticationFilter.java
 │   │   └── user
 │   │       └── CustomUserDetails.java // UserPrincipal 대체
 │   │
 │   ├── common
 │   │   ├── response
 │   │   │   └── ApiResponse.java
 │   │   └── exception
 │   │       └── GlobalExceptionHandler.java
 │   │
 │   └── util
 │       └── FileUtils.java            // Azure Blob SAS URL 생성 등 유틸
 │
 ├── infra                            // 외부 시스템 연동 (구현체 분리)
 │   ├── blob                         // (변경) S3 → blob
 │   │   └── BlobUploader.java         // (변경) 이미지 업로드 (SAS URL 방식)
 │   └── fcm
 │       └── FcmPushService.java      // (New) 푸시 알림 발송
 │
 ├── domain                           // 도메인 주도 설계 (DDD)
 │   ├── auth
 │   │   ├── controller
 │   │   │   └── AuthController.java
 │   │   ├── service
 │   │   │   └── AuthService.java
 │   │   ├── dto
 │   │   │   └── AuthDto.java         // (New) Inner Class로 LoginReq, TokenRes 관리
 │   │   └── mapper
 │   │       └── AuthMapper.java      // (New) Interface는 여기에 위치
 │   │
 │   ├── user
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
 │   │
 │   ├── chat
 │   │   ├── controller
│   │   │   └── ChatController.java      // (New) Handles chat message via WebSocket
 │   │   ├── kafka
 │   │   │   ├── ChatKafkaProducer.java   // (New) Produces messages to Kafka
 │   │   │   └── ChatKafkaConsumer.java   // (New) Consumes messages from Kafka
 │   │   ├── service
 │   │   │   └── ChatService.java         // (To be updated) Business logic for chat
 │   │   ├── dto
 │   │   │   └── ChatDto.java             // (New) DTO for chat messages
 │   │   └── mapper
 │   │       └── ChatMapper.java          // (New) Mapper for chat messages

 │   │
 │   ├── community (board, post, comment...)
 │   ├── notification
 │   └── timetable
 │
 └── NonstopApplication.java
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