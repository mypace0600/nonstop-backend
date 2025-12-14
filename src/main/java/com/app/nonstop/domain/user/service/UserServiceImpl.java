package com.app.nonstop.domain.user.service;

import com.app.nonstop.domain.device.mapper.DeviceMapper;
import com.app.nonstop.domain.major.entity.Major;
import com.app.nonstop.domain.university.entity.University;
import com.app.nonstop.domain.user.dto.PasswordUpdateRequestDto;
import com.app.nonstop.domain.user.dto.ProfileUpdateRequestDto;
import com.app.nonstop.domain.user.dto.UserResponseDto;
import com.app.nonstop.domain.user.entity.AuthProvider;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.exception.DuplicateNicknameException;
import com.app.nonstop.domain.user.exception.InvalidPasswordChangeAttemptException;
import com.app.nonstop.domain.user.exception.InvalidPasswordException;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final DeviceMapper deviceMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto getMyInfo(Long userId) {
        // 1. Mapper를 통해 DB에서 사용자 정보를 조회합니다. `findById`는 Optional<User>를 반환합니다.
        // 2. .orElseThrow()를 사용하여 Optional이 비어있을 경우(사용자가 없을 경우) UserNotFoundException을 즉시 던집니다.
        User user = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        
        // 3. 조회된 User 엔티티를 UserResponseDto로 변환하여 반환합니다.
        return UserResponseDto.of(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequestDto requestDto) {
        // 1. 사용자 존재 여부 확인
        User existingUser = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 2. 닉네임 변경 요청이 있고, 기존 닉네임과 다를 경우 중복 검사
        if (requestDto.getNickname() != null && !requestDto.getNickname().equals(existingUser.getNickname())) {
            if (userMapper.existsByNickname(requestDto.getNickname())) {
                throw new DuplicateNicknameException();
            }
        }

        // TODO: universityId와 majorId가 제공된 경우, 해당 ID가 유효한지 검사하는 로직이 필요합니다.
        // 현재는 ID만 User 객체에 설정하여 매퍼로 전달합니다. (성능상 Mapper에서 JOIN하지 않고 ID만 업데이트)
        University university = null;
        if (requestDto.getUniversityId() != null) {
            // universityMapper.findById(requestDto.getUniversityId()).orElseThrow(UniversityNotFoundException::new);
            university = new University(); // 임시 객체, ID만 설정
            university.setId(requestDto.getUniversityId());
        }

        Major major = null;
        if (requestDto.getMajorId() != null) {
            // majorMapper.findById(requestDto.getMajorId()).orElseThrow(MajorNotFoundException::new);
            major = new Major(); // 임시 객체, ID만 설정
            major.setId(requestDto.getMajorId());
        }

        // 3. 업데이트할 User 객체 생성 (Builder 패턴 사용)
        // DTO에서 제공된 필드만 User 객체에 설정하여 매퍼에 전달하면,
        // 매퍼의 <set> 블록과 <if test="field != null"> 조건에 의해 해당 필드만 업데이트됩니다.
        User userToUpdate = User.builder()
                .id(userId) // WHERE 조건에 사용될 ID
                .nickname(requestDto.getNickname())
                .university(university)
                .major(major)
                .profileImageUrl(requestDto.getProfileImageUrl())
                .introduction(requestDto.getIntroduction())
                .preferredLanguage(requestDto.getPreferredLanguage())
                .build();
        
        // 4. Mapper를 통해 DB 업데이트 수행
        userMapper.updateProfile(userToUpdate);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequestDto requestDto) {
        // 1. 사용자 존재 여부 확인
        User user = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 2. 이메일 가입자인지 확인 (소셜 로그인 유저는 비밀번호 변경 불가)
        if (user.getAuthProvider() != AuthProvider.EMAIL) {
            throw new InvalidPasswordChangeAttemptException();
        }

        // 3. 현재 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 4. 새 비밀번호 암호화
        String encodedNewPassword = passwordEncoder.encode(requestDto.getNewPassword());

        // 5. DB에 암호화된 새 비밀번호 업데이트
        userMapper.updatePassword(userId, encodedNewPassword);
    }

    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        // TODO: 회원 탈퇴 시, 해당 사용자의 Refresh Token을 모두 무효화하는 로직이 필요합니다.

        // 1. 사용자와 연결된 모든 FCM 디바이스 토큰을 삭제합니다.
        deviceMapper.deleteAllByUserId(userId);
        
        // 2. 사용자의 deleted_at 필드를 현재 시간으로 업데이트하여 비활성화합니다.
        userMapper.softDelete(userId);
    }
}
