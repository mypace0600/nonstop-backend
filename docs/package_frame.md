```
Application

com.app.nonstop
 ├── global
 │   ├── config
 │   │   ├── SecurityConfig.java
 │   │   ├── WebSocketConfig.java
 │   │   ├── RedisConfig.java
 │   │   ├── WebMvcConfig.java
 │   │   └── AzureBlobConfig.java
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
 ├── infra
 │   ├── blob
 │   │   └── BlobStorageUploader.java
 │   └── fcm
 │       └── FcmPushService.java
 │
 ├── domain
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
 │   ├── university
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
 │   │
 │   ├── friend
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
 │   │
 │   ├── block
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
 │   │
 │   ├── verification
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
 │   │
 │   ├── file
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
 │   │
 │   ├── report
 │   │   ├── controller
 │   │   ├── service
 │   │   ├── dto
 │   │   └── mapper
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