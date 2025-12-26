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
 │   │   └── ...
 │   ├── community
 │   │   └── ...
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