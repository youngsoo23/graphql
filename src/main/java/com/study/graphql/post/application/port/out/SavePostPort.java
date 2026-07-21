package com.study.graphql.post.application.port.out;

import com.study.graphql.post.domain.Post;

public interface SavePostPort {

    Post savePost(Post post);
}
