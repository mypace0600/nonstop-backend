package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.PostDto;
import com.app.nonstop.domain.community.entity.Post;
import com.app.nonstop.domain.community.mapper.PostMapper;
import com.app.nonstop.global.common.exception.AccessDeniedException;
import com.app.nonstop.global.common.exception.BusinessException;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import com.app.nonstop.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시글 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * 게시글 작성, 수정, 삭제, 조회 및 좋아요 기능을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;

    /**
     * 새로운 게시글을 작성합니다.
     * PRD 요구사항: 대학 인증이 완료된(universityId가 존재하는) 사용자만 작성할 수 있습니다.
     *
     * @param boardId     게시판 ID
     * @param userDetails 작성자 인증 정보
     * @param requestDto  게시글 작성 요청 데이터
     * @return 작성된 게시글 상세 정보
     * @throws RuntimeException 대학 인증이 되지 않은 경우
     */
    @Transactional
    public PostDto.Response createPost(Long boardId, CustomUserDetails userDetails, PostDto.Request requestDto) {
        // 대학 인증 체크 (Graceful Degradation)
        if (userDetails.getUniversityId() == null) {
            throw new BusinessException("게시글 작성을 위해서는 대학 인증이 필요합니다.");
        }

        Post post = new Post();
        post.setBoardId(boardId);
        post.setUserId(userDetails.getUserId());
        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());
        post.setIsAnonymous(requestDto.getIsAnonymous());
        post.setIsSecret(requestDto.getIsSecret());
        
        postMapper.insert(post);
        
        // 작성 직후 상세 정보 반환
        return postMapper.findByIdWithDetail(post.getId(), userDetails.getUserId())
                .map(this::maskPost)
                .orElseThrow(() -> new BusinessException("Post creation failed"));
    }

    /**
     * 게시글 상세 정보를 조회합니다.
     * 조회수 증가 로직이 포함되어 있습니다.
     *
     * @param postId 게시글 ID
     * @param userId 현재 조회하는 사용자 ID (좋아요 여부 확인용, 비로그인 시 null)
     * @return 게시글 상세 정보
     */
    @Transactional(readOnly = true)
    public PostDto.Response getPostDetail(Long postId, Long userId) {
        // TODO: 조회수 중복 증가 방지 로직 (Redis, Cookie 등) 고려 필요. 현재는 호출 시 무조건 증가.
        postMapper.incrementViewCount(postId);

        PostDto.Response post = postMapper.findByIdWithDetail(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        return maskPost(post);
    }

    /**
     * 게시판의 게시글 목록을 페이징하여 조회합니다.
     *
     * @param boardId 게시판 ID
     * @param page    페이지 번호 (1부터 시작)
     * @param size    페이지 크기
     * @param userId  현재 조회하는 사용자 ID
     * @return 게시글 목록 리스트
     */
    @Transactional(readOnly = true)
    public List<PostDto.Response> getPostList(Long boardId, int page, int size, Long userId) {
        int offset = (page - 1) * size;
        List<PostDto.Response> posts = postMapper.findAllByBoardIdWithDetail(boardId, size, offset, userId);
        
        return posts.stream()
                .map(this::maskPost)
                .collect(Collectors.toList());
    }

    /**
     * 게시글을 수정합니다.
     * 작성자 본인만 수정 가능합니다.
     *
     * @param postId     게시글 ID
     * @param userId     요청 사용자 ID
     * @param requestDto 수정할 데이터
     * @return 수정된 게시글 상세 정보
     */
    @Transactional
    public PostDto.Response updatePost(Long postId, Long userId, PostDto.Request requestDto) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());
        post.setIsAnonymous(requestDto.getIsAnonymous());
        post.setIsSecret(requestDto.getIsSecret());

        postMapper.update(post);

        return postMapper.findByIdWithDetail(postId, userId)
                .map(this::maskPost)
                .orElseThrow(() -> new RuntimeException("Post update failed"));
    }

    /**
     * 게시글을 삭제(Soft Delete)합니다.
     * 작성자 본인만 삭제 가능합니다.
     *
     * @param postId 게시글 ID
     * @param userId 요청 사용자 ID
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new AccessDeniedException("작성자만 삭제할 수 있습니다.");
        }

        postMapper.delete(postId);
    }

    /**
     * 게시글 좋아요를 토글(좋아요 <-> 취소)합니다.
     *
     * @param postId 게시글 ID
     * @param userId 요청 사용자 ID
     */
    @Transactional
    public void toggleLike(Long postId, Long userId) {
        int exists = postMapper.existsLike(userId, postId);
        if (exists > 0) {
            boolean isLiked = postMapper.isLiked(userId, postId);
            if (isLiked) {
                postMapper.deleteLike(userId, postId);
            } else {
                postMapper.restoreLike(userId, postId);
            }
        } else {
            postMapper.insertLike(userId, postId);
        }
    }

    /**
     * 익명 작성자인 경우 닉네임을 마스킹 처리합니다.
     *
     * @param dto 게시글 응답 DTO
     * @return 마스킹 처리된 DTO
     */
    private PostDto.Response maskPost(PostDto.Response dto) {
        if (Boolean.TRUE.equals(dto.getIsWriterAnonymous())) {
            dto.setWriterNickname("익명");
        }
        return dto;
    }
}
