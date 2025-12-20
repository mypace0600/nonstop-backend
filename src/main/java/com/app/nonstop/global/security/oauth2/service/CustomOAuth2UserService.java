package com.app.nonstop.global.security.oauth2.service;

import com.app.nonstop.domain.user.entity.AuthProvider;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.entity.UserRole;
import com.app.nonstop.domain.user.mapper.UserMapper;
import com.app.nonstop.global.security.oauth2.exception.OAuth2AuthenticationProcessingException;
import com.app.nonstop.global.security.oauth2.model.OAuth2UserInfo;
import com.app.nonstop.global.security.oauth2.model.OAuth2UserInfoFactory;
import com.app.nonstop.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userMapper.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getAuthProvider().name().equalsIgnoreCase(registrationId)) {
                throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                        user.getAuthProvider() + " account. Please use your " + user.getAuthProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(userRequest, oAuth2UserInfo);
        }

        return CustomUserDetails.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        User user = User.builder()
                .authProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))
                .providerId(oAuth2UserInfo.getId())
                .nickname(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .userRole(UserRole.USER)
                .profileImageUrl(oAuth2UserInfo.getImageUrl())
                .build();
        userMapper.insertUser(user);
        return user;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        User.UserBuilder builder = User.builder()
                .id(existingUser.getId())
                .nickname(oAuth2UserInfo.getName())
                .profileImageUrl(oAuth2UserInfo.getImageUrl());
        
        userMapper.updateUser(builder.build());
        return existingUser;
    }
}
