package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.CommentDto;
import com.app.nonstop.domain.community.entity.Comment;
import com.app.nonstop.domain.community.entity.CommentType;
import com.app.nonstop.domain.community.mapper.CommentMapper;
import com.app.nonstop.global.common.exception.AccessDeniedException;
import com.app.nonstop.global.common.exception.BusinessException;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 댓글 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * 댓글 작성, 삭제, 조회 및 좋아요 기능을 담당하며, 계층형(트리) 구조 조립 로직을 포함합니다.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    /**
     * 댓글 또는 대댓글을 작성합니다.
     * PRD 요구사항: 대댓글은 최대 1 depth까지만 허용됩니다. (원글 -> 댓글 -> 대댓글 가능, 대대댓글 불가능)
     *
     * @param postId     게시글 ID
     * @param userId     작성자 ID
     * @param requestDto 댓글 작성 요청 데이터
     * @return 작성된 댓글 정보
     * @throws RuntimeException 부모 댓글을 찾을 수 없거나, 대댓글 깊이 제한을 초과한 경우
     */
    @Transactional
    public CommentDto.Response createComment(Long postId, Long userId, CommentDto.Request requestDto) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(requestDto.getContent());
        comment.setIsAnonymous(requestDto.getIsAnonymous());
        
        // 대댓글 처리
        if (requestDto.getUpperCommentId() != null) {
            Comment parent = commentMapper.findById(requestDto.getUpperCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("부모 댓글을 찾을 수 없습니다."));
            
            // Depth 제한 체크: 부모가 이미 대댓글(depth >= 1)이면 더 이상 하위 댓글 작성 불가
            if (parent.getDepth() >= 1) {
                throw new BusinessException("대댓글에는 더 이상 댓글을 달 수 없습니다.");
            }

            comment.setUpperCommentId(requestDto.getUpperCommentId());
            comment.setDepth(parent.getDepth() + 1);
            comment.setType(CommentType.REPLY);
        } else {
            comment.setDepth(0);
            comment.setType(CommentType.COMMENT);
        }

        commentMapper.insert(comment);

        // 생성 후엔 트리가 아닌 단일 객체만 반환하거나, 리스트를 다시 조회해야 함.
        // 여기선 간단히 빌더로 응답 생성
        return CommentDto.Response.builder()
                .id(comment.getId())
                .postId(postId)
                .content(comment.getContent())
                .depth(comment.getDepth())
                .build();
    }

    /**
     * 게시글의 전체 댓글 목록을 계층형 구조(트리)로 조회합니다.
     *
     * @param postId 게시글 ID
     * @param userId 현재 조회하는 사용자 ID
     * @return 계층 구조로 조립된 댓글 리스트
     */
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getComments(Long postId, Long userId) {
        List<CommentDto.Response> rawList = commentMapper.findAllByPostIdWithDetail(postId, userId);
        
        // 1. 마스킹 처리 (삭제된 댓글 내용 숨김, 익명 처리)
        rawList.forEach(this::maskComment);

        // 2. 계층 구조 조립 (Map 활용하여 O(N) 처리)
        Map<Long, CommentDto.Response> map = rawList.stream()
                .collect(Collectors.toMap(CommentDto.Response::getId, Function.identity()));
        
        List<CommentDto.Response> roots = new ArrayList<>();

        for (CommentDto.Response dto : rawList) {
            if (dto.getUpperCommentId() == null) {
                roots.add(dto);
            } else {
                CommentDto.Response parent = map.get(dto.getUpperCommentId());
                if (parent != null) {
                    parent.getReplies().add(dto);
                } else {
                    // 부모가 없는데 자식이 있는 경우 (데이터 무결성 깨짐 or 부모가 Hard Delete 됨)
                    // 현재 로직상 부모가 Soft Delete 되어도 조회되므로 여기 걸릴 일은 드뭄.
                    // 만약 발생한다면 Root로 취급하여 노출시킴.
                    roots.add(dto);
                }
            }
        }
        
        return roots;
    }

    /**
     * 댓글을 삭제(Soft Delete)합니다.
     * 작성자 본인만 삭제 가능합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    요청 사용자 ID
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다."));
        
        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }
        
        commentMapper.delete(commentId);
    }

    /**
     * 댓글 좋아요를 토글(좋아요 <-> 취소)합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    요청 사용자 ID
     */
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

    /**
     * 댓글 내용 마스킹 처리.
     * 삭제된 댓글은 내용을 숨기고, 익명 댓글은 닉네임을 숨깁니다.
     *
     * @param dto 댓글 응답 DTO
     */
    private void maskComment(CommentDto.Response dto) {
        if (Boolean.TRUE.equals(dto.getIsDeleted())) {
            dto.setContent("삭제된 댓글입니다.");
            dto.setWriterNickname("(알수없음)");
        } else if (Boolean.TRUE.equals(dto.getIsWriterAnonymous())) {
            dto.setWriterNickname("익명");
        }
    }
}
