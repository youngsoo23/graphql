package com.study.graphql.post.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Post aggregate root. Framework-free on purpose: no JPA/GraphQL annotations here,
 * so business rules can be unit-tested without any Spring context.
 */
public class Post {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 10_000;

    private final Long id;
    private String title;
    private String content;
    private final String author;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Post(Long id, String title, String content, String author,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Creates a brand-new post. Never has an id yet — that's assigned on persistence. */
    public static Post create(String title, String content, String author) {
        requireValidTitle(title);
        requireValidContent(content);
        requireValidAuthor(author);
        LocalDateTime now = LocalDateTime.now();
        return new Post(null, title, content, author, now, now);
    }

    /** Rebuilds a post from persisted state. Used by outbound persistence adapters only. */
    public static Post reconstitute(Long id, String title, String content, String author,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        Objects.requireNonNull(id, "id must not be null when reconstituting a persisted post");
        return new Post(id, title, content, author, createdAt, updatedAt);
    }

    /** Returns a copy of this post carrying the id assigned by the persistence layer. */
    public Post assignId(Long newId) {
        Objects.requireNonNull(newId, "newId must not be null");
        return new Post(newId, this.title, this.content, this.author, this.createdAt, this.updatedAt);
    }

    public void update(String newTitle, String newContent) {
        requireValidTitle(newTitle);
        requireValidContent(newContent);
        this.title = newTitle;
        this.content = newContent;
        this.updatedAt = LocalDateTime.now();
    }

    private static void requireValidTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("제목은 %d자를 초과할 수 없습니다.".formatted(MAX_TITLE_LENGTH));
        }
    }

    private static void requireValidContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("본문은 비어 있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("본문은 %d자를 초과할 수 없습니다.".formatted(MAX_CONTENT_LENGTH));
        }
    }

    private static void requireValidAuthor(String author) {
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("작성자는 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post post)) return false;
        return Objects.equals(id, post.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
