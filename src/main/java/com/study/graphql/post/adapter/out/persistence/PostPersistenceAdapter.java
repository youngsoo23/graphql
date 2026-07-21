package com.study.graphql.post.adapter.out.persistence;

import com.study.graphql.post.application.port.in.PostPage;
import com.study.graphql.post.application.port.out.DeletePostPort;
import com.study.graphql.post.application.port.out.LoadPostPort;
import com.study.graphql.post.application.port.out.SavePostPort;
import com.study.graphql.post.domain.Post;
import com.study.graphql.post.domain.PostNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class PostPersistenceAdapter implements LoadPostPort, SavePostPort, DeletePostPort {

    private final SpringDataPostRepository repository;

    PostPersistenceAdapter(SpringDataPostRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Post> loadPost(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public PostPage loadPosts(int page, int size) {
        Page<PostJpaEntity> result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new PostPage(
                result.getContent().stream().map(this::toDomain).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public boolean existsPost(Long id) {
        return repository.existsById(id);
    }

    @Override
    public Post savePost(Post post) {
        PostJpaEntity entity = post.getId() == null
                ? new PostJpaEntity(null, post.getTitle(), post.getContent(), post.getAuthor(),
                        post.getCreatedAt(), post.getUpdatedAt())
                : repository.findById(post.getId())
                        .orElseThrow(() -> new PostNotFoundException(post.getId()));

        if (post.getId() != null) {
            entity.setTitle(post.getTitle());
            entity.setContent(post.getContent());
            entity.setUpdatedAt(post.getUpdatedAt());
        }

        PostJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deletePost(Long id) {
        repository.deleteById(id);
    }

    private Post toDomain(PostJpaEntity entity) {
        return Post.reconstitute(entity.getId(), entity.getTitle(), entity.getContent(),
                entity.getAuthor(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
