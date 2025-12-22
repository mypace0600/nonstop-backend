package com.app.nonstop.global.security.user;

import com.app.nonstop.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Long userId;
    private final String email;
    private final Long universityId; // Added
    private final Boolean isVerified; // Added
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public CustomUserDetails(Long userId, String email, Long universityId, Boolean isVerified, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.universityId = universityId; // Set in constructor
        this.isVerified = isVerified;     // Set in constructor
        this.authorities = authorities;
    }

    public static CustomUserDetails create(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getUniversityId(), // Pass universityId
                user.getIsVerified(),   // Pass isVerified
                Collections.singletonList(new SimpleGrantedAuthority(user.getUserRole().name()))
        );
    }

    public static CustomUserDetails create(User user, Map<String, Object> attributes) {
        CustomUserDetails userDetails = CustomUserDetails.create(user);
        userDetails.setAttributes(attributes);
        return userDetails;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // Not used
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
