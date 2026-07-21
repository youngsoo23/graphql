package com.study.graphql.post.adapter.in.graphql;

import com.study.graphql.post.application.port.in.CreatePostCommand;
import com.study.graphql.post.application.port.in.CreatePostUseCase;
import com.study.graphql.post.application.port.in.DeletePostUseCase;
import com.study.graphql.post.application.port.in.GetPostListQuery;
import com.study.graphql.post.application.port.in.GetPostQuery;
import com.study.graphql.post.application.port.in.UpdatePostCommand;
import com.study.graphql.post.application.port.in.UpdatePostUseCase;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class PostGraphqlController {

    private final CreatePostUseCase createPostUseCase;
    private final UpdatePostUseCase updatePostUseCase;
    private final DeletePostUseCase deletePostUseCase;
    private final GetPostQuery getPostQuery;
    private final GetPostListQuery getPostListQuery;

    PostGraphqlController(CreatePostUseCase createPostUseCase,
                           UpdatePostUseCase updatePostUseCase,
                           DeletePostUseCase deletePostUseCase,
                           GetPostQuery getPostQuery,
                           GetPostListQuery getPostListQuery) {
        this.createPostUseCase = createPostUseCase;
        this.updatePostUseCase = updatePostUseCase;
        this.deletePostUseCase = deletePostUseCase;
        this.getPostQuery = getPostQuery;
        this.getPostListQuery = getPostListQuery;
    }

    @QueryMapping
    PostResponse post(@Argument Long id) {
        return PostResponse.from(getPostQuery.getPost(id));
    }

    @QueryMapping
    PostPageResponse posts(@Argument Integer page, @Argument Integer size) {
        return PostPageResponse.from(getPostListQuery.getPosts(
                page != null ? page : 0,
                size != null ? size : 10));
    }

    @MutationMapping
    PostResponse createPost(@Argument CreatePostInput input) {
        CreatePostCommand command = new CreatePostCommand(input.title(), input.content(), input.author());
        return PostResponse.from(createPostUseCase.createPost(command));
    }

    @MutationMapping
    PostResponse updatePost(@Argument Long id, @Argument UpdatePostInput input) {
        UpdatePostCommand command = new UpdatePostCommand(id, input.title(), input.content());
        return PostResponse.from(updatePostUseCase.updatePost(command));
    }

    @MutationMapping
    boolean deletePost(@Argument Long id) {
        deletePostUseCase.deletePost(id);
        return true;
    }
}
