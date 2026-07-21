package com.study.graphql.post.application.port.in;

public record CreatePostCommand(String title, String content, String author) {
}
