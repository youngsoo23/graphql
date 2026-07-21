package com.study.graphql.post.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PostTest {

    @Nested
    @DisplayName("Post.create")
    class Create {

        @Test
        @DisplayName("유효한 값이면 새 게시글을 생성한다")
        void createsPost() {
            Post post = Post.create("제목", "본문", "youngsoo");

            assertThat(post.getId()).isNull();
            assertThat(post.getTitle()).isEqualTo("제목");
            assertThat(post.getContent()).isEqualTo("본문");
            assertThat(post.getAuthor()).isEqualTo("youngsoo");
            assertThat(post.getCreatedAt()).isEqualTo(post.getUpdatedAt());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("제목이 비어 있으면 예외가 발생한다")
        void rejectsBlankTitle(String blankTitle) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Post.create(blankTitle, "본문", "youngsoo"));
        }

        @Test
        @DisplayName("제목이 200자를 초과하면 예외가 발생한다")
        void rejectsTooLongTitle() {
            String tooLong = "a".repeat(201);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Post.create(tooLong, "본문", "youngsoo"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("본문이 비어 있으면 예외가 발생한다")
        void rejectsBlankContent(String blankContent) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Post.create("제목", blankContent, "youngsoo"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("작성자가 비어 있으면 예외가 발생한다")
        void rejectsBlankAuthor(String blankAuthor) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Post.create("제목", "본문", blankAuthor));
        }
    }

    @Nested
    @DisplayName("Post.update")
    class Update {

        @Test
        @DisplayName("제목과 본문을 갱신하고 updatedAt을 새로 찍는다")
        void updatesTitleAndContent() throws InterruptedException {
            Post post = Post.create("제목", "본문", "youngsoo");
            var originalUpdatedAt = post.getUpdatedAt();
            Thread.sleep(10);

            post.update("새 제목", "새 본문");

            assertThat(post.getTitle()).isEqualTo("새 제목");
            assertThat(post.getContent()).isEqualTo("새 본문");
            assertThat(post.getUpdatedAt()).isAfter(originalUpdatedAt);
            assertThat(post.getAuthor()).isEqualTo("youngsoo");
        }

        @Test
        @DisplayName("빈 제목으로 수정하면 예외가 발생한다")
        void rejectsBlankTitleOnUpdate() {
            Post post = Post.create("제목", "본문", "youngsoo");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> post.update("", "새 본문"));
        }
    }

    @Nested
    @DisplayName("Post.assignId")
    class AssignId {

        @Test
        @DisplayName("id가 부여된 새 인스턴스를 반환하고 나머지 필드는 유지한다")
        void assignsId() {
            Post post = Post.create("제목", "본문", "youngsoo");

            Post persisted = post.assignId(1L);

            assertThat(persisted.getId()).isEqualTo(1L);
            assertThat(persisted.getTitle()).isEqualTo(post.getTitle());
            assertThat(post.getId()).isNull();
        }
    }
}
