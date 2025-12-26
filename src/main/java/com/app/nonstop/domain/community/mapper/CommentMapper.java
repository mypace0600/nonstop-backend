package com.app.nonstop.domain.community.mapper;

import com.app.nonstop.domain.community.dto.CommentDto;
import com.app.nonstop.domain.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CommentMapper {
    // Comment CRUD
    void insert(Comment comment);
    Optional<Comment> findById(Long id);
    void update(Comment comment);
    void delete(Long id); // Soft Delete

    // Like Related
    int existsLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
    void insertLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
    void deleteLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
    void restoreLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
    Long getLikeCount(Long commentId);
    boolean isLiked(@Param("userId") Long userId, @Param("commentId") Long commentId);

    // Complex Selects
    List<CommentDto.Response> findAllByPostIdWithDetail(@Param("postId") Long postId, @Param("currentUserId") Long currentUserId);
}