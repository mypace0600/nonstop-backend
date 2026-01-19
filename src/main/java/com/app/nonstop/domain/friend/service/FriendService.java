package com.app.nonstop.domain.friend.service;

import com.app.nonstop.domain.friend.dto.FriendDto;

import java.util.List;

public interface FriendService {

    // Friend List
    List<FriendDto.FriendResponseDto> getFriendList(Long userId);

    // Friend Request
    List<FriendDto.FriendRequestResponseDto> getReceivedFriendRequests(Long userId);

    void sendFriendRequest(Long requesterId, FriendDto.FriendRequestSendDto requestDto);

    void acceptFriendRequest(Long userId, Long requestId);

    void rejectFriendRequest(Long userId, Long requestId);

    void cancelFriendRequest(Long userId, Long requestId);

    // Block User
    void blockUser(Long blockerId, FriendDto.UserBlockRequestDto requestDto);

    void removeFriend(Long userId, Long friendId);
}
