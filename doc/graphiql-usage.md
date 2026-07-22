# GraphiQL 사용법

GraphQL은 REST의 Swagger 같은 별도 문서 도구 대신, 스키마 자체가 계약이자 문서
역할을 합니다(introspection). 이 프로젝트는 `spring-boot-starter-graphql`이 제공하는
**GraphiQL**을 통해 스키마 탐색 + 쿼리 실행을 Swagger UI 대신 제공합니다.

## 1. 서버 실행

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 24) ./gradlew bootRun
```

## 2. GraphiQL 접속

브라우저에서 `http://localhost:8080/graphiql` 접속.

## 3. 스키마 탐색 (Swagger의 "API 목록 보기"에 해당)

- **Docs** 패널을 열면 `Query`, `Mutation` 타입과 그 안의 필드들
  (`post`, `posts`, `createPost` 등)이 트리로 나온다.
- 각 필드를 클릭하면 파라미터 타입, 리턴 타입, 설명이 나온다 — introspection으로
  실시간 생성된 문서.
- 타이핑 중 `Ctrl+Space`로 필드/인자 자동완성도 가능하다.

## 4. 조회(Query) 예시

```graphql
query {
  posts(page: 0, size: 10) {
    content { id title }
    totalElements
  }
}
```

## 5. 생성(Mutation) 예시

에디터에 작성:

```graphql
mutation {
  createPost(input: { title: "첫 게시글", content: "본문 내용입니다" }) {
    id
    title
    content
    createdAt
  }
}
```

상단(또는 중앙)의 **▶ (Play) 버튼**을 누르거나 `Ctrl+Enter`(Mac은 `Cmd+Enter`)로 실행.

응답 예:

```json
{
  "data": {
    "createPost": {
      "id": "1",
      "title": "첫 게시글",
      "content": "본문 내용입니다",
      "createdAt": "2026-07-22T..."
    }
  }
}
```

## 6. 변수(Variables)로 분리하기

쿼리를 재사용하려면 하드코딩 대신 변수를 쓴다.

에디터:

```graphql
mutation CreatePost($input: CreatePostInput!) {
  createPost(input: $input) {
    id
    title
    content
  }
}
```

하단 **Variables** 패널(GraphiQL 화면 아래쪽 탭)에:

```json
{
  "input": { "title": "첫 게시글", "content": "본문 내용입니다" }
}
```

값만 바꿔가며 같은 쿼리를 반복 실행할 때 편하다.

## 7. curl로 호출하기

UI 없이 터미널에서 바로 호출하고 싶다면:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { createPost(input: { title: \"제목\", content: \"내용\" }) { id title content } }"}'
```
