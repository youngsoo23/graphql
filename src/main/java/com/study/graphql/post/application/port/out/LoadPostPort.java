package com.study.graphql.post.application.port.out;

import com.study.graphql.post.application.port.in.PostPage;
import com.study.graphql.post.domain.Post;

import java.util.Optional;

public interface LoadPostPort {

    Optional<Post> loadPost(Long id);

    PostPage loadPosts(int page, int size);

    boolean existsPost(Long id);
}
