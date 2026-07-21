# graphql — 게시판 CRUD 예제

Spring Boot 기반 GraphQL 게시판(Post) CRUD 예제입니다. **포트 & 어댑터(헥사고날) 아키텍처**,
**DDD**, **TDD**를 적용해 GraphQL을 처음 접하는 사람도 계층 구조와 테스트 전략을 함께
볼 수 있도록 만들었습니다.

아키텍처/컨벤션에 대한 자세한 설명은 [CLAUDE.md](./CLAUDE.md)를 참고하세요.

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어/빌드 | Java 24 (toolchain), Gradle |
| 프레임워크 | Spring Boot 4.1.0 / Spring Framework 7.0.8 |
| API | Spring GraphQL 2.0.4 (graphql-java 25) |
| 영속성 | Spring Data JPA, H2 (in-memory) |
| 테스트 | JUnit 5, Mockito, AssertJ, `@DataJpaTest`, `@GraphQlTest` |

## 아키텍처

`com.study.graphql.post` 패키지 하나가 게시글 bounded context입니다.

```
post/
├── domain/                 # 순수 도메인 (Post 애그리거트, 검증 규칙, 도메인 예외)
├── application/
│   ├── port/in/            # 인바운드 포트 (유스케이스 인터페이스)
│   ├── port/out/           # 아웃바운드 포트 (영속성 등 외부 의존성 인터페이스)
│   └── service/             # 유스케이스 구현체
└── adapter/
    ├── in/graphql/          # GraphQL 컨트롤러 + DTO
    └── out/persistence/     # JPA 엔티티 + 리포지토리 + 영속성 어댑터
```

의존성은 항상 도메인 방향으로만 향합니다: `adapter → application → domain`.

## 실행 방법

```bash
git clone https://github.com/youngsoo23/graphql.git
cd graphql

# 서버 기동
./gradlew bootRun
```

- GraphiQL: http://localhost:8080/graphiql
- GraphQL 엔드포인트: `POST http://localhost:8080/graphql`
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:graphql`, user: `sa`)

로컬에 여러 JDK가 설치돼 있다면 Java 24를 지정해서 실행하세요.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 24) ./gradlew bootRun
```

## GraphQL API

스키마 정의: [`src/main/resources/graphql/schema.graphqls`](./src/main/resources/graphql/schema.graphqls)

```graphql
type Query {
    post(id: ID!): Post
    posts(page: Int = 0, size: Int = 10): PostPage!
}

type Mutation {
    createPost(input: CreatePostInput!): Post!
    updatePost(id: ID!, input: UpdatePostInput!): Post!
    deletePost(id: ID!): Boolean!
}
```

예시 요청:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createPost(input: { title: \"첫 글\", content: \"내용입니다\", author: \"youngsoo\" }) { id title content author createdAt } }"}'

curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"query { posts(page: 0, size: 10) { content { id title } totalElements totalPages } }"}'
```

## 테스트

```bash
./gradlew test
```

계층별로 독립적인 테스트를 둡니다.

- `domain` — 프레임워크 없는 순수 단위 테스트 (검증 규칙/불변식)
- `application.service` — Mockito로 포트를 모킹해 유스케이스 로직만 검증
- `adapter.out.persistence` — `@DataJpaTest` + H2로 매핑/쿼리/페이징 검증
- `adapter.in.graphql` — `@GraphQlTest` + `GraphQlTester`로 스키마 계약 검증
