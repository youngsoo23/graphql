package com.study.graphql.post.application.port.in;

public record UpdatePostCommand(Long id, String title, String content) {
}
