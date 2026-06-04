# Board 요구사항 정의서

## 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 프로젝트명 | Board |
| 프로젝트 목적 | Spring Boot MVC, Thymeleaf, JPA, Spring Security를 활용한 SSR 게시판 구현 |
| 주요 사용자 | 비회원, 일반 회원, OAuth2 회원, 관리자 |
| 핵심 기능 | 회원가입/로그인, OAuth2 로그인, 게시글 CRUD, 댓글/대댓글, 마이페이지, 관리자 관리 기능 |
| 기술 스택 | Java 17, Spring Boot 4, Spring MVC, Thymeleaf, Spring Data JPA, Spring Security, OAuth2 Client, MySQL, Bootstrap |
| 프로젝트 유형 | Thymeleaf 기반 SSR 게시판 프로젝트 |

현재 프로젝트는 JSON REST API보다 화면 라우팅과 Form POST를 중심으로 동작한다. Controller는 View 이름 또는 Redirect 문자열을 반환하고, Service 계층에서 트랜잭션과 도메인 처리를 수행한다.

## 2. 사용자 유형

| 사용자 유형 | 설명 | 가능한 기능 |
|---|---|---|
| 비회원 | 로그인하지 않은 사용자 | 게시글 목록 조회, 게시글 검색, 게시글 상세 조회, 회원가입, 로그인, OAuth2 로그인 |
| 일반 회원 | form login으로 가입한 LOCAL 회원 | 비회원 기능, 게시글 작성/수정/삭제, 댓글/대댓글 작성/수정/삭제, 마이페이지, 닉네임 수정, 비밀번호 변경, 회원 탈퇴 |
| OAuth2 회원 | Google/Naver/Kakao로 로그인한 SOCIAL 회원 | 일반 회원 기능 중 비밀번호 변경 제외, 닉네임 수정, 게시글/댓글 기능, 마이페이지 |
| 관리자 | `ROLE_ADMIN` 권한 회원 | 일반 회원 기능, 관리자 대시보드, 게시글 관리, 회원 목록 조회, 회원 권한 변경, 회원 삭제 |

## 3. 기능 요구사항

### REQ-MEMBER-001 회원가입

- 구분: 회원
- 우선순위: 높음
- 사용자: 비회원
- 설명:
  - 사용자는 닉네임, 이메일, 비밀번호, 비밀번호 확인을 입력해 회원가입할 수 있다.
- 입력:
  - `nickname`
  - `email`
  - `password`
  - `passwordConfirm`
- 처리 조건:
  - 닉네임은 2~12자, 영문/숫자/한글만 허용한다.
  - 비밀번호는 8~20자, 영문과 숫자를 각각 1개 이상 포함해야 한다.
  - 이메일은 이메일 형식이어야 한다.
  - 이메일과 닉네임은 중복될 수 없다.
  - 비밀번호와 비밀번호 확인이 일치해야 한다.
- 결과:
  - LOCAL 타입 회원을 생성하고 `/loginForm`으로 redirect한다.
- 예외/실패 조건:
  - Bean Validation 실패 시 `member/signup`을 다시 반환한다.
  - 중복 이메일, 중복 닉네임, 비밀번호 불일치는 `CustomException`으로 처리하고 회원가입 화면에 `error`를 전달한다.
- 관련 코드:
  - `MemberController`
  - `MemberService`
  - `MemberCreateRequest`
  - `MemberRepository`
  - `Member`

### REQ-MEMBER-002 로그인

- 구분: 회원
- 우선순위: 높음
- 사용자: 비회원
- 설명:
  - 사용자는 이메일과 비밀번호로 로그인할 수 있다.
- 입력:
  - `username`
  - `password`
- 처리 조건:
  - `CustomUserDetailsService`가 이메일로 회원을 조회한다.
  - 비밀번호는 `BCryptPasswordEncoder`로 검증한다.
- 결과:
  - 일반 회원은 `/`, 관리자는 `/admin`으로 이동한다.
- 예외/실패 조건:
  - 인증 실패 시 `/loginForm?error=true`로 이동한다.
- 관련 코드:
  - `SecurityConfig`
  - `CustomUserDetailsService`
  - `CustomLoginSuccessHandler`
  - `UnifiedPrincipal`

### REQ-MEMBER-003 로그아웃

- 구분: 회원
- 우선순위: 높음
- 사용자: 일반 회원, OAuth2 회원, 관리자
- 설명:
  - 로그인 사용자는 로그아웃할 수 있다.
- 입력:
  - `POST /logout`
- 처리 조건:
  - Spring Security logout 설정을 사용한다.
  - 세션을 무효화하고 `JSESSIONID`를 삭제한다.
- 결과:
  - `/`로 redirect한다.
- 예외/실패 조건:
  - 별도 Custom handler는 현재 코드에 없다.
- 관련 코드:
  - `SecurityConfig`
  - `fragments/navbar.html`

### REQ-MEMBER-004 OAuth2 로그인

- 구분: 회원
- 우선순위: 높음
- 사용자: 비회원
- 설명:
  - 사용자는 Google, Naver, Kakao 계정으로 로그인할 수 있다.
- 입력:
  - `/oauth2/authorization/google`
  - `/oauth2/authorization/naver`
  - `/oauth2/authorization/kakao`
- 처리 조건:
  - provider와 providerId로 기존 회원을 조회한다.
  - 기존 회원이 없으면 SOCIAL 타입 회원을 생성한다.
  - provider별 사용자 정보 파서는 `GoogleUserInfo`, `NaverUserInfo`, `KakaoUserInfo`를 사용한다.
- 결과:
  - 로그인 성공 후 `/`로 이동한다.
- 예외/실패 조건:
  - 지원하지 않는 provider 또는 provider 응답 구조 오류 시 `OAuth2AuthenticationException`이 발생한다.
- 관련 코드:
  - `SecurityConfig`
  - `CustomOauth2UserService`
  - `OAuthUserInfo`
  - `Member`

### REQ-MEMBER-005 회원정보 수정

- 구분: 회원
- 우선순위: 중간
- 사용자: 일반 회원, OAuth2 회원
- 설명:
  - 로그인 사용자는 닉네임을 수정할 수 있다.
- 입력:
  - `nickname`
- 처리 조건:
  - 닉네임 형식을 검증한다.
  - 다른 회원이 사용하는 닉네임으로 변경할 수 없다.
  - 변경 후 현재 Authentication의 nickname을 갱신한다.
- 결과:
  - `/`로 redirect한다.
- 예외/실패 조건:
  - 검증 실패 시 `my/myEdit`을 다시 반환한다.
  - 중복 닉네임이면 `DUPLICATE_NICKNAME`.
- 관련 코드:
  - `MyController`
  - `MemberService`
  - `MemberNicknameUpdateRequest`
  - `SecurityContextHolder`

### REQ-MEMBER-006 비밀번호 변경

- 구분: 회원
- 우선순위: 중간
- 사용자: 일반 회원
- 설명:
  - LOCAL 회원은 현재 비밀번호를 확인한 뒤 새 비밀번호로 변경할 수 있다.
- 입력:
  - `currentPassword`
  - `newPassword`
  - `newPasswordConfirm`
- 처리 조건:
  - 현재 비밀번호가 DB의 암호화된 비밀번호와 일치해야 한다.
  - 새 비밀번호와 확인 값이 일치해야 한다.
  - 새 비밀번호는 정규식 조건을 만족해야 한다.
- 결과:
  - 비밀번호를 BCrypt로 암호화해 변경하고 `/`로 redirect한다.
- 예외/실패 조건:
  - 검증 실패 시 `my/myEdit`.
  - 현재 비밀번호 불일치 시 `PASSWORD_INVALID`.
  - 새 비밀번호 확인 불일치 시 `PASSWORD_CONFIRM`.
- 관련 코드:
  - `MyController`
  - `MemberService`
  - `MemberPasswordUpdateRequest`

### REQ-MEMBER-007 소셜 로그인 회원의 비밀번호 변경 제한

- 구분: 회원
- 우선순위: 중간
- 사용자: OAuth2 회원
- 설명:
  - SOCIAL 회원은 서비스 내에서 비밀번호를 변경할 수 없다.
- 입력:
  - 없음
- 처리 조건:
  - `MemberUpdateResponse.passwordChangeable`이 `false`이면 화면에서 비밀번호 변경 폼을 숨긴다.
- 결과:
  - 소셜 계정에서 비밀번호를 변경하라는 안내를 보여준다.
- 예외/실패 조건:
  - Service 단에서 `loginType` 기반 변경 차단은 현재 코드에 없다.
- 관련 코드:
  - `MemberService.getMyProfile`
  - `my/myEdit.html`

### REQ-POST-001 게시글 목록 조회

- 구분: 게시글
- 우선순위: 높음
- 사용자: 비회원/회원/관리자
- 설명:
  - 사용자는 게시글 목록을 조회할 수 있다.
- 입력:
  - `page`
  - `size`
- 처리 조건:
  - `id desc` 기준으로 조회한다.
  - `PostRepository.findAllWithMember`로 작성자 정보를 fetch join한다.
  - 기본 page는 1, size는 5이다.
- 결과:
  - `post/list` 화면을 반환한다.
- 예외/실패 조건:
  - `page`가 0 이하이거나 `size`가 부적절하면 `PageRequest.of`에서 런타임 예외가 발생할 수 있다.
- 관련 코드:
  - `PostController`
  - `PostService`
  - `PostRepository`
  - `PageRequestDto`
  - `PageResultDto`

### REQ-POST-002 게시글 검색

- 구분: 게시글
- 우선순위: 중간
- 사용자: 비회원/회원/관리자
- 설명:
  - 사용자는 게시글 제목 기준으로 검색할 수 있다.
- 입력:
  - `keyword`
- 처리 조건:
  - 현재 코드 기준 검색 대상은 제목이다.
  - 페이징은 적용되어 있지 않다.
- 결과:
  - 검색 결과를 `post/list` 화면에 표시한다.
- 예외/실패 조건:
  - `keyword` 파라미터가 누락되면 Spring MVC binding 오류가 발생할 수 있다.
- 관련 코드:
  - `PostController.search`
  - `PostService.search`
  - `PostRepository.findByTitleContaining`

### REQ-POST-003 게시글 상세 조회

- 구분: 게시글
- 우선순위: 높음
- 사용자: 비회원/회원/관리자
- 설명:
  - 사용자는 게시글 상세 내용과 댓글 목록을 조회할 수 있다.
- 입력:
  - `postId`
- 처리 조건:
  - 쿠키 `View_Count`에 게시글 ID token이 없으면 조회수를 증가시킨다.
  - 댓글은 root comment와 replies 형태로 조회한다.
- 결과:
  - `post/detail` 화면을 반환한다.
- 예외/실패 조건:
  - 게시글이 없으면 `POST_NOT_FOUND`.
- 관련 코드:
  - `PostController.detail`
  - `PostService.findOne`
  - `CommentResponse`
  - `PostDetailsResponse`

### REQ-POST-004 게시글 작성

- 구분: 게시글
- 우선순위: 높음
- 사용자: 로그인 사용자
- 설명:
  - 로그인 사용자는 제목과 내용을 입력해 게시글을 작성할 수 있다.
- 입력:
  - `title`
  - `content`
- 처리 조건:
  - title/content는 blank일 수 없다.
  - 작성자는 로그인 사용자이다.
- 결과:
  - 생성된 게시글 상세로 redirect한다.
- 예외/실패 조건:
  - Validation 실패 시 `post/form`.
  - 회원을 찾지 못하면 `LOGIN_REQUIRED`.
- 관련 코드:
  - `PostController.create`
  - `PostService.save`
  - `PostRequest`
  - `Post`

### REQ-POST-005 게시글 수정

- 구분: 게시글
- 우선순위: 높음
- 사용자: 게시글 작성자
- 설명:
  - 작성자는 본인 게시글의 제목과 내용을 수정할 수 있다.
- 입력:
  - `postId`
  - `title`
  - `content`
- 처리 조건:
  - 작성자 본인인지 검증한다.
  - title/content는 blank일 수 없다.
- 결과:
  - 수정된 게시글 상세로 redirect한다.
- 예외/실패 조건:
  - 작성자 불일치 시 `NOT_POST_OWNER`.
  - 게시글 없음 시 `POST_NOT_FOUND`.
- 관련 코드:
  - `PostController.edit`
  - `PostService.update`
  - `Post.change`

### REQ-POST-006 게시글 삭제

- 구분: 게시글
- 우선순위: 높음
- 사용자: 게시글 작성자
- 설명:
  - 작성자는 본인 게시글을 삭제할 수 있다.
- 입력:
  - `postId`
- 처리 조건:
  - 작성자 본인인지 검증한다.
  - 게시글 댓글을 먼저 bulk delete한다.
- 결과:
  - `/`로 redirect한다.
- 예외/실패 조건:
  - 작성자 불일치 시 `NOT_POST_OWNER`.
  - 게시글 없음 시 `POST_NOT_FOUND`.
- 관련 코드:
  - `PostController.delete`
  - `PostService.delete`
  - `CommentRepository.deleteByPostId`

### REQ-POST-007 조회수 증가

- 구분: 게시글
- 우선순위: 중간
- 사용자: 비회원/회원/관리자
- 설명:
  - 게시글 상세 조회 시 조회수를 증가시킨다.
- 입력:
  - `postId`
  - `View_Count` cookie
- 처리 조건:
  - 쿠키에 `|postId|` token이 없을 때만 증가한다.
  - 쿠키 유지 시간은 12시간이다.
  - DB update query로 viewCount를 증가시킨다.
- 결과:
  - 조회수가 증가된 게시글 상세를 보여준다.
- 예외/실패 조건:
  - update row count가 0이면 `POST_NOT_FOUND`.
- 관련 코드:
  - `PostController.increaseViewCount`
  - `PostService.viewCount`
  - `PostRepository.incrementViewCount`

### REQ-COMMENT-001 댓글 작성

- 구분: 댓글
- 우선순위: 높음
- 사용자: 로그인 사용자
- 설명:
  - 로그인 사용자는 게시글에 댓글을 작성할 수 있다.
- 입력:
  - `postId`
  - `content`
- 처리 조건:
  - content는 blank일 수 없다.
  - 회원과 게시글이 존재해야 한다.
- 결과:
  - 게시글 상세로 redirect한다.
- 예외/실패 조건:
  - Validation 실패 시 flash `error`.
  - 회원 없음 `MEMBER_NOT_FOUND`, 게시글 없음 `POST_NOT_FOUND`.
- 관련 코드:
  - `CommentController.create`
  - `CommentService.create`
  - `CommentRequestDto`

### REQ-COMMENT-002 댓글 삭제

- 구분: 댓글
- 우선순위: 높음
- 사용자: 댓글 작성자
- 설명:
  - 댓글 작성자는 본인 댓글을 삭제할 수 있다.
- 입력:
  - `postId`
  - `commentId`
- 처리 조건:
  - 댓글 작성자와 로그인 사용자가 일치해야 한다.
- 결과:
  - 게시글 상세로 redirect한다.
- 예외/실패 조건:
  - 댓글 없음 `COMMENT_NOT_FOUND`.
  - 작성자 불일치 `COMMENT_NOT_OWNER`.
  - 부모 댓글에 대댓글이 있을 때의 삭제 정책은 현재 명시되어 있지 않다.
- 관련 코드:
  - `CommentController.delete`
  - `CommentService.delete`

### REQ-COMMENT-003 게시글 상세에서 댓글 조회

- 구분: 댓글
- 우선순위: 높음
- 사용자: 비회원/회원/관리자
- 설명:
  - 게시글 상세 화면에서 댓글과 대댓글을 조회할 수 있다.
- 입력:
  - `postId`
- 처리 조건:
  - `Post.comments`에서 root comment만 필터링한다.
  - 각 root comment의 children을 replies로 변환한다.
- 결과:
  - `post/detail`에 `comments` model attribute를 전달한다.
- 예외/실패 조건:
  - 게시글 없음 `POST_NOT_FOUND`.
- 관련 코드:
  - `PostService.findOne`
  - `CommentResponse`

### REQ-COMMENT-004 내 댓글 목록 조회

- 구분: 댓글
- 우선순위: 중간
- 사용자: 로그인 사용자
- 설명:
  - 로그인 사용자는 자신이 작성한 댓글 목록을 조회할 수 있다.
- 입력:
  - `page`
  - `size`
  - `keyword`
- 처리 조건:
  - 댓글 내용 또는 게시글 제목으로 검색할 수 있다.
  - `id desc` 기준으로 페이징한다.
- 결과:
  - `my/myComment` 화면을 반환한다.
- 예외/실패 조건:
  - memberId가 null이면 `MEMBER_NOT_FOUND`.
- 관련 코드:
  - `MyController.myCommentForm`
  - `CommentService.myCommentPage`
  - `CommentRepository.findMyComments`

### REQ-MY-001 내 정보 조회

- 구분: 마이페이지
- 우선순위: 높음
- 사용자: 로그인 사용자
- 설명:
  - 사용자는 내 정보와 활동 요약을 볼 수 있다.
- 입력:
  - 로그인 사용자 정보
- 처리 조건:
  - 로그인 사용자 ID로 게시글/댓글 수와 최근 활동을 조회한다.
- 결과:
  - `my/my` 화면을 반환한다.
- 예외/실패 조건:
  - 인증 정보가 없으면 `MEMBER_NOT_AUTHENTICATION`.
- 관련 코드:
  - `MyController.myForm`
  - `PostService`
  - `CommentService`

### REQ-MY-002 내가 작성한 게시글 수 조회

- 구분: 마이페이지
- 우선순위: 중간
- 사용자: 로그인 사용자
- 설명:
  - 사용자는 자신이 작성한 게시글 수를 볼 수 있다.
- 입력:
  - `memberId`
- 처리 조건:
  - `PostRepository.countMyPosts`를 사용한다.
- 결과:
  - `myPostCount` model attribute로 전달한다.
- 예외/실패 조건:
  - memberId가 null이면 `MEMBER_NOT_FOUND`.
- 관련 코드:
  - `PostService.myPostCount`

### REQ-MY-003 내가 작성한 댓글 수 조회

- 구분: 마이페이지
- 우선순위: 중간
- 사용자: 로그인 사용자
- 설명:
  - 사용자는 자신이 작성한 댓글 수를 볼 수 있다.
- 입력:
  - `memberId`
- 처리 조건:
  - `CommentRepository.countByMemberId`를 사용한다.
- 결과:
  - `myCommentCount` 또는 `pageResponse.myCommentCount`로 전달한다.
- 예외/실패 조건:
  - `myCommentCount` 자체에는 null 방어가 없다.
- 관련 코드:
  - `CommentService.myCommentCount`

### REQ-MY-004 최근 작성 게시글 조회

- 구분: 마이페이지
- 우선순위: 중간
- 사용자: 로그인 사용자
- 설명:
  - 사용자는 최근 작성한 게시글 최대 5개를 볼 수 있다.
- 입력:
  - `memberId`
- 처리 조건:
  - `PageRequest.of(0, 5)`를 사용한다.
  - DTO projection으로 id, title, viewCount, createdAt을 조회한다.
- 결과:
  - `recentPosts`로 전달한다.
- 예외/실패 조건:
  - 별도 null 방어는 없다.
- 관련 코드:
  - `PostService.recentPosts`
  - `PostRepository.findMyRecentPosts`

### REQ-MY-005 최근 작성 댓글 조회

- 구분: 마이페이지
- 우선순위: 중간
- 사용자: 로그인 사용자
- 설명:
  - 사용자는 최근 작성한 댓글 최대 5개를 볼 수 있다.
- 입력:
  - `memberId`
- 처리 조건:
  - `PageRequest.of(0, 5)`를 사용한다.
  - DTO projection으로 id, title, content, createdAt을 조회한다.
- 결과:
  - `recentComments`로 전달한다.
- 예외/실패 조건:
  - `MyRecentComment.id`는 현재 query상 comment id이다. 화면 링크에서 post id로 사용하면 정합성 문제가 생길 수 있다.
- 관련 코드:
  - `CommentService.recentComments`
  - `CommentRepository.findRecentComments`

### REQ-ADMIN-001 관리자 대시보드

- 구분: 관리자
- 우선순위: 높음
- 사용자: 관리자
- 설명:
  - 관리자는 전체 게시글 수와 전체 사용자 수를 확인할 수 있다.
- 입력:
  - 없음
- 처리 조건:
  - `ROLE_ADMIN` 권한이 필요하다.
- 결과:
  - `admin/index` 화면을 반환한다.
- 예외/실패 조건:
  - 권한 부족 시 Spring Security 기본 처리.
- 관련 코드:
  - `AdminController.index`
  - `PostService.count`
  - `MemberService.countMember`

### REQ-ADMIN-002 회원 목록 조회

- 구분: 관리자
- 우선순위: 높음
- 사용자: 관리자
- 설명:
  - 관리자는 전체 회원 목록을 조회할 수 있다.
- 입력:
  - 없음
- 처리 조건:
  - `ROLE_ADMIN` 권한이 필요하다.
  - 현재 페이징은 없다.
- 결과:
  - `admin/users` 화면을 반환한다.
- 예외/실패 조건:
  - 권한 부족 시 Spring Security 기본 처리.
- 관련 코드:
  - `AdminController.members`
  - `MemberService.findAllForAdmin`

### REQ-ADMIN-003 회원 권한 변경

- 구분: 관리자
- 우선순위: 높음
- 사용자: 관리자
- 설명:
  - 관리자는 회원의 권한을 `USER` 또는 `ADMIN`으로 변경할 수 있다.
- 입력:
  - `memberId`
  - `role`
- 처리 조건:
  - `Role.valueOf(role)`로 enum을 변환한다.
- 결과:
  - `/admin`으로 redirect한다.
- 예외/실패 조건:
  - 회원 없음 `MEMBER_NOT_FOUND`.
  - 잘못된 role 문자열은 `IllegalArgumentException`이 발생할 수 있다.
- 관련 코드:
  - `AdminController.memberUpdate`
  - `MemberService.roleChange`

### REQ-ADMIN-004 회원 삭제

- 구분: 관리자
- 우선순위: 높음
- 사용자: 관리자
- 설명:
  - 관리자는 회원을 삭제할 수 있다.
- 입력:
  - `memberId`
- 처리 조건:
  - 회원 댓글, 회원 게시글의 댓글, 회원 게시글, 회원 순서로 삭제한다.
- 결과:
  - `/admin`으로 redirect한다.
- 예외/실패 조건:
  - 회원 없음 `MEMBER_NOT_FOUND`.
  - 자기 자신 삭제 방지나 마지막 관리자 삭제 방지는 현재 코드에 없다.
- 관련 코드:
  - `AdminController.memberDelete`
  - `MemberService.deleteForAdmin`

### REQ-ADMIN-005 관리자 권한 검증

- 구분: 관리자
- 우선순위: 높음
- 사용자: 관리자
- 설명:
  - `/admin/**`는 관리자만 접근할 수 있어야 한다.
- 입력:
  - 요청 URL
- 처리 조건:
  - `SecurityConfig`에서 `/admin/**`에 `hasRole("ADMIN")` 적용.
  - `AdminController`에 `@PreAuthorize("hasRole('ADMIN')")` 적용.
- 결과:
  - 관리자만 관리자 화면을 사용할 수 있다.
- 예외/실패 조건:
  - 권한 부족 시 Spring Security 기본 처리.
- 관련 코드:
  - `SecurityConfig`
  - `AdminController`

### REQ-EXCEPTION-001 존재하지 않는 회원

- 구분: 예외 처리
- 우선순위: 높음
- 사용자: 전체
- 설명:
  - 존재하지 않는 회원 접근은 `MEMBER_NOT_FOUND`로 처리한다.
- 입력:
  - `memberId`
- 처리 조건:
  - `MemberService.validateMember` 또는 Repository 조회에서 예외를 던진다.
- 결과:
  - 기본 redirect URL `/`로 이동한다.
- 예외/실패 조건:
  - flash `msg`에 메시지가 담긴다.
- 관련 코드:
  - `MemberService`
  - `ErrorCode`
  - `GlobalControllerAdvice`

### REQ-EXCEPTION-002 존재하지 않는 게시글

- 구분: 예외 처리
- 우선순위: 높음
- 사용자: 전체
- 설명:
  - 존재하지 않는 게시글 접근은 `POST_NOT_FOUND`로 처리한다.
- 입력:
  - `postId`
- 처리 조건:
  - `PostService.getPost` 또는 조회수 update 결과가 0이면 예외를 던진다.
- 결과:
  - `/`로 redirect한다.
- 관련 코드:
  - `PostService`
  - `ErrorCode`

### REQ-EXCEPTION-003 존재하지 않는 댓글

- 구분: 예외 처리
- 우선순위: 높음
- 사용자: 로그인 사용자
- 설명:
  - 존재하지 않는 댓글 수정/삭제는 `COMMENT_NOT_FOUND`로 처리한다.
- 입력:
  - `commentId`
- 처리 조건:
  - 댓글 Repository 조회 실패 시 예외를 던진다.
- 결과:
  - 기본은 `/`, 일부 수정/삭제 흐름은 `/post/{postId}`로 redirect한다.
- 관련 코드:
  - `CommentService`
  - `ErrorCode`

### REQ-EXCEPTION-004 인증되지 않은 사용자

- 구분: 예외 처리
- 우선순위: 높음
- 사용자: 비회원
- 설명:
  - 인증이 필요한 요청은 로그인해야 한다.
- 입력:
  - 보호된 URL
- 처리 조건:
  - Spring Security가 `/my/**`, `POST /post/**`, `/admin/**`를 보호한다.
- 결과:
  - 로그인 화면으로 이동한다.
- 관련 코드:
  - `SecurityConfig`

### REQ-EXCEPTION-005 권한 없는 사용자

- 구분: 예외 처리
- 우선순위: 높음
- 사용자: 로그인 사용자
- 설명:
  - 게시글/댓글 수정 삭제는 소유자만 가능하다.
- 입력:
  - `memberId`, `postId`, `commentId`
- 처리 조건:
  - 게시글은 `PostService.validateWriter`.
  - 댓글은 `CommentService.validateOwner`.
- 결과:
  - 게시글은 `NOT_POST_OWNER`, 댓글은 `COMMENT_NOT_OWNER`.
- 관련 코드:
  - `PostService`
  - `CommentService`

### REQ-EXCEPTION-006 잘못된 요청값

- 구분: 예외 처리
- 우선순위: 중간
- 사용자: 전체
- 설명:
  - Bean Validation이 적용된 DTO는 입력값을 검증한다.
- 입력:
  - 회원가입, 게시글, 댓글, 닉네임, 비밀번호 form 값
- 처리 조건:
  - `@Valid`와 `BindingResult`를 사용한다.
- 결과:
  - 화면 재렌더링 또는 redirect 후 flash error.
- 예외/실패 조건:
  - page/size, role 문자열처럼 일부 값은 전용 방어 코드가 부족하다.
- 관련 코드:
  - `MemberCreateRequest`
  - `PostRequest`
  - `CommentRequestDto`
  - `MemberNicknameUpdateRequest`
  - `MemberPasswordUpdateRequest`

## 4. 비기능 요구사항

| 항목 | 현재 충족 상태 | 개선 필요 여부 |
|---|---|---|
| 보안 | Spring Security, BCrypt, 권한 기반 URL 제한 적용 | GET 작성/수정 화면 보호, 관리자 자기 삭제 방지 필요 |
| 인증/인가 | form login, OAuth2 login, `ROLE_ADMIN` 지원 | AccessDenied 처리 커스터마이징 권장 |
| 성능 | 목록 작성자 fetch join, 조회수 update query, 일부 DTO projection 적용 | 상세 댓글 N+1, 검색/관리자 페이징 개선 필요 |
| 유지보수성 | 도메인별 패키지 구조 양호 | Controller 책임 분리, 주석 처리된 DTO 정리 필요 |
| 확장성 | Service/Repository 계층 분리 | REST API 분리 시 응답/예외 표준화 필요 |
| 로깅 | Slf4j, P6Spy Formatter, DiscordNotifier 존재 | 민감 정보 로그 방지 정책 문서화 필요 |
| 예외 처리 | `CustomException`, `ErrorCode`, `GlobalControllerAdvice` 사용 | 일반 Exception, AccessDenied, API 오류 응답 보강 필요 |
| 테스트 | 테스트 파일 존재 | 현재 테스트 컴파일 실패, 최신 코드 반영 필요 |
| 배포 환경 | Dockerfile, docker-compose 파일 존재 | 프로필/환경변수 예시, CI/CD 문서화 필요 |
| 데이터 무결성 | FK 기반 연관관계, 일부 unique 제약 | 댓글 대댓글 삭제 정책, soft delete 검토 필요 |

## 5. 화면 요구사항

### 게시글 목록 화면

- URL: `/`
- 접근 권한: 전체
- 주요 기능: 게시글 목록, 검색 form, 작성 이동, 로그인 사용자 요약
- 전달받는 Model Attribute: `totalCount`, `todayCount`, `memberCount`, `myPostCount`, `myCommentCount`, `page`, `posts`
- 연결되는 Controller: `PostController.list`
- 연결되는 Template 파일: `src/main/resources/templates/post/list.html`
- 성공 흐름: 게시글 목록 렌더링
- 실패 흐름: page/size 오류 시 런타임 예외 가능

### 게시글 상세 화면

- URL: `/post/{id}`
- 접근 권한: 전체
- 주요 기능: 게시글 내용, 조회수, 댓글/대댓글, 수정/삭제 버튼
- 전달받는 Model Attribute: `post`, `comments`, `commentForm`, `memberId`
- 연결되는 Controller: `PostController.detail`
- 연결되는 Template 파일: `src/main/resources/templates/post/detail.html`
- 성공 흐름: 조회수 처리 후 상세 렌더링
- 실패 흐름: 게시글 없음 시 `/`로 redirect

### 게시글 작성/수정 화면

- URL: `/post/new`, `/post/{id}/edit`
- 접근 권한: 현재 GET은 전체 접근 가능, POST는 로그인 필요
- 주요 기능: 제목/내용 입력
- 전달받는 Model Attribute: `mode`, `form`, `actionUrl`, `submitLabel`
- 연결되는 Controller: `PostController.createForm`, `PostController.editForm`
- 연결되는 Template 파일: `src/main/resources/templates/post/form.html`
- 성공 흐름: form 렌더링
- 실패 흐름: 수정 대상 게시글 없음 시 `/`

### 회원가입 화면

- URL: `/signup`
- 접근 권한: 전체
- 주요 기능: 닉네임, 이메일, 비밀번호 입력
- 전달받는 Model Attribute: `form`, `error`
- 연결되는 Controller: `MemberController.signupForm`, `MemberController.signup`
- 연결되는 Template 파일: `src/main/resources/templates/member/signup.html`
- 성공 흐름: 회원가입 완료 후 로그인 화면
- 실패 흐름: 입력 검증 또는 중복 오류 표시

### 로그인 화면

- URL: `/loginForm`
- 접근 권한: 전체
- 주요 기능: form login, Google/Kakao/Naver OAuth2 로그인
- 전달받는 Model Attribute: 없음
- 연결되는 Controller: `MemberController.loginForm`
- 연결되는 Template 파일: `src/main/resources/templates/member/loginForm.html`
- 성공 흐름: 로그인 form 렌더링
- 실패 흐름: `?error=true`, `?logout` 상태 표시

### 마이페이지 화면

- URL: `/my`
- 접근 권한: 로그인 사용자
- 주요 기능: 내 정보, 활동 요약, 최근 글/댓글
- 전달받는 Model Attribute: `myPostCount`, `myCommentCount`, `recentPosts`, `recentComments`
- 연결되는 Controller: `MyController.myForm`
- 연결되는 Template 파일: `src/main/resources/templates/my/my.html`
- 성공 흐름: 마이페이지 렌더링
- 실패 흐름: 인증 정보 없음 시 예외

### 내 게시글 화면

- URL: `/my/posts`
- 접근 권한: 로그인 사용자
- 주요 기능: 내 게시글 목록, 내 글 통계
- 전달받는 Model Attribute: `myPostCount`, `todayMyPostCount`, `myPostViewCount`, `posts`, `page`, `keyword`
- 연결되는 Controller: `MyController.myPostsForm`
- 연결되는 Template 파일: `src/main/resources/templates/my/myPost.html`
- 성공 흐름: 내 게시글 목록 렌더링
- 실패 흐름: 인증 정보 없음 시 예외 가능

### 내 댓글 화면

- URL: `/my/comments`
- 접근 권한: 로그인 사용자
- 주요 기능: 내 댓글 목록, 댓글 검색, 댓글 통계
- 전달받는 Model Attribute: `pageResponse`
- 연결되는 Controller: `MyController.myCommentForm`
- 연결되는 Template 파일: `src/main/resources/templates/my/myComment.html`
- 성공 흐름: 내 댓글 목록 렌더링
- 실패 흐름: memberId null 시 `MEMBER_NOT_FOUND`

### 회원정보 수정 화면

- URL: `/my/edit`
- 접근 권한: 로그인 사용자
- 주요 기능: 닉네임 수정, 비밀번호 변경
- 전달받는 Model Attribute: `form`, `nicknameRequest`, `passwordRequest`
- 연결되는 Controller: `MyController.EditForm`
- 연결되는 Template 파일: `src/main/resources/templates/my/myEdit.html`
- 성공 흐름: 수정 화면 렌더링
- 실패 흐름: 회원 없음 시 `/`

### 회원 탈퇴 화면

- URL: `/my/withdraw`
- 접근 권한: 로그인 사용자
- 주요 기능: 탈퇴 확인
- 전달받는 Model Attribute: `error`
- 연결되는 Controller: `MyController.withdrawForm`, `MyController.withdraw`
- 연결되는 Template 파일: `src/main/resources/templates/my/withdraw.html`
- 성공 흐름: 탈퇴 후 로그아웃 및 `/`
- 실패 흐름: 회원 없음 시 `/`

### 관리자 대시보드

- URL: `/admin`
- 접근 권한: 관리자
- 주요 기능: 게시글 수, 사용자 수, 관리 메뉴
- 전달받는 Model Attribute: `totalPosts`, `totalUsers`
- 연결되는 Controller: `AdminController.index`
- 연결되는 Template 파일: `src/main/resources/templates/admin/index.html`
- 성공 흐름: 관리자 대시보드 렌더링
- 실패 흐름: 권한 부족 시 Spring Security 처리

### 관리자 게시글 관리

- URL: `/admin/posts`
- 접근 권한: 관리자
- 주요 기능: 게시글 목록, 게시글 삭제
- 전달받는 Model Attribute: `posts`
- 연결되는 Controller: `AdminController.posts`
- 연결되는 Template 파일: `src/main/resources/templates/admin/posts.html`
- 성공 흐름: 게시글 관리 화면 렌더링
- 실패 흐름: 권한 부족 시 Spring Security 처리

### 관리자 회원 관리

- URL: `/admin/users`
- 접근 권한: 관리자
- 주요 기능: 회원 목록, 권한 변경, 회원 삭제
- 전달받는 Model Attribute: `members`
- 연결되는 Controller: `AdminController.members`
- 연결되는 Template 파일: `src/main/resources/templates/admin/users.html`
- 성공 흐름: 회원 관리 화면 렌더링
- 실패 흐름: 권한 부족 시 Spring Security 처리

## 6. 정책 요구사항

### 회원가입 정책

- 이메일은 중복될 수 없다.
- 닉네임은 중복될 수 없다.
- LOCAL 회원의 비밀번호는 BCrypt로 암호화한다.
- 회원가입 완료 후 로그인 화면으로 이동한다.

### 로그인 정책

- 이메일을 username으로 사용한다.
- 일반 회원 로그인 성공 시 `/`, 관리자 로그인 성공 시 `/admin`으로 이동한다.
- 로그인 실패 시 `/loginForm?error=true`로 이동한다.

### OAuth2 로그인 정책

- provider는 Google, Naver, Kakao를 지원한다.
- provider와 providerId로 회원을 식별한다.
- 신규 OAuth2 사용자는 SOCIAL 회원으로 생성한다.

### 게시글 작성/수정/삭제 정책

- 작성 요청은 로그인 사용자만 가능하다.
- 수정/삭제는 작성자 본인만 가능하다.
- 제목과 내용은 blank일 수 없다.
- 삭제 시 게시글에 달린 댓글을 먼저 삭제한다.

### 댓글 작성/삭제 정책

- 댓글/대댓글 작성은 로그인 사용자만 가능하다.
- 댓글 수정/삭제는 작성자 본인만 가능하다.
- 대댓글의 대댓글은 허용하지 않는다.
- 부모 댓글 삭제 시 자식 댓글 처리 정책은 현재 명시되어 있지 않다.

### 관리자 권한 정책

- `/admin/**`는 `ROLE_ADMIN`만 접근 가능하다.
- 관리자 Controller에는 `@PreAuthorize("hasRole('ADMIN')")`도 적용되어 있다.

### 회원 삭제 정책

- 회원 삭제 시 해당 회원의 댓글, 해당 회원 게시글에 달린 댓글, 해당 회원 게시글, 회원 순서로 삭제한다.
- 현재 hard delete 방식이다.
- 자기 자신 삭제 방지나 마지막 관리자 삭제 방지는 현재 코드에 없다.

### 비밀번호 변경 정책

- LOCAL 회원은 현재 비밀번호를 확인한 뒤 새 비밀번호로 변경한다.
- SOCIAL 회원은 화면에서 비밀번호 변경이 제한된다.
- Service 단에서 SOCIAL 회원의 비밀번호 변경을 직접 차단하는 코드는 현재 없다.

### 예외 처리 정책

- 도메인 예외는 `CustomException`과 `ErrorCode`로 표현한다.
- 회원가입 중복/비밀번호 불일치 예외는 회원가입 화면에 error를 표시한다.
- 그 외 CustomException은 ErrorCode의 redirect URL로 이동한다.
- 일부 예외는 Discord 알림 대상에서 제외한다.

## 7. 개선 요구사항

### IMP-001 게시판/카테고리 기능

- 구분: 게시글
- 우선순위: 높음
- 도입 이유: 자유게시판, 공지사항, 질문게시판처럼 게시글 분류가 가능하다.
- 기대 효과: 서비스 구조가 명확해지고 관리자 기능 확장이 쉽다.
- 구현 시 고려사항: 게시글 작성 시 게시판 선택, 게시판별 권한, 게시판별 검색/페이징
- 관련 도메인: Post, Admin
- 예상 변경 파일: `Post`, `PostRequest`, `PostController`, `PostService`, `PostRepository`, 신규 `Board` 또는 `Category`

### IMP-002 테스트 코드 보강

- 구분: 품질
- 우선순위: 높음
- 도입 이유: 현재 테스트가 최신 코드와 맞지 않아 컴파일 실패한다.
- 기대 효과: 리팩터링 안정성 확보, 포트폴리오 신뢰도 향상
- 구현 시 고려사항: Service 테스트 우선 복구, Controller/Security 테스트 추가
- 관련 도메인: 전체
- 예상 변경 파일: `src/test/java/project/board/**`

### IMP-003 조회수 개선

- 구분: 성능
- 우선순위: 높음
- 도입 이유: 현재는 쿠키와 DB update에 의존한다.
- 기대 효과: 중복 조회 방지 강화, DB 부하 감소, 인기글 기능 확장
- 구현 시 고려사항: Redis Set/Counter, 주기적 DB 반영, 장애 시 데이터 유실 방지
- 관련 도메인: Post
- 예상 변경 파일: `PostController`, `PostService`, 신규 조회수 서비스

### IMP-004 좋아요 기능

- 구분: 게시글/댓글
- 우선순위: 중간
- 도입 이유: 사용자 반응 기능을 추가할 수 있다.
- 기대 효과: 서비스 완성도 향상, 동시성/유니크 제약 설계 경험 확보
- 구현 시 고려사항: 회원당 1회 제한, 취소 기능, unique constraint
- 관련 도메인: Member, Post, Comment
- 예상 변경 파일: 신규 `PostLike`, `CommentLike`, Controller/Service/Repository/DTO

### IMP-005 파일 첨부

- 구분: 게시글
- 우선순위: 중간
- 도입 이유: 이미지/파일 첨부가 가능한 게시판으로 확장할 수 있다.
- 기대 효과: 파일 저장소, 보안 검증, S3 연동 경험 확보
- 구현 시 고려사항: 확장자, MIME type, 크기 제한, 다운로드 권한, 저장소 전략
- 관련 도메인: Post
- 예상 변경 파일: 신규 `Attachment`, 파일 Service, 게시글 form/template

### IMP-006 신고/관리 기능

- 구분: 관리자
- 우선순위: 중간
- 도입 이유: 운영자 관점의 콘텐츠 관리 기능이 필요하다.
- 기대 효과: 관리자 기능 고도화, 상태 관리 경험 확보
- 구현 시 고려사항: 신고 대상 polymorphic 설계 또는 post/comment 분리 설계, 처리 상태
- 관련 도메인: Member, Post, Comment, Admin
- 예상 변경 파일: 신규 `Report`, Admin Controller/Service/Repository

### IMP-007 검색 고도화

- 구분: 게시글
- 우선순위: 중간
- 도입 이유: 현재 게시글 검색은 제목 기준만 지원한다.
- 기대 효과: 제목/내용/작성자/날짜/카테고리 검색 제공
- 구현 시 고려사항: Querydsl, 인덱스, 페이징, 검색 조건 DTO
- 관련 도메인: Post, Member
- 예상 변경 파일: `PostRepository`, `PostService`, 검색 DTO, template

### IMP-008 API 서버 분리

- 구분: 아키텍처
- 우선순위: 낮음
- 도입 이유: React/Vue 프론트엔드 또는 모바일 앱 확장에 대응한다.
- 기대 효과: REST API 설계, 응답 표준화, API 문서화 경험 확보
- 구현 시 고려사항: SSR Controller와 REST Controller 분리, 인증 전략, CORS, CSRF
- 관련 도메인: 전체
- 예상 변경 파일: 신규 API Controller, Response DTO, API exception handler

### IMP-009 알림 기능

- 구분: 사용자 경험
- 우선순위: 낮음
- 도입 이유: 내 게시글 댓글, 내 댓글 답글을 알려줄 수 있다.
- 기대 효과: 서비스 완성도 향상, SSE/WebSocket 확장 가능
- 구현 시 고려사항: 알림 읽음 처리, 중복 알림, 실시간 여부
- 관련 도메인: Member, Post, Comment
- 예상 변경 파일: 신규 `Notification`, NotificationService, template
