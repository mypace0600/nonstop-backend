package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.PostRequestDto;
import com.app.nonstop.domain.community.dto.PostResponseDto;
import com.app.nonstop.domain.community.entity.Post;
import com.app.nonstop.domain.community.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;

    @Transactional
    public PostResponseDto createPost(Long boardId, Long userId, PostRequestDto requestDto) {
        Post post = new Post();
        post.setBoardId(boardId);
        post.setUserId(userId);
        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());
        post.setIsAnonymous(requestDto.getIsAnonymous());
        post.setIsSecret(requestDto.getIsSecret());
        
        postMapper.insert(post);
        
        // 작성 직후 상세 정보 반환
        return postMapper.findByIdWithDetail(post.getId(), userId)
                .map(this::maskPost)
                .orElseThrow(() -> new RuntimeException("Post creation failed"));
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPostDetail(Long postId, Long userId) {
        // 조회수 증가 (본인이 아닐 경우에만? 정책에 따라 다름. 여기선 일단 무조건 증가)
        postMapper.incrementViewCount(postId);

        PostResponseDto post = postMapper.findByIdWithDetail(postId, userId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        return maskPost(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getPostList(Long boardId, int page, int size, Long userId) {
        int offset = (page - 1) * size;
        List<PostResponseDto> posts = postMapper.findAllByBoardIdWithDetail(boardId, size, offset, userId);
        
        return posts.stream()
                .map(this::maskPost)
                .collect(Collectors.toList());
    }

    @Transactional
    public PostResponseDto updatePost(Long postId, Long userId, PostRequestDto requestDto) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
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

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        postMapper.delete(postId);
    }

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

    private PostResponseDto maskPost(PostResponseDto dto) {
        if (Boolean.TRUE.equals(dto.getIsWriterAnonymous())) {
            dto.setWriterNickname("익명");
        }
        return dto;
    }
}
