# 패키지 구조와 설계 이유

## 실제 구조

```
com.study.graphql
├── common/
│   └── UseCase.java                 # @Service 별칭 스테레오타입 어노테이션
└── post/                            # "게시글" bounded context 전체
    ├── domain/                      # 순수 도메인 (프레임워크 의존성 없음)
    │   ├── Post.java                # 애그리거트 루트
    │   └── PostNotFoundException.java
    ├── application/
    │   ├── port/
    │   │   ├── in/                  # 인바운드 포트 = 유스케이스 인터페이스
    │   │   │   ├── CreatePostUseCase, CreatePostCommand
    │   │   │   ├── UpdatePostUseCase, UpdatePostCommand
    │   │   │   ├── DeletePostUseCase
    │   │   │   ├── GetPostQuery
    │   │   │   └── GetPostListQuery, PostPage
    │   │   └── out/                 # 아웃바운드 포트 = 영속성 등 외부 의존성 인터페이스
    │   │       ├── LoadPostPort
    │   │       ├── SavePostPort
    │   │       └── DeletePostPort
    │   └── service/                 # 유스케이스 구현체
    │       ├── PostCommandService   # Create/Update/Delete
    │       └── PostQueryService     # Get/GetList
    └── adapter/
        ├── in/graphql/              # 인바운드 어댑터: GraphQL 컨트롤러 + DTO
        │   ├── PostGraphqlController
        │   ├── PostGraphqlExceptionHandler
        │   ├── CreatePostInput, UpdatePostInput
        │   └── PostResponse, PostPageResponse
        └── out/persistence/         # 아웃바운드 어댑터: JPA
            ├── PostJpaEntity
            ├── SpringDataPostRepository
            └── PostPersistenceAdapter
```

## 왜 이렇게 나눴는가

### 1. "기능 단위(post)"로 묶고, 그 안에서 계층을 나눈다

`controller / service / repository` 처럼 **기술 종류**로 최상위를 나누지 않고,
`post`라는 **도메인(bounded context)**으로 최상위를 나눴다. 게시판에 댓글, 카테고리
같은 기능이 추가되면 `com.study.graphql.comment`, `com.study.graphql.category`가
같은 패턴으로 옆에 생긴다. 이렇게 하면:

- 한 기능을 이해하려고 여러 최상위 패키지(`controller`, `service`, `repository`)를
  왔다갔다 할 필요 없이 `post/` 하나만 보면 된다.
- 기능을 통째로 들어내거나(마이크로서비스 분리 등) 다른 프로젝트로 옮기기 쉽다.

### 2. 왜 헥사고날(포트&어댑터)인가 — 의존성 방향을 안쪽으로 고정

핵심 문제의식: **"GraphQL을 쓴다"는 결정과 "JPA를 쓴다"는 결정이 도메인 규칙까지
오염시키면 안 된다.** 예를 들어 나중에 REST로 바꾸거나, JPA를 MongoDB로 바꾸는
상황을 생각하면:

- GraphQL 컨트롤러나 JPA 엔티티에 비즈니스 규칙이 섞여 있으면, 기술을 바꿀 때마다
  규칙을 다시 찾아서 옮겨야 한다.
- 그래서 `domain`(도메인)을 가장 안쪽에 두고, 바깥쪽(`adapter`)이 안쪽
  (`application` → `domain`)에만 의존하도록 강제한다. 반대 방향 의존은 없다.
- `adapter.in.graphql`이 사라지고 `adapter.in.rest`가 생겨도 `domain`, `application`은
  단 한 줄도 안 바뀐다 — 인바운드 포트(`CreatePostUseCase` 등)를 그대로 호출하기만
  하면 되기 때문이다. `adapter.out.persistence`가 JPA에서 다른 저장소로 바뀌어도
  마찬가지로 아웃바운드 포트(`SavePostPort` 등) 계약만 지키면 된다.

### 3. `port.in` / `port.out`을 `application` 소유로 둔 이유

포트 인터페이스가 어댑터 쪽에 있으면 "누가 누구에게 의존하는가"가 헷갈리기 쉽다.
포트를 `application`이 소유하게 하면 규칙이 단순해진다:

- `port.in` = 바깥에서 안으로 들어오는 진입점 → 어댑터(컨트롤러)가 이 인터페이스를
  **호출**한다.
- `port.out` = 안에서 바깥으로 나가는 확장점 → 어댑터(영속성)가 이 인터페이스를
  **구현**한다.

즉 화살표가 항상 `adapter → application`으로만 향하고, `application`은 `adapter`를
전혀 모른다. 이게 지켜지는지 코드 리뷰에서 한 줄로 검증 가능하다: "`application`
패키지 안에서 `adapter`를 import하는 곳이 있는가?" 있으면 위반.

`in`/`out`은 **애플리케이션 코어(domain + application) 기준으로 호출 방향이 어느
쪽인지**를 가리키는 이름이다.

```
[adapter.in.graphql] --(호출)--> [port.in]      --구현--> [application.service]
                                                                 |
                                                                 v
[adapter.out.persistence] <--(구현)-- [port.out] <--(호출)-- [application.service]
```

| | 인터페이스 소유 | 호출하는 쪽 | 구현하는 쪽 |
|---|---|---|---|
| `port.in` (예: `CreatePostUseCase`) | application | `adapter.in` | `application.service` |
| `port.out` (예: `SavePostPort`) | application | `application.service` | `adapter.out` |

두 경우 다 인터페이스는 `application`이 소유하지만, `port.in`은 "밖 → 안으로
호출당하는" 인터페이스이고 `port.out`은 "안에서 밖으로 호출하기 위한" 인터페이스라서
방향이 반대다. `adapter.in`/`adapter.out` 패키지명도 같은 방향을 그대로 따라간
것이다.

#### 3-1. `port.in` 인터페이스를 유스케이스 하나당 하나씩 쪼갠 이유

`CreatePostUseCase`, `UpdatePostUseCase`, `DeletePostUseCase`, `GetPostQuery`,
`GetPostListQuery`를 CRUD 메서드 다 담은 `PostUseCase` 인터페이스 하나로 합치지
않고, 유스케이스 단위로 쪼갰다. 인터페이스 분리 원칙(ISP)을 포트에 적용한 것이다.

만약 하나로 합쳤다면:

```java
interface PostUseCase {
    Post createPost(CreatePostCommand command);
    Post updatePost(UpdatePostCommand command);
    void deletePost(Long id);
    Post getPost(Long id);
    PostPage getPosts(int page, int size);
}
```

이렇게 생기는 문제:

1. **의존성이 실제 필요보다 넓어진다.** 조회만 하는 클래스(예: 나중에 생길 리포팅
   배치, 캐시 워밍 로직)도 이 인터페이스를 주입받으면 `createPost`, `deletePost`
   까지 눈에 보이고 호출 가능해진다. 실제로는 조회만 쓰는데 쓰기 권한까지 딸려온다.
2. **생성자 시그니처가 의도를 숨긴다.** `PostGraphqlController`가 지금은
   `CreatePostUseCase`, `UpdatePostUseCase` 등을 각각 주입받는데, 이걸 보면
   "이 컨트롤러가 정확히 어떤 능력을 쓰는지" 생성자만 보고 알 수 있다. 인터페이스가
   하나로 합쳐져 있으면 실제로 `deletePost`를 호출하는지 코드를 다 읽어야 안다.
3. **테스트 모킹이 불필요하게 커진다.** 조회 로직만 테스트하고 싶을 때
   (`PostQueryServiceTest`), 인터페이스가 분리돼 있으면 필요한 것만 모킹하면
   되지만, 하나로 합쳐져 있으면 안 쓰는 메서드까지 신경 써야 한다.
4. **커맨드/쿼리 책임이 뒤섞인다.** `Get*Query`(조회)와 `*UseCase`(상태 변경)로
   이름부터 나눠놓은 건 CQRS적 관점 — 상태를 바꾸는 연산과 조회 연산을 구분한다는
   의도가 인터페이스 이름에서부터 드러난다. `PostCommandService`(Create/Update/
   Delete)와 `PostQueryService`(Get/GetList)로 구현체도 나눠져 있는 것과 짝을
   이룬다.

한 줄 요약: **"이 클래스가 무엇을 할 수 있는지"가 타입 시그니처(생성자 파라미터)만
봐도 드러나게** 하고, 각 유스케이스를 독립적으로 테스트/확장 가능하게 하기 위해서다.

### 4. `domain.Post`에 JPA/GraphQL 어노테이션을 넣지 않는 이유

`Post`에 `@Entity`, `@Table` 같은 JPA 어노테이션을 붙이면 domain 클래스가 곧
영속성 모델이 되어버린다. 그러면:

- 테이블 구조가 바뀔 때마다(컬럼 추가, 인덱스 전략 변경 등) 도메인 클래스도 흔들린다.
- GraphQL 응답 형태를 바꾸고 싶을 때도 `Post`를 건드리게 된다 (API 응답과 도메인
  모델이 항상 1:1일 필요는 없다).

그래서 `PostJpaEntity`(영속성 전용), `PostResponse`/`PostPageResponse`(API 응답
전용)를 각 어댑터 안에 따로 두고, 어댑터가 도메인 객체 ↔ 자신의 모델을 매핑한다.
`Post`는 오직 비즈니스 규칙(검증, 생성, 수정)만 신경 쓴다.

### 5. `Post`를 불변에 가깝게, 정적 팩토리로 생성하게 만든 이유

`create()`(신규 생성 + 검증), `reconstitute()`(영속성 계층 전용 재구성),
`assignId()`(저장 후 id 부여), `update()`(도메인 규칙 통과해야 상태 변경 가능)로
생성/변경 경로를 명시적으로 나눴다. "새로 만드는 것"과 "DB에서 복원하는 것"은
검증 요구 사항이 다르기 때문에(복원된 데이터는 이미 검증을 통과했던 데이터) 이를
구분해서 실수로 검증을 건너뛰거나 중복 검증하는 걸 막는다.

제목/본문 공백 금지, 길이 제한 같은 검증 규칙을 컨트롤러나 서비스가 아니라
`Post` 스스로 갖게 한 것도 같은 맥락: 어떤 경로로 `Post`가 만들어지든(GraphQL,
나중에 생길 REST, 배치 잡 등) 규칙이 항상 적용되도록 보장하기 위해서다.

### 6. `@UseCase`를 따로 만든 이유

기능적으로는 `@Service`와 동일하지만, "이 클래스는 애플리케이션 서비스(유스케이스
구현체)다"라는 걸 어댑터의 `@Controller`/`@Component`와 시각적으로 구분하기 위한
표식이다. 코드를 읽을 때 어노테이션만 보고도 계층을 알 수 있게 하려는 목적.

### 7. `adapter.in.graphql`에서 `@Argument`가 하는 일

`PostGraphqlController`의 메서드들은 `@Argument`로 GraphQL 스키마의 인자를 자바
파라미터에 바인딩한다. REST의 `@RequestParam`/`@PathVariable`/`@RequestBody`를
합쳐놓은 것과 비슷한 역할이다.

```graphql
type Mutation {
    createPost(input: CreatePostInput!): Post!
    updatePost(id: ID!, input: UpdatePostInput!): Post!
}
```

```java
@MutationMapping(name = "createPost")
PostResponse createPost(@Argument CreatePostInput input) { ... }

@MutationMapping(name = "updatePost")
PostResponse updatePost(@Argument Long id, @Argument UpdatePostInput input) { ... }
```

- **매칭 기준은 파라미터 이름**이다. `@Argument Long id` ↔ 스키마의 `id: ID!`,
  `@Argument CreatePostInput input` ↔ 스키마의 `input: CreatePostInput!`처럼 이름이
  같아야 자동으로 연결된다. 이름이 다르면 `@Argument("실제스키마인자명")`처럼 명시할
  수 있다 (`@MutationMapping(name = ...)`과 같은 방식).
- **스칼라 타입**(`ID` → `Long`)은 자동 변환된다.
- **input 타입**(`CreatePostInput`)은 GraphQL 요청으로 넘어온 값을 자바 record의
  각 필드에 자동으로 채워 넣어준다 — 직접 파싱할 필요가 없다.

## 요약: 이 구조가 지키는 규칙 한 줄

**의존성은 항상 `adapter → application → domain` 방향으로만 흐른다.** 어떤 기술
(GraphQL, JPA, 나중에 추가될 REST, Redis 캐시 등)을 갈아끼워도 `domain`과
`application`은 그대로 두고 `adapter`만 새로 작성하면 된다는 것이 이 설계의
핵심 목적이다. GraphQL을 쓰는 이유와 REST 차이는 [[graphql-vs-rest]] 문서 참고.
