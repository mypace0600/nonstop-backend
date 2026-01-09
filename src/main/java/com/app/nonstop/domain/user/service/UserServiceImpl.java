package com.app.nonstop.domain.user.service;

import com.app.nonstop.domain.major.entity.Major;
import com.app.nonstop.domain.university.entity.University;
import com.app.nonstop.domain.user.dto.PasswordUpdateRequestDto;
import com.app.nonstop.domain.user.dto.ProfileUpdateRequestDto;
import com.app.nonstop.domain.user.dto.UserResponseDto;
import com.app.nonstop.domain.user.dto.VerificationStatusResponseDto;
import com.app.nonstop.domain.user.entity.AuthProvider;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.exception.DuplicateNicknameException;
import com.app.nonstop.domain.user.exception.InvalidPasswordChangeAttemptException;
import com.app.nonstop.domain.user.exception.InvalidPasswordException;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import com.app.nonstop.mapper.DeviceMapper;
import com.app.nonstop.mapper.MajorMapper;
import com.app.nonstop.mapper.UniversityMapper;
import com.app.nonstop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final DeviceMapper deviceMapper; // FCM 토큰 삭제를 위해 주입
    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 현재 로그인된 사용자의 정보를 조회합니다.
     *
     * @param userId 현재 로그인된 사용자의 ID
     * @return UserResponseDto 사용자 정보 응답 DTO
     * @throws UserNotFoundException 사용자를 찾을 수 없을 때
     */
    @Override
    public UserResponseDto getMyInfo(Long userId) {
        // 1. Mapper를 통해 DB에서 사용자 정보를 조회합니다. `findById`는 Optional<User>를 반환합니다.
        // 2. .orElseThrow()를 사용하여 Optional이 비어있을 경우(사용자가 없을 경우) UserNotFoundException을 즉시 던집니다.
        User user = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        
        // 3. 조회된 User 엔티티를 UserResponseDto로 변환하여 반환합니다.
        return UserResponseDto.of(user);
    }

    /**
     * 사용자의 프로필 정보를 업데이트합니다.
     * 닉네임 중복 검사, 유효성 검사 등 비즈니스 로직을 포함합니다.
     *
     * @param userId     현재 로그인된 사용자 ID
     * @param requestDto 업데이트할 프로필 정보 DTO
     * @throws UserNotFoundException         사용자를 찾을 수 없을 때
     * @throws DuplicateNicknameException    요청된 닉네임이 이미 존재할 때
     */
    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequestDto requestDto) {
        // 1. 사용자 존재 여부 확인
        User existingUser = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 2. 닉네임 변경 요청이 있고, 기존 닉네임과 다를 경우 중복 검사
        //    - nickname (unique, deleted_at IS NULL) 제약 조건에 맞춰 중복을 확인합니다.
        if (requestDto.getNickname() != null && !requestDto.getNickname().equals(existingUser.getNickname())) {
            if (userMapper.existsByNickname(requestDto.getNickname())) {
                throw new DuplicateNicknameException();
            }
        }

        // TODO: (고려사항) universityId와 majorId가 제공된 경우, 해당 ID가 유효한지 검사하는 로직이 필요합니다.
        //       예: universityMapper.findById(requestDto.getUniversityId()).orElseThrow(UniversityNotFoundException::new);
        //       현재는 DTO에서 ID만 받아 User 객체에 설정하여 매퍼로 전달합니다. 이는 Mapper에서 JOIN하지 않고 ID만 업데이트할 때 효율적입니다.
        University university = null;
        if (requestDto.getUniversityId() != null) {
            university = new University(); // 임시 객체, ID만 설정
            university.setId(requestDto.getUniversityId());
        }

        Major major = null;
        if (requestDto.getMajorId() != null) {
            major = new Major(); // 임시 객체, ID만 설정
            major.setId(requestDto.getMajorId());
        }

        // 3. 업데이트할 User 객체 생성 (Builder 패턴 사용)
        //    - DTO에서 제공된 필드만 User 객체에 설정하여 매퍼에 전달하면,
        //      매퍼의 <set> 블록과 <if test="field != null"> 조건에 의해 해당 필드만 업데이트됩니다.
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

    /**
     * 사용자의 비밀번호를 변경합니다.
     * - 이메일로 가입한 사용자만 비밀번호 변경이 가능합니다.
     * - 현재 비밀번호를 확인 후 새 비밀번호로 업데이트합니다.
     *
     * @param userId     현재 로그인된 사용자 ID
     * @param requestDto 현재 비밀번호와 새 비밀번호 정보 DTO
     * @throws UserNotFoundException               사용자를 찾을 수 없을 때
     * @throws InvalidPasswordChangeAttemptException 소셜 로그인 사용자가 비밀번호 변경을 시도할 때 (비즈니스 규칙)
     * @throws InvalidPasswordException            현재 비밀번호가 일치하지 않을 때
     */
    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequestDto requestDto) {
        // 1. 사용자 존재 여부 확인
        User user = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 2. [비즈니스 규칙] 이메일 가입자인지 확인 (소셜 로그인 유저는 비밀번호 변경 불가)
        //    - AuthProvider가 EMAIL이 아니면 InvalidPasswordChangeAttemptException 발생
        if (user.getAuthProvider() != AuthProvider.EMAIL) {
            throw new InvalidPasswordChangeAttemptException();
        }

        // 3. 현재 비밀번호 일치 여부 확인
        //    - passwordEncoder.matches(평문, 암호화된 비밀번호)를 사용하여 현재 비밀번호가 일치하는지 검증
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 4. 새 비밀번호 암호화
        //    - 새 비밀번호도 반드시 암호화하여 저장해야 합니다.
        String encodedNewPassword = passwordEncoder.encode(requestDto.getNewPassword());

        // 5. DB에 암호화된 새 비밀번호 업데이트
        userMapper.updatePassword(userId, encodedNewPassword);
    }

    /**
     * 회원 계정을 비활성화(soft delete)합니다.
     * - 실제 데이터를 삭제하는 대신 `deleted_at` 필드를 현재 시간으로 업데이트합니다.
     * - 연결된 모든 FCM 디바이스 토큰을 삭제하여 푸시 알림 오발송을 방지합니다.
     *
     * @param userId 탈퇴할 사용자의 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없을 때
     */
    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        // 1. 사용자 존재 여부 확인 (존재하지 않으면 UserNotFoundException 발생)
        userMapper.findById(userId).orElseThrow(UserNotFoundException::new);

        // TODO: (보안/정합성) 회원 탈퇴 시, 해당 사용자의 Refresh Token을 모두 무효화하는 로직이 필요합니다. (관련 기능 미구현)

        // 2. 사용자와 연결된 모든 FCM 디바이스 토큰을 삭제합니다.
        //    - FCM 푸시 알림 오발송 및 데이터 누적 방지를 위함
        deviceMapper.deleteAllByUserId(userId);
        
        // 3. 사용자의 deleted_at 필드를 현재 시간으로 업데이트하여 비활성화(soft delete)합니다.
        userMapper.softDelete(userId);
    }

    /**
     * 현재 로그인된 사용자의 대학생 인증 상태 및 인증 방식을 조회합니다.
     *
     * @param userId 현재 로그인된 사용자의 ID
     * @return VerificationStatusResponseDto 인증 상태 응답 DTO
     * @throws UserNotFoundException 사용자를 찾을 수 없을 때
     */
    @Override
    @Transactional(readOnly = true)
    public VerificationStatusResponseDto getVerificationStatus(Long userId) {
        // 1. Mapper를 통해 DB에서 사용자 정보를 조회합니다.
        User user = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new); // 사용자를 찾을 수 없을 경우 UserNotFoundException 발생

        // 2. 조회된 User 엔티티의 인증 관련 필드를 VerificationStatusResponseDto로 변환하여 반환합니다.
        return VerificationStatusResponseDto.of(user);
    }

    /**
     * 사용자의 대학교 및 전공 정보를 설정합니다.
     * - 대학교 ID가 유효한지 검증합니다.
     * - 전공 ID가 제공된 경우, 해당 전공이 선택된 대학교에 속하는지 검증합니다.
     *
     * @param userId       사용자 ID
     * @param universityId 대학교 ID
     * @param majorId      전공 ID (선택)
     * @throws UserNotFoundException     사용자를 찾을 수 없을 때
     * @throws ResourceNotFoundException 대학교 또는 전공을 찾을 수 없을 때
     * @throws IllegalArgumentException  전공이 해당 대학교에 속하지 않을 때
     */
    @Override
    @Transactional
    public void updateUniversity(Long userId, Long universityId, Long majorId) {
        // 1. 사용자 존재 여부 확인
        userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 2. 대학교 존재 여부 확인
        universityMapper.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found: " + universityId));

        // 3. 전공이 제공된 경우, 해당 전공이 선택된 대학교에 속하는지 검증
        if (majorId != null) {
            boolean isValidMajor = majorMapper.existsByIdAndUniversityId(majorId, universityId);
            if (!isValidMajor) {
                throw new IllegalArgumentException("Major does not belong to the selected university");
            }
        }

        // 4. 사용자 대학교/전공 정보 업데이트
        userMapper.updateUniversity(userId, universityId, majorId);
    }
}
