# SSR HTTP Routing / Form Contract

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

이 문서는 JSON REST API 명세가 아니다. 현재 요청은 주로 `application/x-www-form-urlencoded`, 인증은 `JSESSIONID` Session, 응답은 Thymeleaf View 또는 3xx Redirect다. 향후 REST API 계약은 M2에서 별도로 정의한다. `PostApiController`는 annotation과 Mapping이 없는 빈 클래스이므로 활성 API가 아니다.

## 공통 계약

- GET query/form 객체는 Spring MVC 기본 바인딩을 사용한다.
- 쓰기 POST에는 기본 활성화된 CSRF token이 필요하다.
- `@Valid` 다음의 `BindingResult`로 form 오류를 분기한다.
- `CustomException`은 `GlobalViewControllerAdvice`가 가입 오류를 제외하고 ErrorCode URL로 redirect하며 flash `msg`를 넣는다.
- `/admin/**`는 filter와 `@PreAuthorize` 양쪽에서 ADMIN만 허용한다.
- `POST /post/**`, `/my/**`는 인증이 필요하며, 그 외 Mapping은 기본 permitAll이다.

## Controller Mapping

| 기능 | HTTP Method | URI | 인증 | 권한 | 요청 형식 | Request DTO / 인자 | Validation | 호출 Service | 성공 응답 | 실패 처리 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 회원가입 화면 | GET | `/signup` | 불필요 | 전체 | 없음 | `Model` | 없음 | 없음 | `member/signup`; `form` | View 오류 |
| 회원가입 | POST | `/signup` | 불필요 | 전체 | form | `@ModelAttribute("form") MemberCreateRequest` | `@Valid`, `BindingResult` | `MemberService.signUp` | `redirect:/loginForm`, flash | 같은 View 또는 전역 예외 |
| 로그인 화면 | GET | `/loginForm` | 불필요 | 전체 | query 선택 | 없음 (`redirect`, `error`, `logout`는 View가 param 참조) | 없음 | 없음 | `member/loginForm` | View 오류 |
| 게시글 목록 | GET | `""`, `/` | 선택 | 전체 | query | `PageRequestDto`, principal, Model | 없음 | Post/Member/Comment Service | `post/list` | 전역 예외 |
| 게시글 상세 | GET | `/post/{id}` | 선택 | 전체 | path + cookie | `id`, principal, request/response | 없음 | `viewCount`, `getPostDetail` | `post/detail` | 없는 글 redirect |
| 게시글 검색 | GET | `/posts/search` | 불필요 | 전체 | query | `@RequestParam keyword`, Model | required 바인딩만 | `PostService.search` | `post/list` | 400 또는 예외 |
| 작성 화면 | GET | `/post/new` | 불필요 | 전체 | 없음 | Model | 없음 | 없음 | `post/form`, create mode | 없음 |
| 게시글 작성 | POST | `/post/new` | 필요 | USER/ADMIN | form | `PostRequest`, principal, RedirectAttributes | `@Valid`, `BindingResult`, CSRF | `createPost` | `redirect:/post/{id}`, URI attribute+flash | `post/form` 또는 전역 예외 |
| 수정 화면 | GET | `/post/{id}/edit` | 불필요 | 전체 | path | `id`, Model | 없음 | `getPostDetail` | `post/form`, edit mode | 없는 글 redirect |
| 게시글 수정 | POST | `/post/{id}/edit` | 필요 | 작성자 | form+path | `PostRequest`, principal, Model, RedirectAttributes | `@Valid`, `BindingResult`, CSRF | `PostService.update` | `redirect:/post/{id}`, flash | form 또는 소유권 예외 |
| 게시글 삭제 | POST | `/post/{id}/delete` | 필요 | 작성자 | path | `id`, principal, RedirectAttributes | CSRF | `PostService.delete` | `redirect:/`, flash | 없는 글/소유권 예외 |
| 댓글 작성 | POST | `/post/{postId}/comment` | 필요 | USER/ADMIN | form+path | `CommentCreateRequest`, principal, RA | `@Valid`, `BindingResult`, CSRF | `createComment` | 상세 redirect | null principal은 login redirect; Validation flash |
| 대댓글 작성 | POST | `/post/{postId}/comment/{parentId}/replies` | 필요 | USER/ADMIN | form+path | `CommentCreateRequest`, `postId`, `parentId` | `@Valid`, BindingResult, CSRF | `createReply` | 상세 redirect | login/Validation/parent 예외 |
| 댓글 수정 | POST | `/post/{postId}/comment/{commentId}/edit` | 필요 | 작성자 | form+path | `@ModelAttribute("commentUpdateForm") CommentCreateRequest` | `@Valid`, BindingResult, CSRF | `CommentService.update` | 상세 redirect | Validation/없음/소유권 예외 |
| 댓글 삭제 | POST | `/post/{postId}/comment/{commentId}/delete` | 필요 | 작성자 | path | IDs, principal | CSRF | `CommentService.delete` | 상세 redirect | 없음/소유권 예외 |
| 마이페이지 | GET | `/my` | 필요 | 인증 사용자 | 없음 | principal, Model | principal ID 직접 확인 | 4개 Post/Comment 조회 | `my/my` | 인증 예외 |
| 내 게시글 | GET | `/my/posts` | 필요 | 인증 사용자 | query | `PageRequestDto`, principal, Model | 없음 | `getMyPosts`, 글 수·오늘 글 수·누적 조회수 | `my/myPost` | 전역 예외 |
| 내 댓글 | GET | `/my/comments` | 필요 | 인증 사용자 | query | `PageRequestDto`, 별도 `keyword`, principal | 없음 | `getMyCommentPage` | `my/myComment` | 전역 예외 |
| 탈퇴 화면 | GET | `/my/withdraw` | 필요 | 인증 사용자 | 없음 | 없음 | 없음 | 없음 | `my/withdraw` | Security login redirect |
| 회원 탈퇴 | POST | `/my/withdraw` | 필요 | 본인 | form(path 인자 없음) | principal, request/response | CSRF; `confirmText` 서버 미검증 | `MemberService.withdraw` | logout 후 `redirect:/` | 전역 예외 |
| 정보 수정 화면 | GET | `/my/edit` | 필요 | 본인 | 없음 | principal, Model | 없음 | `getMyProfile` | `my/myEdit` | 전역 예외 |
| 닉네임 수정 | POST | `/my/edit/nickname` | 필요 | 본인 | form | `MemberNicknameUpdateRequest`, principal | `@Valid`, BindingResult, CSRF | `updateNickname` | `redirect:/`, flash | edit View 또는 전역 예외 |
| 비밀번호 수정 | POST | `/my/edit/password` | 필요 | 본인 | form | `MemberPasswordUpdateRequest`, principal | `@Valid`, BindingResult, CSRF | `updatePassword` | `redirect:/`, flash | edit View 또는 전역 예외 |
| 관리자 홈 | GET | `/admin` | 필요 | ADMIN | 없음 | Model | method security | count Service | `admin/index` | login redirect/403 |
| 관리자 게시글 | GET | `/admin/posts` | 필요 | ADMIN | 없음 | Model | method security | `getPostsForAdmin` | `admin/posts` | login redirect/403 |
| 관리자 게시글 삭제 | POST | `/admin/posts/{postId}/delete` | 필요 | ADMIN | path | `postId` | CSRF+method security | `deleteForAdmin` | `redirect:/admin` | 403/예외 |
| 관리자 회원 | GET | `/admin/users` | 필요 | ADMIN | 없음 | Model | method security | `getMembersForAdmin` | `admin/users` | login redirect/403 |
| 역할 변경 | POST | `/admin/users/{memberId}/role` | 필요 | ADMIN | form+path | `memberId`, `@RequestParam role` | CSRF; enum 사전 검증 없음 | `changeMemberRole` | `redirect:/admin` | 403/예외 |
| 회원 삭제 | POST | `/admin/users/{memberId}/delete` | 필요 | ADMIN | path | `memberId` | CSRF | `deleteMemberByAdmin` | `redirect:/admin` | 403/예외 |
| 알림 오류 테스트 | GET | `/test/discord-error` | 불필요 | local profile | 없음 | 없음 | 없음 | 없음; 의도적 예외 | 없음 | `POST_NOT_FOUND` 전역 처리로 `/` redirect |

## Security filter가 제공하는 route

| 기능 | Method/URI | 요청 | 성공 | 실패 |
| --- | --- | --- | --- | --- |
| form login | `POST /login` | `username`, `password`, CSRF | ADMIN `/admin`, 그 외 `/` | `/loginForm?error=true` |
| OAuth2 시작/콜백 | `GET /oauth2/**`, `/login/oauth2/**` | provider별 OAuth2 protocol | `/` | Spring Security OAuth2 실패 처리 |
| logout | `POST /logout` | Session + CSRF | Session 무효화, cookie 삭제, `/` | CSRF 403 |

## Model과 Flash 핵심 계약

- 목록: `totalCount`, `todayCount`, `memberCount`, 선택적 `myPostCount`, `myCommentCount`, `page`, `posts`.
- 상세: `post`, `comments`, `commentForm`, nullable `memberId`.
- post form: `mode`, `form`, `actionUrl`, `submitLabel`.
- my edit: `form`, `nicknameRequest`, `passwordRequest`.
- Redirect flash: 일반 성공/예외 메시지는 주로 `msg`, 댓글 Validation은 `error`.

## 현재 제한

- Controller별 URL prefix가 일관되지 않고 comment route도 `/post/**` 아래에 있다.
- create/reply는 로그인 redirect query를 `addAttribute("redirect", ...)`로 만들지만 로그인 success handler가 이를 소비하지 않는다.
- 활성 `@RestController`는 local 전용 오류 테스트 하나뿐이며 정상 JSON API 계약은 없다.
