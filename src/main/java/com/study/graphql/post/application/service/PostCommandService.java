package com.study.graphql.post.application.service;

import com.study.graphql.common.UseCase;
import com.study.graphql.post.application.port.in.CreatePostCommand;
import com.study.graphql.post.application.port.in.CreatePostUseCase;
import com.study.graphql.post.application.port.in.DeletePostUseCase;
import com.study.graphql.post.application.port.in.UpdatePostCommand;
import com.study.graphql.post.application.port.in.UpdatePostUseCase;
import com.study.graphql.post.application.port.out.DeletePostPort;
import com.study.graphql.post.application.port.out.LoadPostPort;
import com.study.graphql.post.application.port.out.SavePostPort;
import com.study.graphql.post.domain.Post;
import com.study.graphql.post.domain.PostNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class PostCommandService implements CreatePostUseCase, UpdatePostUseCase, DeletePostUseCase {

    private final LoadPostPort loadPostPort;
    private final SavePostPort savePostPort;
    private final DeletePostPort deletePostPort;

    public PostCommandService(LoadPostPort loadPostPort, SavePostPort savePostPort, DeletePostPort deletePostPort) {
        this.loadPostPort = loadPostPort;
        this.savePostPort = savePostPort;
        this.deletePostPort = deletePostPort;
    }

    @Override
    @Transactional
    public Post createPost(CreatePostCommand command) {
        Post post = Post.create(command.title(), command.content(), command.author());
        return savePostPort.savePost(post);
    }

    @Override
    @Transactional
    public Post updatePost(UpdatePostCommand command) {
        Post post = loadPostPort.loadPost(command.id())
                .orElseThrow(() -> new PostNotFoundException(command.id()));
        post.update(command.title(), command.content());
        return savePostPort.savePost(post);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        if (!loadPostPort.existsPost(id)) {
            throw new PostNotFoundException(id);
        }
        deletePostPort.deletePost(id);
    }
}
