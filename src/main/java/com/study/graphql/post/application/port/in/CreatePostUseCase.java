package com.study.graphql.post.application.port.in;

import com.study.graphql.post.domain.Post;

public interface CreatePostUseCase {

    Post createPost(CreatePostCommand command);
}
