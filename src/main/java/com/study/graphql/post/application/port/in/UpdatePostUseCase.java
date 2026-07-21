package com.study.graphql.post.application.port.in;

import com.study.graphql.post.domain.Post;

public interface UpdatePostUseCase {

    Post updatePost(UpdatePostCommand command);
}
