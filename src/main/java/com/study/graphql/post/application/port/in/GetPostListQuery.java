package com.study.graphql.post.application.port.in;

public interface GetPostListQuery {

    PostPage getPosts(int page, int size);
}
