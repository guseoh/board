# Board 프로젝트 개요

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

## 목적과 구현 형태

Board는 회원이 게시글과 계층형 댓글(댓글 1단계 + 대댓글 1단계)을 작성하고, 자신의 활동을 조회하며, 관리자가 회원과 게시글을 관리하는 웹 게시판이다. Browser 요청을 Spring MVC View Controller가 받고 Thymeleaf HTML 또는 Redirect를 반환한다. 현재 JSON REST API는 없다.

## 사용자와 사용 가능 기능

| 사용자 | 현재 기능 |
| --- | --- |
| 비회원 | 게시글 목록·검색·상세 조회, 조회수 쿠키 기반 증가, 회원가입, form/OAuth2 로그인 시작 |
| LOCAL 회원 | 비회원 기능 + 게시글/댓글/대댓글 CRUD, 마이페이지, 닉네임·비밀번호 변경, 탈퇴, 로그아웃 |
| SOCIAL 회원 | LOCAL 회원과 같은 게시판 기능, 닉네임 변경·탈퇴. 화면은 비밀번호 변경 영역을 숨김 |
| 관리자 | 회원 기능 + `/admin/**` 대시보드, 게시글 삭제, 회원 역할 변경·삭제 |

SOCIAL 비밀번호 제한은 `MemberUpdateResponse.passwordChangeable`과 View 표시로만 적용된다. `MemberService.updatePassword` 자체는 `LoginType`을 검사하지 않으므로 서버 정책으로 완전히 강제된 상태는 아니다.

## 핵심 기능

- 이메일·닉네임 중복 및 비밀번호 확인을 포함한 LOCAL 회원가입
- Spring Security form login, Google/Naver/Kakao OAuth2 login, Session 로그아웃
- 게시글 목록(페이지 크기 기본 5), 제목 검색, 상세, 작성·수정·삭제
- `View_Count` HttpOnly 쿠키로 게시글별 12시간 내 중복 조회수 증가 억제
- 댓글과 한 단계 대댓글 작성, 작성자 수정·삭제
- 내 활동 통계, 최근 게시글·댓글, 내 글·댓글 화면
- 닉네임/비밀번호 변경, 회원 탈퇴
- 관리자 통계, 게시글·회원 목록, 게시글 삭제, 회원 역할 변경·삭제
- Actuator와 중요 `CustomException`의 선택적 Discord Webhook 알림

## 기술 스택

| 영역 | 현재 구성 |
| --- | --- |
| 언어/런타임 | Java 17 toolchain |
| 프레임워크 | Spring Boot 4.0.1, Spring MVC, Spring Security, Spring Data JPA |
| View | Thymeleaf, thymeleaf-extras-springsecurity6, 단일 CSS 자원 |
| DB | MySQL Connector/J 8.0.33; 테스트 runtime H2 |
| 빌드 | Gradle Wrapper 9.2.1 |
| 관측 | Actuator, Logback rolling file, P6Spy, Discord Webhook |
| 배포 | GitHub Actions, EC2 SSH, 실행 JAR; 별도 Dockerfile/Compose 제공 |

## 프로필과 설정 범위

- `spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}`: 환경변수가 없으면 `local`.
- `spring.profiles.include=oauth`: 항상 `oauth` 프로필을 포함하도록 선언.
- 저장소에는 공통 `application.properties`만 있다. datasource 및 OAuth client 등록을 제공할 `application-local.properties`, `application-prod.properties`, `application-oauth.properties`, `application-test.properties`는 추적되지 않는다.
- 공통 JPA 설정은 `ddl-auto=update`, `show-sql=false`다.
- Actuator는 `health,info,metrics,mappings`를 노출하고 health 상세를 항상 표시한다.
- Discord는 `DISCORD_WEBHOOK_URL`, `DISCORD_WEBHOOK_ENABLED` 환경변수를 사용하며 기본 비활성이다.

## 현재 구현 범위와 제한

- `post/controller/api/PostApiController`는 비어 있고 annotation/Mapping이 없다.
- 검색 결과는 페이지네이션하지 않는다.
- `/my/posts`는 회원 ID·제목 keyword·page/size를 한 Repository query에 적용하고 회원별 오늘 작성 수와 누적 조회수를 표시한다.
- 회원 탈퇴 화면의 `confirmText` 확인은 브라우저 JavaScript에만 있고 Controller는 파라미터를 검증하지 않는다.
- 프로젝트 설명은 `build.gradle`에 아직 `Demo project for Spring Boot`로 남아 있다.
