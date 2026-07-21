package com.study.graphql.post.application.service;

import com.study.graphql.post.application.port.in.PostPage;
import com.study.graphql.post.application.port.out.LoadPostPort;
import com.study.graphql.post.domain.Post;
import com.study.graphql.post.domain.PostNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private LoadPostPort loadPostPort;

    private PostQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new PostQueryService(loadPostPort);
    }

    @Test
    @DisplayName("getPost는 존재하는 게시글을 반환한다")
    void getPost() {
        Post post = Post.create("제목", "본문", "youngsoo").assignId(1L);
        when(loadPostPort.loadPost(1L)).thenReturn(Optional.of(post));

        Post result = sut.getPost(1L);

        assertThat(result).isEqualTo(post);
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 PostNotFoundException이 발생한다")
    void getPostNotFound() {
        when(loadPostPort.loadPost(999L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(PostNotFoundException.class)
                .isThrownBy(() -> sut.getPost(999L));
    }

    @Test
    @DisplayName("getPosts는 LoadPostPort의 페이지 결과를 그대로 전달한다")
    void getPosts() {
        Post post = Post.create("제목", "본문", "youngsoo").assignId(1L);
        PostPage page = new PostPage(List.of(post), 0, 10, 1L, 1);
        when(loadPostPort.loadPosts(0, 10)).thenReturn(page);

        PostPage result = sut.getPosts(0, 10);

        assertThat(result).isEqualTo(page);
    }
}
