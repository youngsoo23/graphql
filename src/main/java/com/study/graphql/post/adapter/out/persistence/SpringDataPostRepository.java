package com.study.graphql.post.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPostRepository extends JpaRepository<PostJpaEntity, Long> {
}
