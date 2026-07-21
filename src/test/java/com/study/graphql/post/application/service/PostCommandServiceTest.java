package com.study.graphql.post.application.service;

import com.study.graphql.post.application.port.in.CreatePostCommand;
import com.study.graphql.post.application.port.in.UpdatePostCommand;
import com.study.graphql.post.application.port.out.DeletePostPort;
import com.study.graphql.post.application.port.out.LoadPostPort;
import com.study.graphql.post.application.port.out.SavePostPort;
import com.study.graphql.post.domain.Post;
import com.study.graphql.post.domain.PostNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock
    private LoadPostPort loadPostPort;

    @Mock
    private SavePostPort savePostPort;

    @Mock
    private DeletePostPort deletePostPort;

    private PostCommandService sut;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sut = new PostCommandService(loadPostPort, savePostPort, deletePostPort);
    }

    @Test
    @DisplayName("createPost는 도메인 검증을 거친 새 Post를 SavePostPort에 위임한다")
    void createPost() {
        CreatePostCommand command = new CreatePostCommand("제목", "본문", "youngsoo");
        Post saved = Post.create("제목", "본문", "youngsoo").assignId(1L);
        when(savePostPort.savePost(any(Post.class))).thenReturn(saved);

        Post result = sut.createPost(command);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(savePostPort).savePost(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updatePost는 기존 게시글을 불러와 수정한 뒤 저장한다")
    void updatePost() {
        Post existing = Post.create("옛 제목", "옛 본문", "youngsoo").assignId(1L);
        when(loadPostPort.loadPost(1L)).thenReturn(Optional.of(existing));
        when(savePostPort.savePost(existing)).thenReturn(existing);

        Post result = sut.updatePost(new UpdatePostCommand(1L, "새 제목", "새 본문"));

        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getContent()).isEqualTo("새 본문");
        verify(savePostPort).savePost(existing);
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 수정하려 하면 PostNotFoundException이 발생한다")
    void updatePostNotFound() {
        when(loadPostPort.loadPost(999L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(PostNotFoundException.class)
                .isThrownBy(() -> sut.updatePost(new UpdatePostCommand(999L, "제목", "본문")));
        verify(savePostPort, never()).savePost(any());
    }

    @Test
    @DisplayName("deletePost는 존재를 확인한 뒤 DeletePostPort에 위임한다")
    void deletePost() {
        when(loadPostPort.existsPost(1L)).thenReturn(true);

        sut.deletePost(1L);

        verify(deletePostPort).deletePost(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 삭제하려 하면 PostNotFoundException이 발생하고 삭제는 호출되지 않는다")
    void deletePostNotFound() {
        when(loadPostPort.existsPost(999L)).thenReturn(false);

        assertThatExceptionOfType(PostNotFoundException.class)
                .isThrownBy(() -> sut.deletePost(999L));
        verify(deletePostPort, never()).deletePost(eq(999L));
    }
}
