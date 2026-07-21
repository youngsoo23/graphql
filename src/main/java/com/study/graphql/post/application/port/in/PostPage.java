package com.study.graphql.post.application.port.in;

import com.study.graphql.post.domain.Post;

import java.util.List;

/** Read-model shape shared by the inbound query port and the outbound load port. */
public record PostPage(List<Post> content, int page, int size, long totalElements, int totalPages) {
}
