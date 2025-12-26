```
Application

com.app.nonstop
 ├── global
 │   ├── config
 │   │   ├── SecurityConfig.java
 │   │   ├── OpenApiConfig.java
 │   │   ├── AppProperties.java
 │   │   ├── AzureBlobStorageConfig.java
 │   │   └── FirebaseConfig.java
 │   │
 │   ├── security
 │   │   ├── jwt
 │   │   │   ├── JwtTokenProvider.java
 │   │   │   └── JwtAuthenticationFilter.java
 │   │   ├── user
 │   │   │   └── CustomUserDetails.java
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
 │   └── util
 │       └── CookieUtil.java
 │
 ├── infra
 │   └── blob
 │       └── BlobStorageUploader.java
 │
 ├── mapper                              # MyBatis Mapper 인터페이스 (분리됨)
 │   ├── AuthMapper.java
 │   ├── UserMapper.java
 │   ├── RefreshTokenMapper.java
 │   ├── DeviceMapper.java
 │   ├── VerificationMapper.java
 │   ├── FriendMapper.java
 │   ├── CommunityMapper.java
 │   ├── BoardMapper.java
 │   └── FileMapper.java
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
 │   │   │   └── CommunityController.java
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
         │   └── BoardMapper.xml
         └── file
             └── FileMapper.xml
```
