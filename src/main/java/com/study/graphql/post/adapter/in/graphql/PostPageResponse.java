package com.study.graphql.post.adapter.in.graphql;

import com.study.graphql.post.application.port.in.PostPage;

import java.util.List;

record PostPageResponse(List<PostResponse> content, int page, int size, long totalElements, int totalPages) {

    static PostPageResponse from(PostPage page) {
        List<PostResponse> content = page.content().stream().map(PostResponse::from).toList();
        return new PostPageResponse(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
