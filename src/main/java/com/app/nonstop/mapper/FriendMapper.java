package com.app.nonstop.mapper;

import com.app.nonstop.domain.friend.dto.FriendDto;
import com.app.nonstop.domain.friend.entity.Friend;
import com.app.nonstop.domain.friend.entity.UserBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FriendMapper {

    // Friend
    void insertFriend(Friend friend);

    void updateFriendStatus(Friend friend);

    void softDeleteFriend(@Param("id") Long id);

    Optional<Friend> findFriendById(@Param("id") Long id);

    Optional<Friend> findFriendByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    List<FriendDto.FriendRequestResponseDto> findReceivedFriendRequestsByUserId(@Param("userId") Long userId);

    List<FriendDto.FriendResponseDto> findFriendsByUserId(@Param("userId") Long userId);


    // User Block
    void insertUserBlock(UserBlock userBlock);

    boolean existsBlockByUsers(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    Optional<UserBlock> findBlockByUsers(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    void deleteUserBlock(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    List<FriendDto.BlockedUserResponseDto> findBlockedUsersByBlockerId(@Param("blockerId") Long blockerId);
}
