package com.app.nonstop.domain.community.mapper;

import com.app.nonstop.domain.community.dto.PostResponseDto;
import com.app.nonstop.domain.community.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PostMapper {
    // Post CRUD
    void insert(Post post);
    Optional<Post> findById(Long id);
    void update(Post post);
    void delete(Long id); // Soft Delete
    
    // View Count
    void incrementViewCount(Long id);

    // Like Related
    int existsLike(@Param("userId") Long userId, @Param("postId") Long postId);
    void insertLike(@Param("userId") Long userId, @Param("postId") Long postId);
    void deleteLike(@Param("userId") Long userId, @Param("postId") Long postId);
    void restoreLike(@Param("userId") Long userId, @Param("postId") Long postId);
    Long getLikeCount(Long postId);
    boolean isLiked(@Param("userId") Long userId, @Param("postId") Long postId);
    
    // Complex Selects (returning DTO)
    Optional<PostResponseDto> findByIdWithDetail(@Param("id") Long id, @Param("currentUserId") Long currentUserId);
    
    List<PostResponseDto> findAllByBoardIdWithDetail(
            @Param("boardId") Long boardId, 
            @Param("limit") int limit, 
            @Param("offset") int offset,
            @Param("currentUserId") Long currentUserId
    );
}