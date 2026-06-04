# Board API / Routing Documentation

## 1. 문서 개요

Board는 Spring Boot MVC와 Thymeleaf를 사용하는 SSR 기반 게시판 프로젝트이다. 현재 구현은 JSON REST API 중심이 아니라 Controller가 View 이름 또는 Redirect 문자열을 반환하는 화면 라우팅 중심 구조이다.

- 요청 처리 방식: Spring MVC Controller -> Service -> Repository -> JPA Entity
- 화면 렌더링: Thymeleaf template 반환
- Form 요청: `application/x-www-form-urlencoded` 기반 POST 요청
- 인증 방식: Spring Security form login, OAuth2 login
- 권한 구분: `ROLE_USER`, `ROLE_ADMIN`
- 예외 처리: `CustomException`, `ErrorCode`, `GlobalControllerAdvice`

주요 Controller 목록:

| Controller | 역할 |
|---|---|
| `PostController` | 게시글 목록, 검색, 상세, 작성, 수정, 삭제 |
| `CommentController` | 댓글/대댓글 작성, 댓글 수정, 댓글 삭제 |
| `MemberController` | 회원가입, 로그인 화면 |
| `MyController` | 마이페이지, 내 글/댓글, 회원정보 수정, 탈퇴 |
| `AdminController` | 관리자 대시보드, 게시글/회원 관리 |
| `TestController` | local profile 전용 Discord 예외 테스트 |

## 2. 인증/인가 정책

기준 파일:

- `src/main/java/project/board/global/security/config/SecurityConfig.java`
- `src/main/java/project/board/global/security/user/UnifiedPrincipal.java`
- `src/main/java/project/board/global/security/user/CustomLoginSuccessHandler.java`
- `src/main/java/project/board/global/security/config/oauth/CustomOauth2UserService.java`

### 비로그인 사용자 접근 가능

Security 설정상 다음 요청은 명시적으로 허용된다.

| URL | 설명 |
|---|---|
| `/` | 게시글 목록 |
| `/login` | form login 처리 URL |
| `/loginForm` | 로그인 화면 |
| `/signup` | 회원가입 화면 및 회원가입 요청 |
| `/css/**`, `/js/**`, `/images/**` | 정적 리소스 |
| `/error` | Spring 오류 경로 |
| `/oauth2/**`, `/login/oauth2/**` | OAuth2 로그인 관련 경로 |

`anyRequest().permitAll()` 설정 때문에 위 목록 외 GET 요청도 기본적으로 허용될 수 있다. 단, `/my/**`, `/admin/**`, `POST /post/**`는 별도 제한이 있다.

### 로그인 사용자 접근 가능

| URL | 설명 |
|---|---|
| `/my/**` | 마이페이지, 내 글/댓글, 회원정보 수정, 탈퇴 |
| `POST /post/**` | 게시글 작성/수정/삭제, 댓글 작성/수정/삭제 |

### 관리자만 접근 가능

| URL | 설명 |
|---|---|
| `/admin/**` | 관리자 대시보드, 게시글 관리, 회원 관리 |

`AdminController`에는 `@PreAuthorize("hasRole('ADMIN')")`도 적용되어 있다.

### 인증 실패 시 이동 흐름

- form login 화면: `/loginForm`
- 로그인 처리 URL: `POST /login`
- 로그인 실패: `/loginForm?error=true`
- 로그인 성공:
  - `ROLE_ADMIN`: `/admin`
  - 그 외: `/`
- 로그아웃 처리 URL: `POST /logout`
- 로그아웃 성공: `/`
- 인증이 필요한 URL에 비로그인 사용자가 접근하면 Spring Security 기본 흐름에 따라 로그인 화면으로 이동한다.

### 권한 부족 시 처리 흐름

권한 부족에 대한 별도 `AccessDeniedHandler`는 현재 코드에 없다. Spring Security 기본 AccessDenied 처리 흐름을 따른다.

### OAuth2 로그인 흐름

Spring Security OAuth2 Client를 사용한다.

| Provider | 시작 URL | Callback URL 패턴 | 사용자 정보 처리 |
|---|---|---|---|
| Google | `/oauth2/authorization/google` | `/login/oauth2/code/google` | `GoogleUserInfo` |
| Naver | `/oauth2/authorization/naver` | `/login/oauth2/code/naver` | `NaverUserInfo` |
| Kakao | `/oauth2/authorization/kakao` | `/login/oauth2/code/kakao` | `KakaoUserInfo` |

`CustomOauth2UserService`는 provider와 providerId로 기존 회원을 조회하고, 없으면 `Member.createOAuth(...)`로 소셜 회원을 생성한다. 소셜 회원의 `loginType`은 `SOCIAL`이다.

### Principal / SecurityContext 사용

| 사용 위치 | 사용 방식 |
|---|---|
| Controller | `@AuthenticationPrincipal UnifiedPrincipal` |
| `JpaConfig` | `SecurityContextHolder`로 audit 사용자 nickname 제공 |
| `MyController.refreshAuthentication` | 닉네임 변경 후 `SecurityContextHolder`의 Authentication 재구성 |
| `UnifiedPrincipal` | `UserDetails`, `OAuth2User`를 동시에 구현 |

## 3. 공통 응답/흐름

### 성공 시 View 반환

Controller는 Thymeleaf template 경로를 반환한다.

예:

- `post/list`
- `post/detail`
- `post/form`
- `member/signup`
- `member/loginForm`
- `my/my`
- `admin/index`

### 성공 시 Redirect 반환

Form 처리 성공 후에는 대부분 redirect를 반환한다.

예:

- 회원가입 성공: `redirect:/loginForm`
- 게시글 작성 성공: `redirect:/post/{id}`
- 게시글 수정 성공: `redirect:/post/{id}`
- 게시글 삭제 성공: `redirect:/`
- 댓글 작성/수정/삭제 성공: `redirect:/post/{postId}`
- 관리자 처리 성공: `redirect:/admin`

### Validation 실패 시 동작

| 영역 | 실패 흐름 |
|---|---|
| 회원가입 | `BindingResult` 오류 시 `member/signup` 재렌더링 |
| 게시글 작성 | `BindingResult` 오류 시 `post/form` 반환 |
| 게시글 수정 | `BindingResult` 오류 시 `mode`, `actionUrl`, `submitLabel`을 model에 넣고 `post/form` 반환 |
| 댓글/대댓글 작성 | flash `error` 추가 후 게시글 상세로 redirect |
| 댓글 수정 | flash `error` 추가 후 게시글 상세로 redirect |
| 닉네임 수정 | `my/myEdit` 재렌더링 |
| 비밀번호 변경 | `my/myEdit` 재렌더링 |

### 예외 발생 시 처리 방식

`CustomException`은 `GlobalControllerAdvice`에서 처리한다.

- ErrorCode별 message를 로그로 남긴다.
- 일부 예외는 Discord 알림 대상에서 제외한다.
- `DUPLICATE_EMAIL`, `DUPLICATE_NICKNAME`, `PASSWORD_MISMATCH`는 `member/signup` 화면을 반환하고 `error` model attribute를 사용한다.
- 그 외 예외는 `ErrorCode.redirectUrl`로 redirect한다.
- flash attribute는 현재 `msg` 키를 사용한다.

## 4. API/라우팅 목록

### 회원가입 화면

- Method: `GET`
- URL: `/signup`
- Controller: `MemberController`
- Handler Method: `signupForm`
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `form = new MemberCreateRequest()`
- 반환 View 또는 Redirect: `member/signup`
- 성공 흐름: 회원가입 화면을 렌더링한다.
- 실패 흐름: 별도 처리 없음.
- 관련 DTO: `MemberCreateRequest`
- 관련 Service: 없음
- 관련 Entity: `Member`
- 비고: 이메일, 닉네임, 비밀번호, 비밀번호 확인 입력 폼을 제공한다.

### 회원가입 요청

- Method: `POST`
- URL: `/signup`
- Controller: `MemberController`
- Handler Method: `signup`
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: `nickname`, `email`, `password`, `passwordConfirm`
- Model Attribute: `form`
- 반환 View 또는 Redirect: 성공 시 `redirect:/loginForm`, 검증 실패 시 `member/signup`
- 성공 흐름: `MemberService.signUp`이 중복 이메일/닉네임과 비밀번호 확인을 검사하고 LOCAL 회원을 저장한다.
- 실패 흐름: Bean Validation 실패 시 회원가입 화면 재렌더링. 중복/비밀번호 불일치는 `CustomException` -> `member/signup`.
- 관련 DTO: `MemberCreateRequest`
- 관련 Service: `MemberService`
- 관련 Entity: `Member`
- 비고: 비밀번호는 `BCryptPasswordEncoder`로 암호화한다.

### 로그인 화면

- Method: `GET`
- URL: `/loginForm`
- Controller: `MemberController`
- Handler Method: `loginForm`
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: `error`, `logout`는 Spring Security 흐름에서 화면 표시용으로 사용된다.
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: 없음
- 반환 View 또는 Redirect: `member/loginForm`
- 성공 흐름: 로그인 화면을 렌더링한다.
- 실패 흐름: 별도 처리 없음.
- 관련 DTO: 없음
- 관련 Service: `CustomUserDetailsService`
- 관련 Entity: `Member`
- 비고: Google, Kakao, Naver OAuth2 로그인 링크를 포함한다.

### 로그인 요청

- Method: `POST`
- URL: `/login`
- Controller: 현재 코드에 직접 Controller 없음. Spring Security form login 처리.
- Handler Method: Spring Security `UsernamePasswordAuthenticationFilter`
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: `username`, `password`, optional `redirect`
- Model Attribute: 없음
- 반환 View 또는 Redirect: 성공 시 `CustomLoginSuccessHandler`에 따라 `/admin` 또는 `/`, 실패 시 `/loginForm?error=true`
- 성공 흐름: `CustomUserDetailsService.loadUserByUsername`가 이메일로 회원을 조회하고 인증한다.
- 실패 흐름: 인증 실패 시 `/loginForm?error=true`.
- 관련 DTO: 없음
- 관련 Service: `CustomUserDetailsService`
- 관련 Entity: `Member`
- 비고: `redirect` hidden input은 화면에 있으나 현재 success handler에서 사용하지 않는다.

### 로그아웃 요청

- Method: `POST`
- URL: `/logout`
- Controller: 현재 코드에 직접 Controller 없음. Spring Security logout 처리.
- Handler Method: Spring Security LogoutFilter
- 인증 필요 여부: 일반적으로 로그인 사용자
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: CSRF 사용 시 CSRF token
- Model Attribute: 없음
- 반환 View 또는 Redirect: `redirect:/`
- 성공 흐름: 세션 무효화, `JSESSIONID` 삭제 후 `/`로 이동한다.
- 실패 흐름: Spring Security 기본 처리.
- 관련 DTO: 없음
- 관련 Service: 없음
- 관련 Entity: 없음
- 비고: navbar의 logout form에서 POST 요청을 보낸다.

### OAuth2 로그인

- Method: `GET`
- URL: `/oauth2/authorization/{registrationId}`
- Controller: 현재 코드에 직접 Controller 없음. Spring Security OAuth2 login 처리.
- Handler Method: Spring Security OAuth2 authorization endpoint
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: 없음
- Path Variable: `registrationId = google | kakao | naver`
- Form Data / Request Body: 없음
- Model Attribute: 없음
- 반환 View 또는 Redirect: 외부 OAuth provider 인증 페이지로 redirect
- 성공 흐름: callback 수신 후 `CustomOauth2UserService.loadUser`에서 회원 조회 또는 생성, 기본 성공 URL `/`.
- 실패 흐름: provider 응답 오류 또는 사용자 정보 형식 오류 시 Spring Security OAuth2 예외 흐름.
- 관련 DTO: `OAuthUserInfo`, `GoogleUserInfo`, `NaverUserInfo`, `KakaoUserInfo`
- 관련 Service: `CustomOauth2UserService`
- 관련 Entity: `Member`
- 비고: 실제 Client Secret 값은 문서화하지 않는다.

### 게시글 목록

- Method: `GET`
- URL: `/`
- Controller: `PostController`
- Handler Method: `list`
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: `page`, `size`, `keyword`는 `PageRequestDto` 바인딩 대상이다. 현재 목록 조회에서는 keyword를 사용하지 않는다.
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `totalCount`, `todayCount`, `memberCount`, `myPostCount`, `myCommentCount`, `page`, `posts`
- 반환 View 또는 Redirect: `post/list`
- 성공 흐름: `PostService.findAll`로 게시글 목록을 페이징 조회하고 화면에 전달한다.
- 실패 흐름: 잘못된 `page` 또는 `size`가 들어오면 `PageRequest.of(page - 1, size, sort)`에서 런타임 예외가 발생할 수 있다.
- 관련 DTO: `PageRequestDto`, `PageResultDto`, `PostListResponse`
- 관련 Service: `PostService`, `MemberService`, `CommentService`
- 관련 Entity: `Post`, `Member`, `Comment`
- 비고: 최신순 기준은 `id desc`이다.

### 게시글 검색

- Method: `GET`
- URL: `/posts/search`
- Controller: `PostController`
- Handler Method: `search`
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: `keyword`
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `posts`, `keyword`
- 반환 View 또는 Redirect: `post/list`
- 성공 흐름: `PostRepository.findByTitleContaining(keyword)`로 제목 기준 검색 후 목록 화면을 반환한다.
- 실패 흐름: `keyword`가 누락되면 Spring MVC request parameter binding 오류가 발생할 수 있다.
- 관련 DTO: `PostListResponse`
- 관련 Service: `PostService`
- 관련 Entity: `Post`
- 비고: 현재 코드 기준 검색 대상은 제목만이다. 내용 검색은 현재 코드에 없음.

### 게시글 상세

- Method: `GET`
- URL: `/post/{id}`
- Controller: `PostController`
- Handler Method: `detail`
- 인증 필요 여부: 아니오
- 권한: 전체
- Request Parameter: 없음
- Path Variable: `id`
- Form Data / Request Body: 없음
- Model Attribute: `post`, `comments`, `commentForm`, `memberId`
- 반환 View 또는 Redirect: `post/detail`
- 성공 흐름: 쿠키 `View_Count` 기준으로 12시간 내 중복 조회 여부를 확인하고 조회수를 증가시킨 뒤 게시글과 댓글을 조회한다.
- 실패 흐름: 게시글이 없으면 `POST_NOT_FOUND` -> `/`로 redirect.
- 관련 DTO: `PostDetailsResponse`, `CommentResponse`, `CommentRequestDto`
- 관련 Service: `PostService`
- 관련 Entity: `Post`, `Comment`, `Member`
- 비고: 댓글은 root comment와 replies 구조로 화면에 전달된다.

### 게시글 작성 화면

- Method: `GET`
- URL: `/post/new`
- Controller: `PostController`
- Handler Method: `createForm`
- 인증 필요 여부: Security 설정상 명시 제한 없음
- 권한: 전체 접근 가능 상태. 실제 작성 요청은 로그인 필요.
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `mode`, `form`, `actionUrl`, `submitLabel`
- 반환 View 또는 Redirect: `post/form`
- 성공 흐름: 작성 폼을 렌더링한다.
- 실패 흐름: 별도 처리 없음.
- 관련 DTO: `PostRequest`
- 관련 Service: 없음
- 관련 Entity: `Post`
- 비고: 화면 접근도 로그인 필요로 제한하는 개선 여지가 있다.

### 게시글 작성 요청

- Method: `POST`
- URL: `/post/new`
- Controller: `PostController`
- Handler Method: `create`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: `title`, `content`
- Model Attribute: `form`
- 반환 View 또는 Redirect: 성공 시 `redirect:/post/{id}`, 검증 실패 시 `post/form`
- 성공 흐름: 로그인 사용자 ID로 게시글을 저장하고 상세 화면으로 이동한다.
- 실패 흐름: Bean Validation 실패 시 `post/form`. 로그인 정보가 없으면 Security에서 먼저 차단되어야 한다.
- 관련 DTO: `PostRequest`, `PostListResponse`
- 관련 Service: `PostService`
- 관련 Entity: `Post`, `Member`
- 비고: 실패 시 `mode`, `actionUrl`, `submitLabel`을 다시 넣지 않는다.

### 게시글 수정 화면

- Method: `GET`
- URL: `/post/{id}/edit`
- Controller: `PostController`
- Handler Method: `editForm`
- 인증 필요 여부: Security 설정상 명시 제한 없음
- 권한: 전체 접근 가능 상태
- Request Parameter: 없음
- Path Variable: `id`
- Form Data / Request Body: 없음
- Model Attribute: `mode`, `form`, `actionUrl`, `submitLabel`
- 반환 View 또는 Redirect: `post/form`
- 성공 흐름: 게시글을 조회하고 수정 폼을 렌더링한다.
- 실패 흐름: 게시글이 없으면 `POST_NOT_FOUND` -> `/`.
- 관련 DTO: `PostDetailsResponse`, `PostRequest`
- 관련 Service: `PostService`
- 관련 Entity: `Post`
- 비고: 현재 수정 화면 조회 시 작성자 검증은 없다. 실제 수정 요청에서 작성자 검증을 수행한다.

### 게시글 수정 요청

- Method: `POST`
- URL: `/post/{id}/edit`
- Controller: `PostController`
- Handler Method: `edit`
- 인증 필요 여부: 예
- 권한: 작성자 본인
- Request Parameter: 없음
- Path Variable: `id`
- Form Data / Request Body: `title`, `content`
- Model Attribute: `form`
- 반환 View 또는 Redirect: 성공 시 `redirect:/post/{id}`, 검증 실패 시 `post/form`
- 성공 흐름: `PostService.update`에서 작성자 검증 후 제목/내용을 변경한다.
- 실패 흐름: 검증 실패 시 수정 폼 재렌더링. 작성자가 아니면 `NOT_POST_OWNER` -> `/`.
- 관련 DTO: `PostRequest`
- 관련 Service: `PostService`
- 관련 Entity: `Post`
- 비고: 작성자 검증은 `PostService.validateWriter`에서 수행된다.

### 게시글 삭제 요청

- Method: `POST`
- URL: `/post/{id}/delete`
- Controller: `PostController`
- Handler Method: `delete`
- 인증 필요 여부: 예
- 권한: 작성자 본인
- Request Parameter: 없음
- Path Variable: `id`
- Form Data / Request Body: 없음
- Model Attribute: 없음
- 반환 View 또는 Redirect: `redirect:/`
- 성공 흐름: 댓글을 먼저 삭제한 뒤 게시글을 삭제한다.
- 실패 흐름: 게시글 없음 `POST_NOT_FOUND`, 작성자 불일치 `NOT_POST_OWNER`.
- 관련 DTO: 없음
- 관련 Service: `PostService`
- 관련 Entity: `Post`, `Comment`
- 비고: 댓글 삭제는 `CommentRepository.deleteByPostId` bulk delete로 수행된다.

### 댓글 작성

- Method: `POST`
- URL: `/post/{postId}/comment`
- Controller: `CommentController`
- Handler Method: `create`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: `postId`
- Form Data / Request Body: `content`
- Model Attribute: `commentForm`
- 반환 View 또는 Redirect: `redirect:/post/{postId}`
- 성공 흐름: 로그인 사용자와 게시글을 조회하고 root comment를 저장한다.
- 실패 흐름: 비로그인 사용자는 `/loginForm?redirect=/post/{postId}`로 이동. 검증 실패 시 flash `error`와 함께 상세로 redirect.
- 관련 DTO: `CommentRequestDto`, `CommentResponse`
- 관련 Service: `CommentService`
- 관련 Entity: `Comment`, `Post`, `Member`
- 비고: `POST /post/**`는 Security에서도 인증 필요로 제한된다.

### 대댓글 작성

- Method: `POST`
- URL: `/post/{postId}/comment/{parentId}/replies`
- Controller: `CommentController`
- Handler Method: `createReply`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: `postId`, `parentId`
- Form Data / Request Body: `content`
- Model Attribute: `commentForm`
- 반환 View 또는 Redirect: `redirect:/post/{postId}`
- 성공 흐름: 부모 댓글이 현재 게시글에 속하고 부모가 대댓글이 아닌지 검증한 뒤 reply를 저장한다.
- 실패 흐름: 검증 실패 시 `COMMENT_INVALID_PARENT`, 댓글 없음 `COMMENT_NOT_FOUND`.
- 관련 DTO: `CommentRequestDto`, `CommentResponse`
- 관련 Service: `CommentService`
- 관련 Entity: `Comment`, `Post`, `Member`
- 비고: 대댓글의 대댓글은 허용하지 않는다.

### 댓글 수정

- Method: `POST`
- URL: `/post/{postId}/comment/{commentId}/edit`
- Controller: `CommentController`
- Handler Method: `update`
- 인증 필요 여부: 예
- 권한: 댓글 작성자 본인
- Request Parameter: 없음
- Path Variable: `postId`, `commentId`
- Form Data / Request Body: `content`
- Model Attribute: `commentUpdateForm`
- 반환 View 또는 Redirect: `redirect:/post/{postId}`
- 성공 흐름: 댓글 작성자 검증 후 내용을 변경한다.
- 실패 흐름: 검증 실패 시 flash `error`. 댓글 없음 또는 작성자 불일치 시 `CustomException`.
- 관련 DTO: `CommentRequestDto`, `CommentResponse`
- 관련 Service: `CommentService`
- 관련 Entity: `Comment`
- 비고: 요청 URL은 구현되어 있지만 사용자 요구 확인 목록에는 없던 기능이다.

### 댓글 삭제

- Method: `POST`
- URL: `/post/{postId}/comment/{commentId}/delete`
- Controller: `CommentController`
- Handler Method: `delete`
- 인증 필요 여부: 예
- 권한: 댓글 작성자 본인
- Request Parameter: 없음
- Path Variable: `postId`, `commentId`
- Form Data / Request Body: 없음
- Model Attribute: 없음
- 반환 View 또는 Redirect: `redirect:/post/{postId}`
- 성공 흐름: 댓글 작성자 검증 후 댓글을 삭제한다.
- 실패 흐름: 댓글 없음 `COMMENT_NOT_FOUND`, 작성자 불일치 `COMMENT_NOT_OWNER`.
- 관련 DTO: 없음
- 관련 Service: `CommentService`
- 관련 Entity: `Comment`
- 비고: 부모 댓글에 자식 댓글이 있을 때 삭제 정책은 명시되어 있지 않다.

### 마이페이지

- Method: `GET`
- URL: `/my`
- Controller: `MyController`
- Handler Method: `myForm`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `myPostCount`, `myCommentCount`, `recentPosts`, `recentComments`
- 반환 View 또는 Redirect: `my/my`
- 성공 흐름: 내 게시글/댓글 수와 최근 작성 글/댓글을 조회한다.
- 실패 흐름: 인증 객체가 없으면 `MEMBER_NOT_AUTHENTICATION` -> `/signup`.
- 관련 DTO: `PostRecent`, `MyRecentComment`
- 관련 Service: `PostService`, `CommentService`
- 관련 Entity: `Member`, `Post`, `Comment`
- 비고: `/my/**`는 Security에서 인증 필요로 제한된다.

### 내 게시글 조회

- Method: `GET`
- URL: `/my/posts`
- Controller: `MyController`
- Handler Method: `myPostsForm`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: `page`, `size`, `keyword`
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `myPostCount`, `todayMyPostCount`, `myPostViewCount`, `posts`, `page`, `keyword`
- 반환 View 또는 Redirect: `my/myPost`
- 성공 흐름: 내 게시글 수, 오늘 작성 수, 오늘 내 글 수, 내 게시글 목록을 조회한다.
- 실패 흐름: 인증 객체가 없으면 Security 또는 NPE/예외 가능.
- 관련 DTO: `PostListResponse`, `PageRequestDto`
- 관련 Service: `PostService`
- 관련 Entity: `Post`, `Member`
- 비고: 현재 `page`는 전체 게시글 기준이고, `posts`는 내 게시글 전체 목록이다. 검색 keyword는 내 게시글 조회에 적용되지 않는다.

### 내 댓글 조회

- Method: `GET`
- URL: `/my/comments`
- Controller: `MyController`
- Handler Method: `myCommentForm`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: `page`, `size`, `keyword`
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `pageResponse`
- 반환 View 또는 Redirect: `my/myComment`
- 성공 흐름: 내 댓글 수, 오늘 댓글 수, 최근 7일 댓글 수, 내 댓글 목록을 조회한다.
- 실패 흐름: memberId가 null이면 `MEMBER_NOT_FOUND`.
- 관련 DTO: `MyCommentPageResponse`, `MyCommentResponse`, `PageRequestDto`
- 관련 Service: `CommentService`
- 관련 Entity: `Comment`, `Post`, `Member`
- 비고: 댓글 내용 또는 게시글 제목 기준 검색이 Repository query에 구현되어 있다.

### 회원정보 수정 화면

- Method: `GET`
- URL: `/my/edit`
- Controller: `MyController`
- Handler Method: `EditForm`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `form`, `nicknameRequest`, `passwordRequest`
- 반환 View 또는 Redirect: `my/myEdit`
- 성공 흐름: 현재 회원 profile을 조회하고 수정 폼을 렌더링한다.
- 실패 흐름: 회원이 없으면 `MEMBER_NOT_FOUND`.
- 관련 DTO: `MemberUpdateResponse`, `MemberNicknameUpdateRequest`, `MemberPasswordUpdateRequest`
- 관련 Service: `MemberService`
- 관련 Entity: `Member`
- 비고: `form.passwordChangeable`이 false이면 소셜 회원 비밀번호 변경 영역을 숨긴다.

### 닉네임 수정 요청

- Method: `POST`
- URL: `/my/edit/nickname`
- Controller: `MyController`
- Handler Method: `editNickname`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: `nickname`
- Model Attribute: `nicknameRequest`
- 반환 View 또는 Redirect: 성공 시 `redirect:/`, 검증 실패 시 `my/myEdit`
- 성공 흐름: 닉네임 중복 검사 후 변경하고 현재 Authentication을 갱신한다.
- 실패 흐름: Bean Validation 실패 시 수정 화면 재렌더링. 중복 닉네임은 `DUPLICATE_NICKNAME`.
- 관련 DTO: `MemberNicknameUpdateRequest`, `MemberUpdateResponse`
- 관련 Service: `MemberService`
- 관련 Entity: `Member`
- 비고: `SecurityContextHolder`로 인증 객체를 갱신한다.

### 비밀번호 변경 요청

- Method: `POST`
- URL: `/my/edit/password`
- Controller: `MyController`
- Handler Method: `editPassword`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: `currentPassword`, `newPassword`, `newPasswordConfirm`
- Model Attribute: `passwordRequest`
- 반환 View 또는 Redirect: 성공 시 `redirect:/`, 검증 실패 시 `my/myEdit`
- 성공 흐름: 현재 비밀번호 일치와 새 비밀번호 확인 후 비밀번호를 암호화해 변경한다.
- 실패 흐름: Bean Validation 실패, 현재 비밀번호 불일치 `PASSWORD_INVALID`, 확인 불일치 `PASSWORD_CONFIRM`.
- 관련 DTO: `MemberPasswordUpdateRequest`
- 관련 Service: `MemberService`
- 관련 Entity: `Member`
- 비고: 소셜 회원은 화면에서 비밀번호 변경 폼이 숨겨진다. Service 단에서 loginType 차단 검증은 현재 코드에 없다.

### 회원 탈퇴 화면

- Method: `GET`
- URL: `/my/withdraw`
- Controller: `MyController`
- Handler Method: `withdrawForm`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: 없음
- 반환 View 또는 Redirect: `my/withdraw`
- 성공 흐름: 탈퇴 확인 화면을 렌더링한다.
- 실패 흐름: Spring Security 인증 실패 흐름.
- 관련 DTO: 없음
- 관련 Service: 없음
- 관련 Entity: `Member`
- 비고: 화면에서 확인 문구 검증 JavaScript를 사용한다.

### 회원 탈퇴 요청

- Method: `POST`
- URL: `/my/withdraw`
- Controller: `MyController`
- Handler Method: `withdraw`
- 인증 필요 여부: 예
- 권한: 로그인 사용자
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: `confirmText`는 화면에서 전송될 수 있으나 Controller에서는 사용하지 않는다.
- Model Attribute: 없음
- 반환 View 또는 Redirect: `redirect:/`
- 성공 흐름: 회원 관련 댓글, 회원 게시글의 댓글, 회원 게시글, 회원을 삭제하고 로그아웃 처리한다.
- 실패 흐름: 회원이 없으면 `MEMBER_NOT_FOUND`.
- 관련 DTO: 없음
- 관련 Service: `MemberService`
- 관련 Entity: `Member`, `Post`, `Comment`
- 비고: 서버 측 confirmText 검증은 현재 코드에 없다.

### 관리자 대시보드

- Method: `GET`
- URL: `/admin`
- Controller: `AdminController`
- Handler Method: `index`
- 인증 필요 여부: 예
- 권한: `ROLE_ADMIN`
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `totalPosts`, `totalUsers`
- 반환 View 또는 Redirect: `admin/index`
- 성공 흐름: 전체 게시글 수와 전체 사용자 수를 조회한다.
- 실패 흐름: 비로그인 또는 권한 부족 시 Spring Security 처리.
- 관련 DTO: 없음
- 관련 Service: `PostService`, `MemberService`
- 관련 Entity: `Post`, `Member`
- 비고: SecurityConfig와 `@PreAuthorize`가 모두 적용된다.

### 관리자 게시글 목록

- Method: `GET`
- URL: `/admin/posts`
- Controller: `AdminController`
- Handler Method: `posts`
- 인증 필요 여부: 예
- 권한: `ROLE_ADMIN`
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `posts`
- 반환 View 또는 Redirect: `admin/posts`
- 성공 흐름: 작성자 fetch join으로 전체 게시글 목록을 조회한다.
- 실패 흐름: 권한 부족 시 Spring Security 처리.
- 관련 DTO: 없음
- 관련 Service: `PostService`
- 관련 Entity: `Post`
- 비고: 페이징은 현재 코드에 없음.

### 관리자 게시글 삭제

- Method: `POST`
- URL: `/admin/posts/{postId}/delete`
- Controller: `AdminController`
- Handler Method: `deletePosts`
- 인증 필요 여부: 예
- 권한: `ROLE_ADMIN`
- Request Parameter: 없음
- Path Variable: `postId`
- Form Data / Request Body: 없음
- Model Attribute: 없음
- 반환 View 또는 Redirect: `redirect:/admin`
- 성공 흐름: 게시글 댓글을 삭제한 뒤 게시글을 삭제한다.
- 실패 흐름: 게시글 없음 `POST_NOT_FOUND`.
- 관련 DTO: 없음
- 관련 Service: `PostService`
- 관련 Entity: `Post`, `Comment`
- 비고: 관리자 게시글 삭제 기능은 현재 코드에 있음.

### 관리자 회원 목록

- Method: `GET`
- URL: `/admin/users`
- Controller: `AdminController`
- Handler Method: `members`
- 인증 필요 여부: 예
- 권한: `ROLE_ADMIN`
- Request Parameter: 없음
- Path Variable: 없음
- Form Data / Request Body: 없음
- Model Attribute: `members`
- 반환 View 또는 Redirect: `admin/users`
- 성공 흐름: 전체 회원 목록을 조회한다.
- 실패 흐름: 권한 부족 시 Spring Security 처리.
- 관련 DTO: 없음
- 관련 Service: `MemberService`
- 관련 Entity: `Member`
- 비고: 페이징은 현재 코드에 없음.

### 관리자 회원 권한 변경

- Method: `POST`
- URL: `/admin/users/{memberId}/role`
- Controller: `AdminController`
- Handler Method: `memberUpdate`
- 인증 필요 여부: 예
- 권한: `ROLE_ADMIN`
- Request Parameter: `role`
- Path Variable: `memberId`
- Form Data / Request Body: `role`
- Model Attribute: 없음
- 반환 View 또는 Redirect: `redirect:/admin`
- 성공 흐름: 회원을 조회한 뒤 `Role.valueOf(role)`로 권한을 변경한다.
- 실패 흐름: 회원 없음 `MEMBER_NOT_FOUND`. 잘못된 role 문자열은 `IllegalArgumentException`이 발생할 수 있다.
- 관련 DTO: 없음
- 관련 Service: `MemberService`
- 관련 Entity: `Member`
- 비고: 허용 role 값은 `USER`, `ADMIN`이다.

### 관리자 회원 삭제

- Method: `POST`
- URL: `/admin/users/{memberId}/delete`
- Controller: `AdminController`
- Handler Method: `memberDelete`
- 인증 필요 여부: 예
- 권한: `ROLE_ADMIN`
- Request Parameter: 없음
- Path Variable: `memberId`
- Form Data / Request Body: 없음
- Model Attribute: 없음
- 반환 View 또는 Redirect: `redirect:/admin`
- 성공 흐름: 회원 관련 댓글, 회원 게시글의 댓글, 회원 게시글, 회원을 삭제한다.
- 실패 흐름: 회원 없음 `MEMBER_NOT_FOUND`.
- 관련 DTO: 없음
- 관련 Service: `MemberService`
- 관련 Entity: `Member`, `Post`, `Comment`
- 비고: 자기 자신 삭제 방지 로직은 현재 코드에 없음.

### 에러 처리

- Method: N/A
- URL: N/A
- Controller: `GlobalControllerAdvice`
- Handler Method: `customException`
- 인증 필요 여부: N/A
- 권한: N/A
- Request Parameter: N/A
- Path Variable: N/A
- Form Data / Request Body: N/A
- Model Attribute: `error`, `form`, flash `msg`
- 반환 View 또는 Redirect: 일부 회원가입 예외는 `member/signup`, 그 외 `redirect:{ErrorCode.redirectUrl}`
- 성공 흐름: N/A
- 실패 흐름: `CustomException`을 SSR 흐름에 맞게 view 또는 redirect로 변환한다.
- 관련 DTO: `MemberCreateRequest`
- 관련 Service: `DiscordNotifier`
- 관련 Entity: 없음
- 비고: 일반 `Exception`, `AccessDeniedException`, validation binding 예외의 전용 handler는 현재 코드에 없음.

### 현재 코드에 없는 기능

| 기능 | 상태 |
|---|---|
| JSON REST API 응답 표준 | 현재 코드에 없음 |
| Swagger/OpenAPI 문서 | 현재 코드에 없음 |
| Spring REST Docs | 현재 코드에 없음 |
| 게시글 카테고리/게시판 분리 | 현재 코드에 없음 |
| 좋아요 | 현재 코드에 없음 |
| 파일 첨부 | 현재 코드에 없음 |
| 신고 | 현재 코드에 없음 |
| 실시간 알림 | 현재 코드에 없음 |
| 관리자 통계 상세 | 현재 코드에 없음 |

## 5. API 개선 제안

### URL 네이밍

- 현재 `/post/{id}`와 `/posts/search`가 혼재되어 있다. SSR 유지 시에도 `/posts`, `/posts/{id}`, `/posts/new`처럼 복수형 기준으로 통일하면 좋다.
- 마이페이지는 `/my/posts`, `/my/comments`가 명확하므로 유지해도 좋다.
- 관리자 URL은 `/admin/posts`, `/admin/users`로 구분되어 있어 유지 가능하다.

### Controller 책임 분리

- `PostController`의 조회수 쿠키 처리는 별도 component 또는 service로 분리할 수 있다.
- `MyController`의 인증 객체 갱신 로직은 security service로 분리할 수 있다.
- `AdminController`는 기능이 커지면 `AdminPostController`, `AdminMemberController`로 나누는 편이 좋다.

### REST API 전환 시 고려사항

- View 반환 Controller와 JSON API Controller를 분리한다.
- 공통 응답 형식과 공통 오류 응답 형식을 정의한다.
- form validation 실패 응답을 JSON으로 내려줄 별도 `@RestControllerAdvice`가 필요하다.
- `PostDetailsResponse`, `CommentResponse`는 API DTO로 재사용 가능하지만 Lazy loading과 N+1을 피하도록 조회 쿼리를 분리해야 한다.

### SSR 구조에서 유지해도 되는 부분

- 회원가입, 로그인, 게시글 작성/수정처럼 form submit 중심의 흐름은 SSR에 잘 맞는다.
- `RedirectAttributes`를 통한 완료 메시지는 현재 프로젝트 규모에서는 충분히 단순하다.

### Form 요청과 API 요청 분리 필요 영역

- 댓글 작성/수정/삭제는 화면 내 비동기 UX로 바꾸려면 API 분리가 효과적이다.
- 관리자 권한 변경/삭제는 API로 분리하면 confirm modal과 결과 메시지를 다루기 쉽다.
- 게시글 검색은 SSR 검색 유지 또는 REST 검색 API 전환 둘 다 가능하다.

### 예외 응답 구조

- 현재 ErrorCode가 redirect URL과 message를 함께 가진다. API 전환 시에는 `code`, `message`, `fieldErrors`, `timestamp` 같은 응답 구조가 필요하다.
- SSR에서도 flash key를 `msg`와 `error`로 명확히 분리하는 것이 좋다.

### 인증/인가 처리

- `GET /post/new`, `GET /post/{id}/edit`도 인증 필요로 제한하는 것이 좋다.
- 수정 화면 조회 시 작성자 검증을 수행하는 것이 좋다.
- 관리자 회원 삭제에서는 자기 자신 삭제 방지, 마지막 관리자 삭제 방지 정책을 추가할 수 있다.
