# 현재 구현 요구사항

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

상태는 `구현`, `부분 구현`, `미구현`으로 구분한다. 현재 test source는 main 공개 메서드명과 일치하며 2026-07-13 기준 `clean test`와 `clean build`가 성공했다.

## 회원과 인증

### REQ-MEMBER-001 회원가입

- 상태/사용자: 구현 / 비회원
- 설명·입력: `GET/POST /signup`; `MemberCreateRequest(nickname,password,passwordConfirm,email)` form.
- 사전 조건·규칙: nickname 2~12자 영문/숫자/한글, password 8~20자 영문+숫자, email 형식; email/nickname 중복 금지; 두 비밀번호 일치; BCrypt, `USER`/`LOCAL` 생성.
- 성공/실패: 성공 시 `/loginForm`과 flash `msg`; BindingResult 오류는 `member/signup`; 정책 오류는 `GlobalViewControllerAdvice`가 가입 View 반환.
- 권한: permitAll.
- 코드/테스트: `MemberViewController.signup`, `MemberService.signUp`, `MemberServiceTest.signUp*`, `ControllerMvcTest.memberController`, `ExceptionAndValidationTest`.
- 현재 제한: email에는 `@NotBlank`가 없어 빈 문자열이 `@Email`을 통과할 수 있고 DB nullable/unique 제약에서만 실패할 수 있다. 정책 오류 시 nickname/email만 보존하고 password는 제거한다.

### REQ-AUTH-001 form login

- 상태/사용자: 구현 / 비회원 LOCAL 계정
- 설명·입력: `GET /loginForm`, `POST /login`; form field `username`(email), `password`, 선택적 `redirect` hidden.
- 사전 조건·규칙: `CustomUserDetailsService`가 email 조회, BCrypt 비교, `UnifiedPrincipal` 생성.
- 성공/실패: ADMIN `/admin`, USER `/`; 실패 `/loginForm?error=true`.
- 권한: permitAll; 성공 후 Session 인증.
- 코드/테스트: `SecurityConfig`, `CustomUserDetailsService`, `CustomLoginSuccessHandler`, `SecurityConfigTest.loginSuccessRedirectsByRole`.
- 현재 제한: hidden `redirect`를 success handler가 사용하지 않는다.

### REQ-AUTH-002 OAuth2 login

- 상태/사용자: 구현 / 비회원
- 설명·입력: `/oauth2/authorization/{google|naver|kakao}` 시작, provider callback은 Spring Security가 처리.
- 사전 조건·규칙: provider별 ID/email/name 파싱; `(provider,providerId)` 재사용; 신규는 dummy BCrypt password, `USER`/`SOCIAL` 저장.
- 성공/실패: 성공 `/`; 미지원 provider나 잘못된 Naver 응답은 `OAuth2AuthenticationException`.
- 권한: OAuth2 endpoint permitAll, 이후 Session.
- 코드/테스트: `CustomOauth2UserService`, `GoogleUserInfo`, `NaverUserInfo`, `KakaoUserInfo`, `OAuthTest` 5개.
- 현재 제한: Google/Naver 이메일 누락을 명시 검증하지 않는다. Kakao는 `kakao_{id}@oauth.local`을 사용한다. LOCAL email과 OAuth email의 계정 연결은 하지 않는다.

### REQ-AUTH-003 로그아웃

- 상태/사용자: 구현 / 인증 사용자
- 설명·입력: `POST /logout` form + CSRF.
- 규칙·결과: Session 무효화, `JSESSIONID` 삭제, `/` redirect.
- 실패/권한: CSRF 없으면 403; Spring Security logout filter 처리.
- 코드/테스트: `SecurityConfig`, `fragments/navbar.html`; 전용 테스트 없음.
- 현재 제한: 없음(현재 계약 범위).

### REQ-MEMBER-002 닉네임 수정

- 상태/사용자: 구현 / LOCAL·SOCIAL
- 설명·입력: `POST /my/edit/nickname`, `MemberNicknameUpdateRequest`.
- 사전 조건·규칙: 인증 회원; 형식 검증, trim, 본인 외 nickname 중복 검사, 변경 감지; Session principal nickname 갱신.
- 성공/실패: 성공 `/` + flash; Validation은 `my/myEdit`; 정책 예외는 `/signup`으로 redirect되는 `DUPLICATE_NICKNAME` 공통 코드 때문에 맥락이 맞지 않는다.
- 권한: authenticated.
- 코드/테스트: `MyPageViewController.editNickname/refreshAuthentication`, `MemberService.updateNickname`, `MemberServiceTest.updateNickname`.
- 현재 제한: nickname이 null이면 `@Pattern`을 통과하고 Service가 아무 변경 없이 성공 처리할 수 있다.

### REQ-MEMBER-003 비밀번호 변경

- 상태/사용자: 부분 구현 / UI상 LOCAL
- 설명·입력: `POST /my/edit/password`, current/new/confirm.
- 규칙: 세 필드 NotBlank+패턴, current BCrypt 확인, new/confirm 일치, BCrypt 후 변경 감지.
- 성공/실패: 성공 `/` + flash; Validation은 edit View; 정책 오류는 `/my/edit` redirect.
- 권한: authenticated.
- 코드/테스트: `MyPageViewController.editPassword`, `MemberService.updatePassword`, `MemberServiceTest.updatePassword`.
- 현재 제한: View는 SOCIAL에 form을 숨기지만 Service/endpoint가 `LoginType.LOCAL`을 강제하지 않는다.

### REQ-MEMBER-004 회원 탈퇴

- 상태/사용자: 부분 구현 / 인증 회원
- 설명·입력: `GET/POST /my/withdraw`; 화면의 `confirmText`.
- 규칙: 회원 댓글 → 회원 게시글의 댓글 → 회원 게시글 → 회원 bulk delete 후 logout.
- 성공/실패: 성공 `/`; 없는 회원은 `MEMBER_NOT_FOUND`.
- 권한: authenticated.
- 코드/테스트: `MyPageViewController.withdraw`, `MemberService.withdraw/MemberRemovalPolicy`, `MemberServiceTest.deletePolicies`.
- 현재 제한: Controller가 `confirmText`를 받지 않아 확인 문구는 client-side JavaScript로만 검사한다. self-reference 댓글 FK 삭제 순서 테스트가 없다.

## 게시글

### REQ-POST-001 목록과 페이지네이션

- 상태/사용자: 구현 / 전체
- 입력·규칙: `GET /`, `PageRequestDto(page=1,size=5)`; id 내림차순, member fetch join.
- 성공: 전체/오늘/회원 수, 로그인 시 내 글/댓글 수와 `post/list`.
- 실패/권한: permitAll; 잘못된 page/size의 별도 Validation 없음.
- 코드/테스트: `PostViewController.list`, `PostService.getPosts`, `PostRepository.findAllWithMember`, Repository/Service/MVC 테스트.
- 현재 제한: page 0 이하·size 0 등의 방어가 없다.

### REQ-POST-002 제목 검색

- 상태/사용자: 구현 / 전체
- 입력·규칙: `GET /posts/search?keyword=...`; 제목 containing 검색.
- 성공: `post/list`에 `posts`, `keyword`.
- 실패/권한: permitAll; keyword 필수 바인딩.
- 코드/테스트: `PostViewController.search`, `PostService.search`, `PostRepository.findByTitleContaining`.
- 현재 제한: pagination과 목록 통계/page model을 제공하지 않는다.

### REQ-POST-003 상세와 조회수

- 상태/사용자: 구현 / 전체
- 입력·규칙: `GET /post/{id}`; `View_Count` 쿠키에 `|id|`가 없을 때 JPQL bulk increment, 12시간 유지; root comments와 children DTO화.
- 성공: `post/detail`, post/comments/commentForm/memberId model.
- 실패/권한: permitAll; 없는 Post는 `/` redirect + flash, increment 결과 0도 같은 오류.
- 코드/테스트: `PostViewController.detail/increaseViewCount`, `PostService.getPostDetail/viewCount`, `PostRepository.incrementViewCount`.
- 현재 제한: 쿠키 문자열이 방문 게시글 수만큼 커지고 Secure/SameSite 설정이 없다.

### REQ-POST-004 작성

- 상태/사용자: 구현 / USER·ADMIN
- 입력·규칙: `GET /post/new`, `POST /post/new`; title/content NotBlank, 인증 member 조회, Entity 재검증.
- 성공: 생성 ID의 `/post/{id}` + flash.
- 실패/권한: POST authenticated+CSRF; GET은 permitAll. Validation은 `post/form`.
- 코드/테스트: `PostViewController.create*`, `PostService.createPost`, `Post.create`, 관련 테스트.
- 현재 제한: Entity column length 500을 DTO가 사전 검증하지 않는다.

### REQ-POST-005 수정

- 상태/사용자: 부분 구현 / 작성자
- 입력·규칙: `GET/POST /post/{id}/edit`; POST에서 작성자 검증 후 변경 감지.
- 성공: `/post/{id}` + flash; Validation은 edit mode form.
- 실패/권한: POST authenticated+CSRF; 비작성자는 `NOT_POST_OWNER`.
- 코드/테스트: `PostViewController.editForm/edit`, `PostService.update`, `PostServiceTest.updateOwnerOnly`.
- 현재 제한: GET form은 인증/소유권 검증 없이 permitAll이고 본문 길이 Validation이 없다.

### REQ-POST-006 삭제

- 상태/사용자: 구현 / 작성자
- 입력·규칙: `POST /post/{id}/delete`; 작성자 확인, 댓글 bulk delete 후 Post 삭제.
- 성공: `/` + flash.
- 실패/권한: authenticated+CSRF; 없는 글/비소유자 예외.
- 코드/테스트: `PostViewController.delete`, `PostService.delete`, `PostServiceTest.deleteOwner`.
- 현재 제한: 댓글 self-reference를 고려한 별도 자식 삭제가 없다.

## 댓글

### REQ-COMMENT-001 댓글·대댓글 작성

- 상태/사용자: 구현 / USER·ADMIN
- 입력·규칙: `POST /post/{postId}/comment`, `POST /post/{postId}/comment/{parentId}/replies`; content NotBlank. 대댓글 parent는 같은 Post의 root comment여야 한다.
- 성공: 해당 상세 redirect.
- 실패/권한: Security상 POST authenticated; Controller도 null principal을 로그인 화면으로 처리. Validation은 flash error.
- 코드/테스트: `CommentViewController.create/createReply`, `CommentService.createComment/createReply`, `CommentServiceTest`.
- 현재 제한: DB column 500 길이와 DTO 길이 검증 불일치.

### REQ-COMMENT-002 수정·삭제

- 상태/사용자: 구현 / 작성자
- 입력·규칙: edit/delete POST; Entity의 member ID 소유권 확인, 수정은 변경 감지.
- 성공: Post 상세 redirect.
- 실패/권한: authenticated+CSRF; Validation, 없음, 비소유자 예외.
- 코드/테스트: `CommentViewController.update/delete`, `CommentService.update/delete`, `CommentServiceTest.updateAndDeleteOwnerOnly`.
- 현재 제한: 인증 객체 null 방어는 create/reply에만 있고 edit/delete에는 없다(필터가 정상 적용된다는 전제).

## 마이페이지

### REQ-MYPAGE-001 대시보드

- 상태/사용자: 구현 / 인증 회원
- 설명: `GET /my`; 내 글/댓글 수와 최근 각 5개.
- 성공/실패: `my/my`; principal/member ID 없으면 인증 예외.
- 권한/코드/테스트: authenticated; `MyPageViewController.myForm`, Post/Comment Service, `ControllerMvcTest.myController`.
- 현재 제한: 여러 독립 쿼리를 한 화면에서 수행한다.

### REQ-MYPAGE-002 내 게시글

- 상태/사용자: 구현 / 인증 회원
- 입력: `GET /my/posts?page&size&keyword`.
- 성공: 회원별 제목 검색·페이지 결과, 내 글 수, 오늘 작성 수, 누적 조회수와 `my/myPost`.
- 코드/테스트: `MyPageViewController.myPostsForm`, `PostService.getMyPosts/countMyPostViews`, `PostRepository.findMyPosts/sumViewCountByMemberId`, MVC·Repository·Service 테스트.
- 현재 제한: page/size 입력 범위 Validation과 빈 페이지 표시 품질은 별도 정리가 필요하다.

### REQ-MYPAGE-003 내 댓글

- 상태/사용자: 구현 / 인증 회원
- 입력·규칙: `GET /my/comments?page&size&keyword`; 댓글 내용 또는 글 제목 keyword, 오늘/7일/전체 통계.
- 성공: DTO projection page를 `my/myComment`에 제공.
- 코드/테스트: `MyPageViewController.myCommentForm`, `CommentService.getMyCommentPage`, `CommentRepository.findMyComments`, Repository/Service/MVC 테스트 소스.
- 현재 제한: page/size Validation 없음.

## 관리자

### REQ-ADMIN-001 대시보드·게시글 관리

- 상태/사용자: 구현 / ADMIN
- 설명: `GET /admin`, `GET /admin/posts`, `POST /admin/posts/{postId}/delete`.
- 규칙·결과: 전체 count, member fetch join 목록, 댓글 bulk delete 후 글 삭제; Admin View/redirect.
- 실패/권한: URL rule + `@PreAuthorize`; USER 403, 익명 login redirect.
- 코드/테스트: `AdminViewController`, `PostService.getPostsForAdmin/deleteForAdmin`, `SecurityConfigTest`, `ControllerMvcTest.adminController`.
- 현재 제한: pagination이 없고 Entity를 View에 직접 전달한다.

### REQ-ADMIN-002 회원 관리와 역할 변경

- 상태/사용자: 구현 / ADMIN
- 설명·입력: `GET /admin/users`, role/delete POST; role 문자열.
- 규칙·결과: `Role.valueOf`, 변경 감지 또는 회원 제거 정책; `/admin` redirect.
- 실패/권한: ADMIN; 잘못된 role은 처리되지 않은 `IllegalArgumentException`이 될 수 있다.
- 코드/테스트: `AdminViewController`, `MemberService.changeMemberRole/deleteMemberByAdmin`, Member Service/Security/MVC 테스트.
- 현재 제한: 자기 자신 삭제·최후 ADMIN 강등 방지 정책과 pagination이 없다.

## 관측 가능성

### REQ-OBSERVABILITY-001 Actuator·로그·Discord 알림

- 상태/사용자: 부분 구현 / 운영자·local 개발자
- 설명: Actuator `health,info,metrics,mappings`; Logback console/rolling files; local P6Spy; 중요 `CustomException` Discord 전송; local `/test/discord-error`.
- 입력·규칙: `DISCORD_WEBHOOK_URL`, `DISCORD_WEBHOOK_ENABLED`; 가입 중복/비밀번호 불일치 세 종류는 알림 제외; 전송 실패는 warn 후 원 요청 계속.
- 성공/실패: endpoint 응답 또는 로그/선택적 Webhook; notifier failure는 삼킨다.
- 권한: Actuator 경로에 별도 matcher가 없어 `anyRequest().permitAll()` 적용. test route는 local profile에서 permitAll.
- 코드/테스트: `application.properties`, `GlobalViewControllerAdvice`, `DiscordNotifier`, `LocalTestController`, Exception 테스트(Notifier mock); 실제 Webhook 통합 테스트 없음.
- 현재 제한: health 상세와 metrics/mappings가 비인증 공개되고, 알림 기준이 HTTP 심각도 대신 ErrorCode 제외 목록이다.

## 현재 미구현

- 활성 JSON REST API와 JSON 오류 계약
- React 클라이언트
- 풋살 매칭·예약 기능
- 서버 측 회원 탈퇴 확인 문구 검증
