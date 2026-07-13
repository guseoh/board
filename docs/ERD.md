# Entity 구조와 관계

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

이 문서는 사용자가 ERD를 직접 작성하기 위한 현재 JPA metadata 설명이다. ERD 이미지, Mermaid, DBML, DDL은 포함하지 않는다. 세 Entity 모두 `@Table`이 없으므로 테이블명은 naming strategy에 따른 암시적 이름(통상 `member`, `post`, `comment`)이며 코드만으로 물리 DB의 대소문자까지 확정할 수 없다.

## 공통 audit: `BaseEntity`

`@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`가 적용된다. `JpaConfig.auditorProvider`는 인증된 `UnifiedPrincipal.nickname`, 그 외에는 `system`을 반환한다.

| 필드 | Java 타입 | JPA annotation | nullable | 기본/동작 |
| --- | --- | --- | --- | --- |
| `createdAt` | `LocalDateTime` | `@CreatedDate`, `@Column(updatable=false)` | 명시 없음 | 최초 persist 시 audit |
| `createdBy` | `String` | `@CreatedBy`, `@Column(updatable=false, nullable=false)` | 불가 | nickname 또는 `system` |
| `updatedAt` | `LocalDateTime` | `@LastModifiedDate` | 명시 없음 | 변경 시 audit |
| `updatedBy` | `String` | `@LastModifiedBy`, `@Column(nullable=false)` | 불가 | nickname 또는 `system` |

## `Member`

| 필드 | Java 타입 | JPA annotation | nullable | unique/length/기본 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `@Id`, `IDENTITY` | PK | PK |
| `nickname` | `String` | `@Column(nullable=false,length=100)` | 불가 | DB unique 아님 |
| `password` | `String` | `@Column(nullable=false)` | 불가 | BCrypt 또는 OAuth dummy hash |
| `email` | `String` | `@Column(nullable=false,unique=true)` | 불가 | unique |
| `role` | `Role` | `@Enumerated(STRING)`, nullable=false | 불가 | `ADMIN`/`USER` |
| `posts` | `List<Post>` | `@OneToMany(mappedBy="member")` | 관계 컬렉션 | 빈 `ArrayList`, cascade 없음 |
| `provider` | `String` | 기본 column | 허용 | LOCAL은 null |
| `providerId` | `String` | 기본 column | 허용 | LOCAL은 null, unique 없음 |
| `loginType` | `LoginType` | `@Enumerated(STRING)` | 명시 없음 | `LOCAL`/`SOCIAL` |

`Member`에는 comments 컬렉션이 없다.

## `Post`

| 필드 | Java 타입 | JPA annotation | nullable | unique/length/기본 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `@Id`, `IDENTITY` | PK | PK |
| `title` | `String` | nullable=false, length=500 | 불가 | unique 아님 |
| `content` | `String` | nullable=false, length=500 | 불가 | unique 아님 |
| `viewCount` | `int` | 기본 column | primitive | Java 신규 객체 기본 0 |
| `member` | `Member` | `@ManyToOne(LAZY)`, `@JoinColumn(name="member_id",nullable=false)` | 불가 | FK는 Post 쪽 |
| `comments` | `List<Comment>` | `@OneToMany(mappedBy="post")`, `@OrderBy("id asc")` | 관계 컬렉션 | cascade/orphanRemoval 없음 |

## `Comment`

| 필드 | Java 타입 | JPA annotation | nullable | unique/length/기본 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `@Id`, `IDENTITY` | PK | PK |
| `content` | `String` | nullable=false, length=500 | 불가 | unique 아님 |
| `member` | `Member` | `@ManyToOne(LAZY)`, `member_id` nullable=false | 불가 | FK는 Comment 쪽 |
| `post` | `Post` | `@ManyToOne(LAZY)`, `post_id` nullable=false | 불가 | FK는 Comment 쪽 |
| `parent` | `Comment` | `@ManyToOne(LAZY)`, `parent_id` | 허용 | root는 null |
| `children` | `List<Comment>` | `@OneToMany(mappedBy="parent")` | 관계 컬렉션 | cascade/orphanRemoval/order 없음 |

## 관계 상세

| 관계 | cardinality | FK 위치 / 주인 | inverse `mappedBy` | fetch | cascade / orphanRemoval | nullable | 편의 메서드 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Member : Post | 1:N | `post.member_id`; Post | `Member.posts` → `member` | N:1 LAZY; 1:N 기본 LAZY | 없음 / false | FK 불가 | `Post.assignMember`가 양쪽 연결 |
| Member : Comment | 1:N 단방향 역참조 없음 | `comment.member_id`; Comment | Member에 collection 없음 | N:1 LAZY | 없음 | FK 불가 | Comment 생성 시 직접 지정 |
| Post : Comment | 1:N | `comment.post_id`; Comment | `Post.comments` → `post` | N:1 LAZY; 1:N 기본 LAZY | 없음 / false | FK 불가 | `Post.addComment` ↔ `Comment.addPost` |
| Comment : Comment | 1:N 자기 참조 | child의 `parent_id`; child Comment | `children` → `parent` | N:1 LAZY; 1:N 기본 LAZY | 없음 / false | parent 허용 | `parent.addChild`가 양쪽 연결 |

현재 도메인 규칙은 `CommentService.validateReply`에서 parent가 같은 Post의 root인지만 허용하여 깊이를 1단계로 제한한다. DB constraint만으로는 이 깊이 규칙을 표현하지 않는다.

## 삭제 동작과 주의점

- Entity association에는 remove cascade와 orphanRemoval이 전혀 없다.
- 게시글 삭제: `deleteByPostId` bulk delete 후 Post 삭제.
- 회원 삭제/탈퇴: 회원 작성 댓글 → 회원 작성 Post의 모든 댓글 → 회원 Post → Member 순서의 bulk delete.
- self-reference `parent_id`에 대한 `ON DELETE` 옵션은 코드에 없고, child를 먼저 삭제하거나 parent reference를 null로 만드는 JPQL도 없다. 다른 회원이 작성한 reply가 삭제 대상 root를 참조하는 경우의 FK 동작은 현재 테스트로 보장되지 않는다.
- bulk delete는 persistence context를 자동 clear하지 않으므로 같은 transaction에서 이미 로드된 Entity와 DB 상태가 어긋날 가능성을 검토해야 한다.

## 모델링상 현재 제한

- `nickname`은 Service에서 중복 검사하지만 DB unique constraint가 없다.
- `(provider, providerId)`는 조회 키지만 DB unique constraint가 없다.
- DTO에는 title/content/comment 길이 제한이 없어 DB length 500과 Validation 계약이 일치하지 않는다.
- `ddl-auto=update`이므로 명시적 migration/DDL이 저장소에 없다.
