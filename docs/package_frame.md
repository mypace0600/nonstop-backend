```
Application

com.app.nonstop
 ├── global
 │   ├── config
 │   │   ├── SecurityConfig.java
 │   │   ├── OpenApiConfig.java
 │   │   ├── AppProperties.java
 │   │   ├── AzureBlobStorageConfig.java
 │   │   ├── FirebaseConfig.java
 │   │   ├── WebSocketConfig.java                 # WebSocket/STOMP 설정
 │   │   ├── KafkaTopicConfig.java                # Kafka 토픽 설정
 │   │   ├── KafkaProducerConfig.java             # Kafka Producer 설정
 │   │   └── KafkaConsumerConfig.java             # Kafka Consumer 설정
 │   │
 │   ├── security
 │   │   ├── jwt
 │   │   │   ├── JwtTokenProvider.java
 │   │   │   └── JwtAuthenticationFilter.java
 │   │   ├── user
 │   │   │   └── CustomUserDetails.java
 │   │   ├── websocket                            # WebSocket 보안 (채팅)
 │   │   │   ├── WebSocketAuthInterceptor.java    # JWT 인증
 │   │   │   ├── WebSocketSessionManager.java     # 세션 관리 (Redis)
 │   │   │   └── WebSocketRateLimitInterceptor.java # Rate Limiting
 │   │   └── oauth2
 │   │       ├── HttpCookieOAuth2AuthorizationRequestRepository.java
 │   │       ├── exception
 │   │       │   └── OAuth2AuthenticationProcessingException.java
 │   │       ├── handler
 │   │       │   ├── OAuth2AuthenticationSuccessHandler.java
 │   │       │   └── OAuth2AuthenticationFailureHandler.java
 │   │       ├── model
 │   │       │   ├── OAuth2UserInfo.java
 │   │       │   ├── OAuth2UserInfoFactory.java
 │   │       │   └── GoogleOAuth2UserInfo.java
 │   │       └── service
 │   │           └── CustomOAuth2UserService.java
 │   │
 │   ├── common
 │   │   ├── entity
 │   │   │   └── BaseTimeEntity.java
 │   │   ├── response
 │   │   │   └── ApiResponse.java
 │   │   └── exception
 │   │       └── FileUploadException.java
 │   │
 │   ├── properties
 │   │   └── WebSocketProperties.java             # WebSocket 설정값 관리
 │   │
 │   └── util
 │       └── CookieUtil.java
 │
 ├── infra
 │   └── blob
 │       └── BlobStorageUploader.java
 │
 ├── mapper                              # MyBatis Mapper 인터페이스 (분리됨)
 │   ├── AuthMapper.java
 │   ├── BoardMapper.java
 │   ├── ChatMapper.java
 │   ├── ChatRoomMapper.java
 │   ├── CommentMapper.java
 │   ├── CommunityMapper.java
 │   ├── DeviceMapper.java
 │   ├── FileMapper.java
 │   ├── FriendMapper.java
 │   ├── NotificationMapper.java
 │   ├── PostMapper.java
 │   ├── RefreshTokenMapper.java
 │   ├── ReportMapper.java
 │   ├── UserMapper.java
 │   └── VerificationMapper.java
 │
 ├── domain
 │   ├── auth
 │   │   ├── controller
 │   │   │   └── AuthController.java
 │   │   ├── service
 │   │   │   ├── AuthService.java
 │   │   │   └── AuthServiceImpl.java
 │   │   └── dto
 │   │       ├── SignUpRequestDto.java
 │   │       ├── LoginRequestDto.java
 │   │       ├── GoogleLoginRequestDto.java
 │   │       ├── TokenResponseDto.java
 │   │       ├── RefreshRequestDto.java
 │   │       ├── EmailCheckRequestDto.java
 │   │       └── NicknameCheckRequestDto.java
 │   │
 │   ├── user
 │   │   ├── controller
 │   │   │   └── UserController.java
 │   │   ├── service
 │   │   │   ├── UserService.java
 │   │   │   └── UserServiceImpl.java
 │   │   ├── dto
 │   │   │   ├── UserResponseDto.java
 │   │   │   ├── ProfileUpdateRequestDto.java
 │   │   │   ├── PasswordUpdateRequestDto.java
 │   │   │   └── VerificationStatusResponseDto.java
 │   │   ├── entity
 │   │   │   ├── User.java
 │   │   │   ├── UserRole.java
 │   │   │   ├── AuthProvider.java
 │   │   │   └── VerificationMethod.java
 │   │   └── exception
 │   │       ├── UserNotFoundException.java
 │   │       ├── DuplicateNicknameException.java
 │   │       ├── InvalidPasswordException.java
 │   │       └── InvalidPasswordChangeAttemptException.java
 │   │
 │   ├── token
 │   │   └── entity
 │   │       └── RefreshToken.java
 │   │
 │   ├── device
 │   │   ├── controller
 │   │   │   └── DeviceController.java
 │   │   ├── service
 │   │   │   ├── DeviceService.java
 │   │   │   └── DeviceServiceImpl.java
 │   │   ├── dto
 │   │   │   └── DeviceTokenRequestDto.java
 │   │   └── entity
 │   │       └── DeviceToken.java
 │   │
 │   ├── verification
 │   │   ├── controller
 │   │   │   └── VerificationController.java
 │   │   ├── service
 │   │   │   ├── VerificationService.java
 │   │   │   └── VerificationServiceImpl.java
 │   │   ├── entity
 │   │   │   ├── StudentVerificationRequest.java
 │   │   │   └── ReportStatus.java
 │   │   └── exception
 │   │       ├── FileTooLargeException.java
 │   │       ├── InvalidFileTypeException.java
 │   │       └── VerificationRequestAlreadyExistsException.java
 │   │
 │   ├── friend
 │   │   ├── controller
 │   │   │   └── FriendController.java
 │   │   ├── service
 │   │   │   ├── FriendService.java
 │   │   │   └── FriendServiceImpl.java
 │   │   ├── dto
 │   │   │   └── FriendDto.java
 │   │   ├── entity
 │   │   │   ├── Friend.java
 │   │   │   ├── FriendStatus.java
 │   │   │   └── UserBlock.java
 │   │   └── exception
 │   │       ├── CannotSendFriendRequestException.java
 │   │       ├── FriendRequestNotFoundException.java
 │   │       ├── InvalidFriendRequestAccessException.java
 │   │       ├── AlreadyBlockedException.java
 │   │       └── CannotBlockSelfException.java
 │   │
 │   ├── community
 │   │   ├── controller
 │   │   │   ├── CommunityController.java
 │   │   ├── service
 │   │   │   ├── CommunityService.java
 │   │   │   ├── CommunityServiceImpl.java
 │   │   │   ├── BoardService.java
 │   │   │   └── BoardServiceImpl.java
 │   │   ├── dto
 │   │   │   ├── CommunityResponseDto.java
 │   │   │   ├── CommunityListWrapper.java
 │   │   │   └── BoardResponseDto.java
 │   │   └── entity
 │   │       ├── Community.java
 │   │       ├── Board.java
 │   │       └── BoardType.java
 │   │
 │   ├── file
 │   │   ├── controller
 │   │   │   └── FileController.java
 │   │   ├── service
 │   │   │   ├── FileService.java
 │   │   │   └── FileServiceImpl.java
 │   │   ├── dto
 │   │   │   ├── FileUploadRequestDto.java
 │   │   │   └── FileUploadCompleteDto.java
 │   │   └── entity
 │   │       └── File.java
 │   │
 │   ├── university
 │   │   └── entity
 │   │       └── University.java
 │   │
 │   └── major
 │       └── entity
 │           └── Major.java
 │
 │   ├── chat                              # 채팅 시스템 (상세: docs/chatting-docs/chat-system.md)
 │   │   ├── controller
 │   │   │   ├── ChatController.java              # REST API (채팅방 CRUD, 메시지 조회)
 │   │   │   └── WebSocketChatController.java     # STOMP 메시지 수신
 │   │   ├── dto
 │   │   │   ├── ChatMessageDto.java              # 메시지 DTO
 │   │   │   ├── ChatReadEventDto.java            # 읽음 이벤트 DTO (Kafka)
 │   │   │   └── ChatReadStatusDto.java           # 읽음 상태 DTO (WebSocket)
 │   │   ├── entity
 │   │   │   ├── ChatRoom.java
 │   │   │   ├── ChatRoomMember.java
 │   │   │   └── Message.java
 │   │   └── service
 │   │       ├── ChatService.java
 │   │       ├── ChatServiceImpl.java
 │   │       ├── ChatKafkaProducer.java           # 메시지 Kafka 발행
 │   │       ├── ChatKafkaConsumer.java           # 메시지 Kafka 소비
 │   │       ├── ChatReadEventProducer.java       # 읽음 이벤트 발행
 │   │       └── ChatReadEventConsumer.java       # 읽음 이벤트 소비
 │   │
 │   ├── notification
 │   │   ├── controller
 │   │   ├── dto
 │   │   ├── entity
 │   │   └── service
 │   │
 │   └── report
 │       ├── controller
 │       ├── dto
 │       ├── entity
 │       └── service
 │
 └── NonstopApplication.java
```

```
MyBatis XML Mappers

resources/
 └── mybatis
     └── mappers
         ├── auth
         │   └── AuthMapper.xml
         ├── user
         │   └── UserMapper.xml
         ├── token
         │   └── RefreshTokenMapper.xml
         ├── device
         │   └── DeviceMapper.xml
         ├── verification
         │   └── VerificationMapper.xml
         ├── friend
         │   └── FriendMapper.xml
         ├── community
         │   ├── CommunityMapper.xml
         │   ├── BoardMapper.xml
         │   ├── CommentMapper.xml
         │   └── PostMapper.xml
         └── file
             └── FileMapper.xml
         ├── chat
         │   ├── ChatMapper.xml
         │   └── ChatRoomMapper.xml
         ├── notification
         │   └── NotificationMapper.xml
         └── report
             └── ReportMapper.xml
```