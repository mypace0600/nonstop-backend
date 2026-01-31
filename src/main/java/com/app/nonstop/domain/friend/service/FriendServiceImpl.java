package com.app.nonstop.domain.friend.service;

import com.app.nonstop.domain.friend.dto.FriendDto;
import com.app.nonstop.domain.friend.entity.Friend;
import com.app.nonstop.domain.friend.entity.FriendStatus;
import com.app.nonstop.domain.friend.entity.UserBlock;
import com.app.nonstop.domain.friend.exception.*;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.mapper.FriendMapper;
import com.app.nonstop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FriendDto.FriendResponseDto> getFriendList(Long userId) {
        return friendMapper.findFriendsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendDto.FriendRequestResponseDto> getReceivedFriendRequests(Long userId) {
        return friendMapper.findReceivedFriendRequestsByUserId(userId);
    }

    @Override
    @Transactional
    public void sendFriendRequest(Long senderId, FriendDto.FriendRequestSendDto requestDto) {
        Long receiverId = requestDto.getTargetUserId();

        if (Objects.equals(senderId, receiverId)) {
            throw new CannotSendFriendRequestException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        userMapper.findById(receiverId).orElseThrow(UserNotFoundException::new);

        if (friendMapper.existsBlockByUsers(receiverId, senderId)) {
            throw new CannotSendFriendRequestException("상대방이 당신을 차단하여 친구 요청을 보낼 수 없습니다.");
        }
        if (friendMapper.existsBlockByUsers(senderId, receiverId)) {
            throw new CannotSendFriendRequestException("당신이 상대방을 차단하여 친구 요청을 보낼 수 없습니다. 차단을 먼저 해제해주세요.");
        }

        var existingFriendship = friendMapper.findFriendByUsers(senderId, receiverId);
        if (existingFriendship.isPresent()) {
            Friend friendship = existingFriendship.get();

            if (friendship.getStatus() == FriendStatus.WAITING) {
                // Idempotency: if I already sent the same request, treat as success.
                if (Objects.equals(friendship.getSenderId(), senderId)) {
                    return;
                }
                throw new CannotSendFriendRequestException("이미 친구 요청을 보냈거나 받은 상태입니다.");
            }

            if (friendship.getStatus() == FriendStatus.ACCEPTED) {
                throw new CannotSendFriendRequestException("이미 친구 관계입니다.");
            }
        }

        Friend friend = Friend.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .status(FriendStatus.WAITING)
                .build();

        friendMapper.insertFriend(friend);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long userId, Long requestId) {
        Friend friend = friendMapper.findFriendById(requestId)
                .orElseThrow(FriendRequestNotFoundException::new);

        if (!Objects.equals(friend.getReceiverId(), userId)) {
            throw new InvalidFriendRequestAccessException();
        }

        if (friend.getStatus() != FriendStatus.WAITING) {
            throw new CannotSendFriendRequestException("이미 처리된 요청입니다.");
        }

        Friend updatedFriend = Friend.builder()
                .id(friend.getId())
                .status(FriendStatus.ACCEPTED)
                .build();
        friendMapper.updateFriendStatus(updatedFriend);
    }

    @Override
    @Transactional
    public void rejectFriendRequest(Long userId, Long requestId) {
        Friend friend = friendMapper.findFriendById(requestId)
                .orElseThrow(FriendRequestNotFoundException::new);

        if (!Objects.equals(friend.getReceiverId(), userId)) {
            throw new InvalidFriendRequestAccessException();
        }

        if (friend.getStatus() != FriendStatus.WAITING) {
            throw new CannotSendFriendRequestException("이미 처리된 요청입니다.");
        }

        Friend updatedFriend = Friend.builder()
                .id(friend.getId())
                .status(FriendStatus.REJECTED)
                .build();
        friendMapper.updateFriendStatus(updatedFriend);
    }

    @Override
    @Transactional
    public void cancelFriendRequest(Long userId, Long requestId) {
        Friend friend = friendMapper.findFriendById(requestId)
                .orElseThrow(FriendRequestNotFoundException::new);

        if (!Objects.equals(friend.getSenderId(), userId)) {
            throw new InvalidFriendRequestAccessException();
        }

        if (friend.getStatus() != FriendStatus.WAITING) {
            throw new CannotSendFriendRequestException("이미 처리된 요청입니다.");
        }

        friendMapper.softDeleteFriend(requestId);
    }

    @Override
    @Transactional
    public void blockUser(Long blockerId, FriendDto.UserBlockRequestDto requestDto) {
        Long blockedId = requestDto.getTargetUserId();

        if (Objects.equals(blockerId, blockedId)) {
            throw new CannotBlockSelfException();
        }

        userMapper.findById(blockedId).orElseThrow(UserNotFoundException::new);

        if (friendMapper.existsBlockByUsers(blockerId, blockedId)) {
            throw new AlreadyBlockedException();
        }

        UserBlock userBlock = UserBlock.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();

        friendMapper.insertUserBlock(userBlock);
    }

    @Override
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        if (!friendMapper.existsBlockByUsers(blockerId, blockedId)) {
            throw new NotBlockedException();
        }
        friendMapper.deleteUserBlock(blockerId, blockedId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendDto.BlockedUserResponseDto> getBlockedUsers(Long blockerId) {
        return friendMapper.findBlockedUsersByBlockerId(blockerId);
    }

    @Override
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        Friend friendship = friendMapper.findFriendByUsers(userId, friendId)
                .orElseThrow(FriendRequestNotFoundException::new);

        if (friendship.getStatus() != FriendStatus.ACCEPTED) {
            throw new CannotSendFriendRequestException("친구 관계가 아닙니다.");
        }

        friendMapper.softDeleteFriend(friendship.getId());
    }
}
