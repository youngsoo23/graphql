package com.study.graphql.post.adapter.out.persistence;

import com.study.graphql.post.application.port.in.PostPage;
import com.study.graphql.post.domain.Post;
import com.study.graphql.post.domain.PostNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@Import(PostPersistenceAdapter.class)
class PostPersistenceAdapterTest {

    @Autowired
    private PostPersistenceAdapter sut;

    @Autowired
    private SpringDataPostRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("savePost는 새 Post를 저장하고 id가 채워진 도메인 객체를 반환한다")
    void savesNewPost() {
        Post post = Post.create("제목", "본문", "youngsoo");

        Post saved = sut.savePost(post);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("loadPost는 저장된 Post를 id로 조회한다")
    void loadsPostById() {
        Post saved = sut.savePost(Post.create("제목", "본문", "youngsoo"));

        Optional<Post> found = sut.loadPost(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("존재하지 않는 id를 조회하면 빈 Optional을 반환한다")
    void loadPostNotFound() {
        assertThat(sut.loadPost(999L)).isEmpty();
    }

    @Test
    @DisplayName("savePost는 id가 있으면 기존 Post를 갱신한다")
    void updatesExistingPost() {
        Post saved = sut.savePost(Post.create("제목", "본문", "youngsoo"));
        saved.update("새 제목", "새 본문");

        Post updated = sut.savePost(saved);

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("loadPosts는 페이지 단위로 최신순 결과를 반환한다")
    void loadsPostsWithPaging() {
        for (int i = 0; i < 3; i++) {
            sut.savePost(Post.create("제목" + i, "본문" + i, "youngsoo"));
        }

        PostPage page = sut.loadPosts(0, 2);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("existsPost는 저장된 게시글에 대해 true를 반환한다")
    void existsPost() {
        Post saved = sut.savePost(Post.create("제목", "본문", "youngsoo"));

        assertThat(sut.existsPost(saved.getId())).isTrue();
        assertThat(sut.existsPost(999L)).isFalse();
    }

    @Test
    @DisplayName("deletePost는 저장된 게시글을 삭제한다")
    void deletesPost() {
        Post saved = sut.savePost(Post.create("제목", "본문", "youngsoo"));

        sut.deletePost(saved.getId());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 id로 savePost를 갱신 모드로 호출하면 PostNotFoundException이 발생한다")
    void updateMissingPostThrows() {
        Post ghost = Post.reconstitute(999L, "제목", "본문", "youngsoo",
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());

        assertThatExceptionOfType(PostNotFoundException.class)
                .isThrownBy(() -> sut.savePost(ghost));
    }
}
