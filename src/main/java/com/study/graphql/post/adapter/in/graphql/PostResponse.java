package com.study.graphql.post.adapter.in.graphql;

import com.study.graphql.post.domain.Post;

import java.time.format.DateTimeFormatter;

record PostResponse(String id, String title, String content, String author, String createdAt, String updatedAt) {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    static PostResponse from(Post post) {
        return new PostResponse(
                String.valueOf(post.getId()),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt().format(TIMESTAMP_FORMAT),
                post.getUpdatedAt().format(TIMESTAMP_FORMAT));
    }
}
