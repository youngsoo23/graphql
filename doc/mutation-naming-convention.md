# Mutation 네이밍 컨벤션

## GraphQL은 CRUD 동사를 강제하지 않는다

`createX`/`updateX`/`deleteX`는 GraphQL 스펙이 강제하는 규칙이 아니라 관례 중
하나다. 실제로는 도메인 행위(action)를 그대로 드러내는 이름을 쓰는 것도 흔하다.

```graphql
type Mutation {
  publishPost(id: ID!): Post!        # 단순 update가 아니라 "발행"이라는 의미
  archivePost(id: ID!): Post!        # 삭제가 아니라 "보관"
  likePost(id: ID!): Post!           # 좋아요 누르기
  addCommentToPost(input: AddCommentInput!): Comment!
  changePostAuthor(id: ID!, newAuthorId: ID!): Post!
}
```

이 프로젝트는 스키마가 딱 "게시글 CRUD"라서 가장 직관적인 `createPost`/
`updatePost`/`deletePost`를 골랐을 뿐이다. `updatePost`가 제목 변경, 발행 상태
변경처럼 여러 책임을 떠안기 시작하면, 하나의 뮤테이션 대신 `changePostTitle`,
`publishPost`처럼 의도가 드러나는 단위로 쪼개는 걸 고려한다.

## 그런데 왜 REST에서는 X(리소스명)를 빼는 컨벤션이 흔한가

REST는 **URL 경로 자체가 리소스를 특정**한다. `PostController` 안에 있는
핸들러 메서드는 이미 클래스/경로가 "Post"라는 문맥을 제공하므로, 메서드 이름에서
`Post`를 반복할 필요가 없다.

```java
@RestController
@RequestMapping("/posts")
class PostController {
    @PostMapping        create(...)      // POST /posts
    @PutMapping("/{id}") update(...)     // PUT /posts/{id}
    @DeleteMapping("/{id}") delete(...)  // DELETE /posts/{id}
}
```

`create`/`update`/`delete`라는 이름이 다른 `CommentController`, `CategoryController`
안의 동명 메서드와 이름이 겹쳐도 전혀 문제 없다 — 각자 다른 클래스(=다른
네임스페이스) 안에 있고, HTTP 메서드 + URL 경로가 실제 식별자이기 때문이다.

## GraphQL은 왜 X를 뺄 수 없는가

GraphQL은 모든 뮤테이션이 **하나의 평평한 `Mutation` 타입 아래 필드로 공존**한다.
클래스 경계나 URL 경로 같은 자연스러운 네임스페이스가 없다.

```graphql
type Mutation {
  create(input: ...): Post!       # Post용 create
  create(input: ...): Comment!    # Comment용 create -> 이름 충돌!
}
```

즉 스키마 전체에서 필드 이름이 유일해야 하므로, 리소스명을 이름 안에 넣어
구분할 수밖에 없다(`createPost`, `createComment`, `createCategory`). Java 쪽에서
`@MutationMapping` 메서드는 각자 다른 컨트롤러 클래스(`PostGraphqlController`,
`CommentGraphqlController`)에 나눠 둘 수 있어 메서드명 충돌은 안 나지만, 스키마
필드명은 반드시 전역으로 유일해야 하는 게 핵심 차이다.

## 정리

| | REST | GraphQL |
|---|---|---|
| 네임스페이스 단위 | URL 경로 + HTTP 메서드 | 없음 (Mutation 타입 하나로 평평함) |
| 이름 충돌 방지 | 클래스/경로가 자동으로 구분 | 필드 이름 자체에 리소스명을 넣어야 함 |
| 흔한 컨벤션 | `create`/`update`/`delete` (X 생략 가능) | `createX`/`updateX`/`deleteX` 또는 도메인 행위 동사 |

REST에서 X를 빼는 컨벤션은 "경로가 이미 문맥을 주기 때문"이고, GraphQL에서
X를 넣는(혹은 의미 있는 동사를 쓰는) 컨벤션은 "전역에서 유일해야 하기 때문"이다.
같은 목표(중복 없는 명확한 이름)를 서로 다른 구조적 제약 아래서 달성하는 것뿐,
어느 한쪽이 더 옳은 컨벤션은 아니다.
