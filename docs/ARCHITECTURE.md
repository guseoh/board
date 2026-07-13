# 현재 아키텍처

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

## 전체 요청 구조

```text
Browser
→ Spring Security Filter Chain
→ View Controller
→ Request DTO/Form + Bean Validation/BindingResult
→ Service (@Transactional)
→ Spring Data JPA Repository
→ MySQL
→ Response DTO 또는 Model
→ Thymeleaf View 또는 Redirect
```

세션의 principal은 form/OAuth2 양쪽에서 `UnifiedPrincipal`로 통일된다. Controller는 `@AuthenticationPrincipal`로 회원 ID를 받고, Service는 ID로 Entity를 다시 조회한다.

## 계층 책임

| 계층 | 현재 책임 |
| --- | --- |
| Security | URL 인증/인가, form/OAuth2 login, logout, CSRF, principal 생성 |
| View Controller | 바인딩/Validation 분기, Model·Flash 구성, View/Redirect 선택 |
| Service | 트랜잭션, Entity 조회, 소유권·도메인 규칙, DTO 변환 |
| Repository | CRUD, fetch join, projection, count, bulk update/delete |
| Entity | 필드/연관관계, 생성·변경 규칙, 양방향 편의 메서드 |
| Thymeleaf | HTML 렌더링, Security 표현식, 폼·CSRF 제출 |

## 요청 흐름

### 회원가입

`MemberViewController.signup` → `MemberCreateRequest`의 nickname/password/email 검증 → `MemberService.signUp` → 이메일·닉네임 중복 및 passwordConfirm 비교 → BCrypt → `Member.create(..., Role.USER, LoginType.LOCAL)` → `MemberRepository.save` → `/loginForm` redirect. Bean Validation 오류는 `member/signup`, Service의 가입 정책 오류는 `GlobalViewControllerAdvice`가 nickname/email만 옮긴 안전한 form을 반환하고 password 필드는 보존하지 않는다.

### form login / OAuth2 login / logout

- `POST /login`은 Controller가 아니라 Security filter가 처리한다. `CustomUserDetailsService.loadUserByUsername` → `MemberRepository.findByEmail` → `UnifiedPrincipal.from` → `DaoAuthenticationProvider`/BCrypt 순이다.
- `CustomLoginSuccessHandler.onAuthenticationSuccess`는 ADMIN을 `/admin`, USER를 `/`로 보낸다.
- OAuth2는 `CustomOauth2UserService.loadUser`가 provider별 `OAuthUserInfo`로 속성을 변환하고 `(provider, providerId)`로 회원을 조회한다. 신규면 임의 비밀번호를 BCrypt로 인코딩해 SOCIAL/USER 회원을 저장한다.
- `POST /logout`은 Security logout filter가 Session 무효화와 `JSESSIONID` 삭제 후 `/`로 보낸다.

### 게시글 목록·검색·상세와 조회수

- `PostViewController.list` → `PostService.getPosts` → `PostRepository.findAllWithMember(Pageable)` fetch join → `PageResultDto<PostListResponse, Post>` → `post/list`.
- 목록 Controller는 전체/오늘 게시글, 회원 수와 로그인 사용자의 글/댓글 수를 추가 조회한다.
- `search` → `PostService.search` → `findByTitleContaining` → 전체 결과를 `PostListResponse`로 변환한다. 페이지네이션은 없다.
- `detail`은 먼저 private `increaseViewCount`에서 `View_Count` 쿠키를 검사·갱신한다. 같은 게시글 token이 없으면 `PostService.viewCount` → `PostRepository.incrementViewCount` bulk update 후, `getPostDetail`로 Post와 root comment tree를 DTO화해 `post/detail`을 반환한다.
- 쿠키는 path `/`, max-age 12시간, HttpOnly이며 Secure/SameSite 속성은 코드에 없다.

### 게시글 작성·수정·삭제

- 작성: `PostViewController.create` → `PostRequest` `@NotBlank` → `PostService.createPost` → Member 조회 → `Post.create` → save → `/post/{id}`.
- 수정 form: `getPostDetail`을 `PostRequest.from`으로 변환한다. POST는 `PostService.update`가 `validateWriter` 후 `Post.change`; 별도 save 없이 변경 감지를 사용한다.
- 삭제: `PostService.delete`가 작성자 검증 → `CommentRepository.deleteByPostId` bulk delete → `PostRepository.delete` 순으로 실행한다.
- 수정/삭제 GET/POST URL은 Security상 인증되지만, 수정 form GET은 `/post/**`의 POST 규칙에 해당하지 않아 비회원도 접근 가능하다. 수정 form 자체에서도 소유권을 검증하지 않고 제출 시 Service가 검증한다.

### 댓글·대댓글

- `CommentViewController.create`/`createReply`가 null principal을 별도 확인하고 로그인 화면으로 보낸다. `CommentCreateRequest.content` 검증 후 `CommentService`가 Member/Post/parent를 조회한다.
- 대댓글은 parent가 동일 Post 소속 root comment인지 `validateReply`로 제한한다.
- `Comment.create` → `Post.addComment`; parent가 있으면 `parent.addChild`로 양방향 메모리 관계를 맞춘 뒤 save한다.
- 수정/삭제는 `CommentService.validateOwner` 후 변경 감지 또는 `delete`를 사용한다.

### 마이페이지와 정보 변경

- `myForm`: 내 글/댓글 수, 최근 글 5개, 최근 댓글 5개를 합성한다.
- `myPostsForm`: `PostService.getMyPosts(memberId, pageRequestDto)`가 `PostRepository.findMyPosts`의 회원·제목 keyword·page 조건을 같은 query/count query에 적용한다. 같은 `PageResultDto`에서 목록과 page metadata를 만들며, 회원별 오늘 글 수와 `countMyPostViews` 누적 조회수도 제공한다.
- `myCommentForm`: `CommentService.getMyCommentPage`가 전체/오늘/7일 통계와 keyword가 적용된 DTO projection page를 만든다.
- 닉네임 변경은 `MemberService.updateNickname`의 변경 감지 후 `MyPageViewController.refreshAuthentication`이 Session principal의 nickname을 재구성한다.
- 비밀번호 변경은 current password BCrypt 검증, 새 비밀번호 확인, 변경 감지를 사용한다.

### 회원 탈퇴와 관리자 삭제

`MemberService.withdraw`와 `deleteMemberByAdmin`은 같은 `MemberRemovalPolicy`를 호출한다.

```text
CommentRepository.deleteAllByMemberId
→ CommentRepository.deleteAllByPostMemberId
→ PostRepository.deleteAllByMemberId
→ MemberRepository.deleteById
```

모두 하나의 쓰기 트랜잭션이다. Entity association에는 cascade/orphanRemoval이 없으므로 Service가 삭제 순서를 직접 관리한다. 다만 self-reference 댓글 parent FK를 해제하거나 자식부터 지우는 별도 쿼리는 없다.

### 관리자 기능

`AdminViewController` 전체에 `@PreAuthorize("hasRole('ADMIN')")`가 있고 URL filter도 `/admin/**`를 ADMIN으로 제한한다. 대시보드는 count, 목록은 Entity를 View에 전달한다. 게시글 삭제는 댓글 bulk delete 후 게시글 삭제, 회원 역할 변경은 `Role.valueOf(role)`과 변경 감지, 회원 삭제는 위 탈퇴 정책을 재사용한다.

## 트랜잭션 경계

| Service | 기본 | 쓰기 경계 |
| --- | --- | --- |
| `MemberService` | `@Transactional(readOnly=true)` | 가입, 역할/프로필 변경, 탈퇴·관리자 삭제 |
| `PostService` | `@Transactional(readOnly=true)` | 작성, 수정, 삭제, 관리자 삭제, 조회수 bulk update |
| `CommentService` | 클래스 전체 `@Transactional` | 읽기 메서드까지 read-write transaction |
| `CustomOauth2UserService.loadUser` | 쓰기 transaction | OAuth 신규 회원 저장 |

조회수 `@Modifying(clearAutomatically=true, flushAutomatically=true)`는 persistence context를 정리한다. 나머지 bulk delete에는 clear 옵션이 없다.

## 확인된 기술 부채

- Controller에 조회수 쿠키와 SecurityContext 재구성 로직이 있다.
- `CommentService` 읽기 기능도 read-write transaction이다.
- 게시글 검색/내 글 조회의 pagination 방식이 일관되지 않으며 page/size 범위 Validation이 없다.
- 전체 게시글 검색은 pagination하지 않고 page/size 입력 범위 Validation도 없다.
- Admin View가 Entity를 직접 노출한다.
- 회원 삭제 bulk 쿼리는 self-reference 댓글 삭제 순서를 명시하지 않는다.
- Post 상세의 LAZY comment/member 변환과 검색의 LAZY member 접근은 쿼리 수를 P6Spy로 확인할 필요가 있다.
- 가입 예외 입력 복원은 request/model에서 nickname과 email만 복사하는 Advice 로직에 의존한다.
