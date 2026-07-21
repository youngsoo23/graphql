package com.study.graphql.post.application.port.in;

import com.study.graphql.post.domain.Post;

public interface GetPostQuery {

    Post getPost(Long id);
}
