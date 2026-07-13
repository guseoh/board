# 현재 Security 구조

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

## 인증 모델

Spring Security의 서버 Session 인증을 사용한다. JWT나 API token은 없다. `UnifiedPrincipal`은 `UserDetails`와 `OAuth2User`를 동시에 구현하고 member ID, email, nickname, role key, `LoginType`, form password, OAuth provider/attributes를 한 타입으로 제공한다.

`getAuthorities()`는 저장된 `role` 문자열로 `SimpleGrantedAuthority` 하나를 만든다. `Role.USER.getKey()`는 `ROLE_USER`, ADMIN은 `ROLE_ADMIN`이다.

## Filter Chain과 URL 정책

`SecurityConfig`는 `@EnableWebSecurity`, `@EnableMethodSecurity`를 사용한다.

| 규칙 | 정책 |
| --- | --- |
| `/`, `/login`, `/loginForm`, `/signup`, 정적 자원, `/error` | permitAll |
| `/oauth2/**`, `/login/oauth2/**` | permitAll |
| `/admin/**` | `hasRole("ADMIN")` |
| `POST /post/**` | authenticated |
| `/my/**` | authenticated |
| 그 외 | permitAll |

`AdminViewController`에도 클래스 수준 `@PreAuthorize("hasRole('ADMIN')")`가 있어 관리자 경로를 중복 방어한다.

## 권한 매트릭스

| 요청 영역 | 비회원 | USER | ADMIN |
| --- | ---: | ---: | ---: |
| `/`, 게시글 목록·검색·상세 | 허용 | 허용 | 허용 |
| `/signup`, `/loginForm`, OAuth2 시작 | 허용 | 허용 | 허용 |
| `GET /post/new`, `GET /post/{id}/edit` | 허용 | 허용 | 허용 |
| `POST /post/**` (작성·수정·삭제·댓글) | 로그인 이동 | 허용(소유권은 Service) | 허용(소유권은 동일 적용) |
| `/my/**` | 로그인 이동 | 허용 | 허용 |
| `/admin/**` | 로그인 이동 | 403 | 허용 |
| Actuator와 local test route | 허용 | 허용 | 허용 |

ADMIN도 일반 게시글/댓글 수정·삭제 시 작성자 검증을 우회하지 않는다. 관리자 전용 삭제 route를 사용해야 한다.

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
| password 변경 endpoint | 가능 | 서버에서 LoginType을 막지 않아 호출 가능 |
| 탈퇴 | 가능 | 가능 |

## CSRF, logout, 접근 거부

- CSRF는 비활성화하지 않았으므로 기본 활성이다. Security test가 token 없는 인증 POST의 403을 확인하도록 작성되어 있다.
- logout URL은 `/logout`, Session invalidate, `JSESSIONID` 삭제, 성공 `/`다.
- custom `AccessDeniedHandler`나 authentication entry point는 없다. 익명 보호 URL은 login page redirect, 인증 USER의 admin 접근은 기본 403이다.
- `GlobalViewControllerAdvice`는 `CustomException`만 처리하며 Security filter의 접근 거부는 처리하지 않는다.

## 현재 보안 한계

- Actuator의 health 상세, metrics, mappings가 `anyRequest().permitAll()` 아래 공개된다.
- local `/test/discord-error`도 별도 권한이 없다.
- 게시글 수정 form GET은 인증/소유권 검증 없이 공개된다.
- 회원 탈퇴 확인 문구는 client-side에서만 검증된다.
- SOCIAL password 변경 제한이 View에만 있다.
- OAuth provider 데이터의 필수값/중복 정책이 충분히 명시되지 않았고 `(provider,providerId)` DB unique가 없다.
- 조회수 cookie는 HttpOnly이지만 Secure/SameSite를 지정하지 않는다.
- 관리자 role form 문자열은 allow-list 바인딩 DTO 없이 `Role.valueOf`로 처리된다.
- health 상세 공개와 `ddl-auto=update`는 운영 프로필별 재검토가 필요하지만 현재 저장소에는 profile별 설정 파일이 없다.
