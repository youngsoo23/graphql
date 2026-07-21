package com.study.graphql.post.application.service;

import com.study.graphql.common.UseCase;
import com.study.graphql.post.application.port.in.GetPostListQuery;
import com.study.graphql.post.application.port.in.GetPostQuery;
import com.study.graphql.post.application.port.in.PostPage;
import com.study.graphql.post.application.port.out.LoadPostPort;
import com.study.graphql.post.domain.Post;
import com.study.graphql.post.domain.PostNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional(readOnly = true)
public class PostQueryService implements GetPostQuery, GetPostListQuery {

    private final LoadPostPort loadPostPort;

    public PostQueryService(LoadPostPort loadPostPort) {
        this.loadPostPort = loadPostPort;
    }

    @Override
    public Post getPost(Long id) {
        return loadPostPort.loadPost(id)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    @Override
    public PostPage getPosts(int page, int size) {
        return loadPostPort.loadPosts(page, size);
    }
}
