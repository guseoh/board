# 현재 Security 구조

> 기준: `refactor/pre-m2-quality-baseline` / 2026-07-14

## 인증 모델

Spring Security의 서버 Session 인증을 사용한다. JWT나 API token은 없다. `UnifiedPrincipal`은 `UserDetails`와 `OAuth2User`를 동시에 구현하고 member ID, email, nickname, role key, `LoginType`, form password, OAuth provider/attributes를 한 타입으로 제공한다.

`getAuthorities()`는 저장된 `role` 문자열로 `SimpleGrantedAuthority` 하나를 만든다. `Role.USER.getKey()`는 `ROLE_USER`, ADMIN은 `ROLE_ADMIN`이다.

## Filter Chain과 URL 정책

`SecurityConfig`는 `@EnableWebSecurity`, `@EnableMethodSecurity`를 사용한다.

| 규칙 | 정책 |
| --- | --- |
| `/`, `/login`, `/loginForm`, `/signup`, 정적 자원, `/error` | permitAll |
| `/oauth2/**`, `/login/oauth2/**` | permitAll |
| `/actuator/health` | permitAll |
| `/actuator/**` | `hasRole("ADMIN")` |
| `/admin/**` | `hasRole("ADMIN")` |
| `GET /post/*/edit` | authenticated |
| `POST /post/**` | authenticated |
| `/my/**` | authenticated |
| 그 외 | permitAll |

`AdminViewController`에도 클래스 수준 `@PreAuthorize("hasRole('ADMIN')")`가 있어 관리자 경로를 중복 방어한다.

## 권한 매트릭스

| 요청 영역 | 비회원 | USER | ADMIN |
| --- | ---: | ---: | ---: |
| `/`, 게시글 목록·검색·상세 | 허용 | 허용 | 허용 |
| `/signup`, `/loginForm`, OAuth2 시작 | 허용 | 허용 | 허용 |
| `GET /post/new` | 허용 | 허용 | 허용 |
| `GET /post/{id}/edit` | 로그인 이동 | 작성자만 허용 | 작성자인 경우 허용 |
| `POST /post/**` (작성·수정·삭제·댓글) | 로그인 이동 | 허용(소유권은 Service) | 허용(소유권은 동일 적용) |
| `/my/**` | 로그인 이동 | 허용 | 허용 |
| `/admin/**` | 로그인 이동 | 403 | 허용 |
| `/actuator/health` | 허용 | 허용 | 허용 |
| 그 외 노출된 Actuator | 로그인 이동 | 403 | 허용 |
| local test route | local profile에서만 허용 | local profile에서만 허용 | local profile에서만 허용 |

ADMIN도 일반 게시글/댓글 수정·삭제 시 작성자 검증을 우회하지 않는다. GET 수정 화면과 POST 수정·삭제 모두 SecurityContext principal의 member ID를 Service가 Entity 작성자와 비교하며, 관리자는 관리자 전용 삭제 route를 사용해야 한다.

## form login

`POST /login` → `DaoAuthenticationProvider` → `CustomUserDetailsService.loadUserByUsername(email)` → `MemberRepository.findByEmail` → `UnifiedPrincipal.from` → BCrypt 검증이다. 성공은 `CustomLoginSuccessHandler`가 role에 따라 `/admin` 또는 `/`로 redirect하고 실패는 `/loginForm?error=true`다.

로그인 form의 `redirect` hidden parameter는 success handler에서 읽지 않아 댓글 작성 중 로그인 유도 후 원 상세로 복귀하지 않는다.

## OAuth2 login

`CustomOauth2UserService`는 Google의 top-level `sub/email/name`, Naver의 `response.id/email/name`, Kakao의 `id`와 `kakao_account`를 변환한다. `(provider,providerId)`가 없으면 dummy password를 BCrypt로 저장한 SOCIAL/USER 회원을 만든다.

- Kakao email 미제공: provider ID를 포함한 `kakao_{providerId}@oauth.local` 대체 문자열.
- Google/Naver email/name null 검증은 없다.
- provider 응답의 email과 기존 LOCAL 회원을 연결하지 않는다.
- 미지원 provider와 Naver `response` 누락은 `OAuth2AuthenticationException`.

OAuth2 성공은 `defaultSuccessUrl("/")`이다. form login의 custom role redirect와 다르게 OAuth ADMIN도 기본적으로 `/`로 이동한다.

## LOCAL과 SOCIAL 차이

| 기능 | LOCAL | SOCIAL |
| --- | --- | --- |
| form login | 가능 | 저장된 dummy password를 사용자가 모르므로 실질적으로 불가 |
| OAuth2 login | 계정 자동 연결 없음 | provider ID로 가능 |
| nickname 변경 | 가능 | 가능 |
| password 변경 화면 | 표시 | `passwordChangeable=false`로 숨김 |
| password 변경 endpoint | 가능 | Service에서 차단 |
| 탈퇴 | 가능 | 가능 |

## CSRF, logout, 접근 거부

- CSRF는 비활성화하지 않았으므로 기본 활성이다. Security test가 token 없는 인증 POST의 403을 확인하도록 작성되어 있다.
- logout URL은 `/logout`, Session invalidate, `JSESSIONID` 삭제, 성공 `/`다.
- custom `AccessDeniedHandler`나 authentication entry point는 없다. 익명 보호 URL은 login page redirect, 인증 USER의 admin 접근은 기본 403이다.
- `GlobalViewControllerAdvice`는 `CustomException`만 처리하며 Security filter의 접근 거부는 처리하지 않는다.

## 현재 보안 한계

- 공통 profile은 Actuator `health,info,metrics,mappings`를 노출하지만 health만 익명 허용하고 나머지는 ADMIN으로 제한한다. prod profile은 `health,info`만 노출하고 health detail을 숨긴다.
- local `/test/discord-error`는 `@Profile("local")`에서만 등록된다. local 환경의 익명 접근은 의도된 개발용 동작이다.
- 회원 탈퇴 확인 문구는 화면과 Service 양쪽에서 검증한다.
- OAuth provider 데이터의 필수값/중복 정책이 충분히 명시되지 않았고 `(provider,providerId)` DB unique가 없다.
- LOCAL/SOCIAL 동일 이메일 연결, provider별 계정 통합과 로그인 후 redirect parameter 처리는 제품 결정이 필요하다.
- 조회수 cookie는 HttpOnly이지만 Secure/SameSite를 지정하지 않는다.
- 관리자 role form은 `Role` enum으로 직접 바인딩해 허용값을 제한한다. 자기 계정 삭제와 최후 ADMIN 보호 정책은 별도 제품 정책이다.
- `ddl-auto=update`와 Secret 외부화·회전은 운영 정책으로 남아 있다.
