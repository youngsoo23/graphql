# graphql — 게시판 CRUD (GraphQL + JPA + DDD + 헥사고날 아키텍처)

Spring Boot 4 / Spring GraphQL로 만든 게시판(Post) CRUD 예제입니다. GraphQL을 처음 접하는
사람이 "포트/도메인 분리 + DDD + TDD" 베스트 프랙티스를 한 번에 볼 수 있도록 구성했습니다.

## 스택

- Java 24 (toolchain), Gradle
- Spring Boot 4.1.0 / Spring Framework 7.0.8
- Spring GraphQL 2.0.4 (graphql-java 25) — `spring-boot-starter-graphql`
- Spring Data JPA + H2 (in-memory)
- Lombok, JUnit 5 (Jupiter) + Mockito + AssertJ

로컬에 Java 25가 없어 `build.gradle`의 toolchain은 24로 맞춰뒀습니다. Java 25가 설치되면
`languageVersion`을 다시 25로 올려도 됩니다.

## 아키텍처: 포트 & 어댑터(헥사고날) + DDD

`com.study.graphql.post` 패키지 하나가 "게시글" bounded context 전체입니다.

```
post/
├── domain/                         # 순수 도메인 (프레임워크 의존성 없음)
│   ├── Post.java                   # 애그리거트 루트 — 생성/검증/수정 규칙을 캡슐화
│   └── PostNotFoundException.java
├── application/
│   ├── port/
│   │   ├── in/                     # 인바운드 포트 = 유스케이스 인터페이스
│   │   │   ├── CreatePostUseCase, CreatePostCommand
│   │   │   ├── UpdatePostUseCase, UpdatePostCommand
│   │   │   ├── DeletePostUseCase
│   │   │   ├── GetPostQuery
│   │   │   └── GetPostListQuery, PostPage
│   │   └── out/                    # 아웃바운드 포트 = 영속성 등 외부 의존성 인터페이스
│   │       ├── LoadPostPort
│   │       ├── SavePostPort
│   │       └── DeletePostPort
│   └── service/                    # 유스케이스 구현체 (인바운드 포트 구현, 아웃바운드 포트 사용)
│       ├── PostCommandService       # Create/Update/Delete
│       └── PostQueryService         # Get/GetList
└── adapter/
    ├── in/graphql/                 # 인바운드 어댑터: GraphQL 컨트롤러 + DTO
    │   ├── PostGraphqlController
    │   ├── PostGraphqlExceptionHandler
    │   ├── CreatePostInput, UpdatePostInput  (요청 DTO)
    │   └── PostResponse, PostPageResponse    (응답 DTO)
    └── out/persistence/             # 아웃바운드 어댑터: JPA
        ├── PostJpaEntity
        ├── SpringDataPostRepository
        └── PostPersistenceAdapter   (포트 구현체이자 도메인 ↔ 엔티티 매퍼)
```

핵심 규칙:

- **의존성 방향은 항상 안쪽(domain)을 향한다.** `domain`은 아무것도 import하지 않고,
  `application`은 `domain`과 자신의 `port`만 알고, `adapter`는 `application.port`를
  구현/사용한다. GraphQL이든 JPA든 바뀌어도 `domain`, `application`은 손대지 않는다.
- **`domain.Post`에 JPA/GraphQL 어노테이션을 절대 넣지 않는다.** 영속성 모델(`PostJpaEntity`)과
  API 모델(`PostResponse` 등)은 각 어댑터 안에서만 존재하고, 어댑터가 도메인 객체로/에서 매핑한다.
- **포트 인터페이스는 application 패키지 소유.** `port.in`은 컨트롤러가 호출하는 진입점,
  `port.out`은 영속성 어댑터가 구현하는 확장점. 새 인바운드/아웃바운드 어댑터(REST, 메시징,
  Redis 캐시 등)를 추가해도 포트 인터페이스와 애플리케이션 서비스는 그대로 재사용한다.
- **`@UseCase`** (`com.study.graphql.common.UseCase`, `@Service`의 별칭)는 애플리케이션
  서비스 전용 스테레오타입. 어댑터의 `@Controller`/`@Component`와 구분하기 위한 표식일 뿐,
  동작은 `@Service`와 동일하다.
- 다른 게시판 기능(댓글, 카테고리 등)을 추가할 때도 `com.study.graphql.<feature>` 밑에
  동일한 `domain / application(port.in, port.out, service) / adapter(in, out)` 구조를 반복한다.

## DDD 포인트

- `Post`는 불변에 가까운 애그리거트 루트: `create()`(정적 팩토리, 신규 생성 + 검증),
  `reconstitute()`(영속성 계층 전용 재구성), `assignId()`(저장 후 id 부여), `update()`(도메인
  규칙을 통과해야만 상태 변경 가능).
- 검증 규칙(제목/본문 공백 금지, 제목 200자·본문 10,000자 제한)은 도메인 안에 있다.
  컨트롤러나 서비스가 아니라 `Post` 자신이 스스로를 보호한다.
- `PostNotFoundException`은 도메인 예외. 어댑터(`PostGraphqlExceptionHandler`)가 이를
  GraphQL `ErrorType.NOT_FOUND`로 번역한다 — 도메인은 GraphQL을 모른다.

## TDD 워크플로

새 기능을 추가할 때는 아래 순서로 테스트를 먼저 작성:

1. **`domain`** — 순수 JUnit 단위 테스트. 스프링 컨텍스트 없이 검증 규칙/불변식을 빠르게 검증.
   (`src/test/.../post/domain/PostTest.java`)
2. **`application.service`** — Mockito로 `port.out`을 모킹하고 유스케이스 로직만 검증.
   (`PostCommandServiceTest`, `PostQueryServiceTest`)
3. **`adapter.out.persistence`** — `@DataJpaTest` + 실제 H2로 매핑/쿼리/페이징 검증.
   (`PostPersistenceAdapterTest`)
4. **`adapter.in.graphql`** — `@GraphQlTest` + `GraphQlTester` + `@MockitoBean`으로 포트를
   모킹하고 스키마 계약(쿼리/뮤테이션 응답, 에러 타입)을 검증. (`PostGraphqlControllerTest`)

각 계층은 독립적으로 테스트되므로, 예를 들어 JPA를 다른 영속성 기술로 바꿔도 domain/application
테스트는 전혀 손대지 않아도 된다.

## 실행 명령

```bash
# 전체 테스트
JAVA_HOME=<jdk24-home> ./gradlew test

# 특정 계층만
./gradlew test --tests "com.study.graphql.post.domain.*"

# 서버 기동 (GraphiQL: http://localhost:8080/graphiql, H2 콘솔: /h2-console)
./gradlew bootRun
```

로컬 JDK가 여러 개 설치돼 있으면 `JAVA_HOME`을 24로 지정하고 실행할 것
(`/usr/libexec/java_home -v 24` 로 경로 확인).

## GraphQL 스키마

`src/main/resources/graphql/schema.graphqls` 가 서버 스키마다. (참고: `build.gradle`의
`com.netflix.dgs.codegen` 플러그인은 `src/main/resources/graphql-client`를 읽어 **클라이언트**
코드를 생성하는 별도 설정이며, 서버 스키마와는 무관하다. 서버 스키마를 바꿀 땐 `graphql/`
디렉터리만 수정하면 된다.)

주요 연산:

- `query post(id: ID!): Post`
- `query posts(page: Int = 0, size: Int = 10): PostPage!`
- `mutation createPost(input: CreatePostInput!): Post!`
- `mutation updatePost(id: ID!, input: UpdatePostInput!): Post!`
- `mutation deletePost(id: ID!): Boolean!`

## 코딩 컨벤션

- DTO(요청/응답)는 record로 작성하고 GraphQL 어댑터 패키지 밖으로 노출하지 않는다
  (package-private).
- 포트 인터페이스 메서드는 도메인 타입(`Post`, `PostPage`)만 주고받는다. JPA 엔티티나
  GraphQL DTO가 포트 시그니처에 등장하면 계층 위반.
- 새 유스케이스를 추가할 때: `port.in`에 인터페이스(+필요하면 커맨드 record) 추가 →
  `application.service`에 구현 추가 → 필요한 `port.out` 메서드 추가 → 어댑터에서 연결.
  이 순서를 지키면 항상 안쪽(도메인)부터 설계하게 된다.
