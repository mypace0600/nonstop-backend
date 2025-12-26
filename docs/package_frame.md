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
 │   │   └── user
 │   │
 │   ├── common
 │   │   ├── response
 │   │   └── exception
 │   │
 │   └── util
 │
 ├── infra
 │   ├── blob
 │   │   └── BlobStorageUploader.java
 │   └── fcm
 │       └── FcmPushService.java
 │
 ├── domain
 │   ├── auth
 │   │   └── ...
 │   ├── user
 │   │   └── ...
 │   ├── file
 │   │   ├── controller
 │   │   │   └── FileController.java
 │   │   ├── service
 │   │   │   └── FileService.java
 │   │   ├── dto
 │   │   │   └── FileUploadRequestDto.java
 │   │   ├── entity
 │   │   │   └── File.java
 │   │   └── mapper
 │   │       └── FileMapper.java
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
 │   │   └── ...
 │   └── timetable
 │       └── ...
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