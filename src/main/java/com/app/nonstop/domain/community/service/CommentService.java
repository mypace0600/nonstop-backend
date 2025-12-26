package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.CommentRequestDto;
import com.app.nonstop.domain.community.dto.CommentResponseDto;
import com.app.nonstop.domain.community.entity.Comment;
import com.app.nonstop.domain.community.entity.CommentType;
import com.app.nonstop.domain.community.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponseDto createComment(Long postId, Long userId, CommentRequestDto requestDto) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(requestDto.getContent());
        comment.setIsAnonymous(requestDto.getIsAnonymous());
        
        // 대댓글 처리
        if (requestDto.getUpperCommentId() != null) {
            Comment parent = commentMapper.findById(requestDto.getUpperCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            
            comment.setUpperCommentId(requestDto.getUpperCommentId());
            comment.setDepth(parent.getDepth() + 1);
            comment.setType(CommentType.REPLY);
        } else {
            comment.setDepth(0);
            comment.setType(CommentType.COMMENT);
        }

        commentMapper.insert(comment);

        // 생성 후엔 트리가 아닌 단일 객체만 반환하거나, 리스트를 다시 조회해야 함.
        // 여기선 간단히 빌더로 응답 생성 (실제 닉네임 조회 등이 필요하면 Mapper 조회가 좋음)
        // 편의상 재조회 없이 기본 정보만 반환하거나, 필요 시 Mapper select 호출
        return CommentResponseDto.builder()
                .id(comment.getId())
                .postId(postId)
                .content(comment.getContent())
                .depth(comment.getDepth())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId, Long userId) {
        List<CommentResponseDto> rawList = commentMapper.findAllByPostIdWithDetail(postId, userId);
        
        // 1. 마스킹 처리
        rawList.forEach(this::maskComment);

        // 2. 계층 구조 조립 (Map 활용)
        Map<Long, CommentResponseDto> map = rawList.stream()
                .collect(Collectors.toMap(CommentResponseDto::getId, Function.identity()));
        
        List<CommentResponseDto> roots = new ArrayList<>();

        for (CommentResponseDto dto : rawList) {
            if (dto.getUpperCommentId() == null) {
                roots.add(dto);
            } else {
                CommentResponseDto parent = map.get(dto.getUpperCommentId());
                if (parent != null) {
                    parent.getReplies().add(dto);
                } else {
                    // 부모가 없는데 자식이 있는 경우 (데이터 무결성 깨짐 or 부모가 Hard Delete 됨)
                    // 일단 루트로 취급하거나 버림. 여기선 루트로 취급
                    roots.add(dto);
                }
            }
        }
        
        return roots;
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        commentMapper.delete(commentId);
    }

    @Transactional
    public void toggleLike(Long commentId, Long userId) {
        int exists = commentMapper.existsLike(userId, commentId);
        if (exists > 0) {
            boolean isLiked = commentMapper.isLiked(userId, commentId);
            if (isLiked) {
                commentMapper.deleteLike(userId, commentId);
            } else {
                commentMapper.restoreLike(userId, commentId);
            }
        } else {
            commentMapper.insertLike(userId, commentId);
        }
    }

    private void maskComment(CommentResponseDto dto) {
        if (Boolean.TRUE.equals(dto.getIsDeleted())) {
            dto.setContent("삭제된 댓글입니다.");
            dto.setWriterNickname("(알수없음)");
        } else if (Boolean.TRUE.equals(dto.getIsWriterAnonymous())) {
            dto.setWriterNickname("익명");
        }
    }
}
