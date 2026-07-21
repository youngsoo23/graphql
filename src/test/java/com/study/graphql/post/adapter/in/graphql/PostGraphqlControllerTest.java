package com.study.graphql.post.adapter.in.graphql;

import com.study.graphql.post.application.port.in.CreatePostCommand;
import com.study.graphql.post.application.port.in.CreatePostUseCase;
import com.study.graphql.post.application.port.in.DeletePostUseCase;
import com.study.graphql.post.application.port.in.GetPostListQuery;
import com.study.graphql.post.application.port.in.GetPostQuery;
import com.study.graphql.post.application.port.in.PostPage;
import com.study.graphql.post.application.port.in.UpdatePostCommand;
import com.study.graphql.post.application.port.in.UpdatePostUseCase;
import com.study.graphql.post.domain.Post;
import com.study.graphql.post.domain.PostNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@GraphQlTest({PostGraphqlController.class, PostGraphqlExceptionHandler.class})
class PostGraphqlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private CreatePostUseCase createPostUseCase;

    @MockitoBean
    private UpdatePostUseCase updatePostUseCase;

    @MockitoBean
    private DeletePostUseCase deletePostUseCase;

    @MockitoBean
    private GetPostQuery getPostQuery;

    @MockitoBean
    private GetPostListQuery getPostListQuery;

    @Test
    @DisplayName("post 쿼리는 id로 조회한 게시글을 반환한다")
    void post() {
        Post post = Post.create("제목", "본문", "youngsoo").assignId(1L);
        when(getPostQuery.getPost(1L)).thenReturn(post);

        graphQlTester.document("query { post(id: \"1\") { id title content author } }")
                .execute()
                .path("post.id").entity(String.class).isEqualTo("1")
                .path("post.title").entity(String.class).isEqualTo("제목")
                .path("post.author").entity(String.class).isEqualTo("youngsoo");
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 NOT_FOUND 에러가 반환된다")
    void postNotFound() {
        when(getPostQuery.getPost(999L)).thenThrow(new PostNotFoundException(999L));

        graphQlTester.document("query { post(id: \"999\") { id } }")
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getErrorType()).hasToString("NOT_FOUND");
                });
    }

    @Test
    @DisplayName("posts 쿼리는 페이지 결과를 반환한다")
    void posts() {
        Post post = Post.create("제목", "본문", "youngsoo").assignId(1L);
        when(getPostListQuery.getPosts(0, 10)).thenReturn(new PostPage(List.of(post), 0, 10, 1L, 1));

        graphQlTester.document("query { posts(page: 0, size: 10) { content { id title } totalElements totalPages } }")
                .execute()
                .path("posts.content").entityList(Object.class).hasSize(1)
                .path("posts.totalElements").entity(Long.class).isEqualTo(1L);
    }

    @Test
    @DisplayName("createPost 뮤테이션은 입력값으로 유스케이스를 호출하고 결과를 반환한다")
    void createPost() {
        Post created = Post.create("새 글", "새 내용", "youngsoo").assignId(1L);
        when(createPostUseCase.createPost(any(CreatePostCommand.class))).thenReturn(created);

        graphQlTester.document("""
                        mutation {
                            createPost(input: { title: "새 글", content: "새 내용", author: "youngsoo" }) {
                                id title content author
                            }
                        }
                        """)
                .execute()
                .path("createPost.title").entity(String.class).isEqualTo("새 글");

        verify(createPostUseCase).createPost(new CreatePostCommand("새 글", "새 내용", "youngsoo"));
    }

    @Test
    @DisplayName("updatePost 뮤테이션은 id와 입력값으로 유스케이스를 호출한다")
    void updatePost() {
        Post updated = Post.create("바뀐 제목", "바뀐 내용", "youngsoo").assignId(1L);
        when(updatePostUseCase.updatePost(any(UpdatePostCommand.class))).thenReturn(updated);

        graphQlTester.document("""
                        mutation {
                            updatePost(id: "1", input: { title: "바뀐 제목", content: "바뀐 내용" }) {
                                id title content
                            }
                        }
                        """)
                .execute()
                .path("updatePost.title").entity(String.class).isEqualTo("바뀐 제목");

        verify(updatePostUseCase).updatePost(new UpdatePostCommand(1L, "바뀐 제목", "바뀐 내용"));
    }

    @Test
    @DisplayName("deletePost 뮤테이션은 유스케이스를 호출하고 true를 반환한다")
    void deletePost() {
        graphQlTester.document("mutation { deletePost(id: \"1\") }")
                .execute()
                .path("deletePost").entity(Boolean.class).isEqualTo(true);

        verify(deletePostUseCase).deletePost(eq(1L));
    }
}
