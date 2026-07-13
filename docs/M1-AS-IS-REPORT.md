# M1 As-Is 조사 보고서

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 조사일 2026-07-13 (Asia/Seoul)

## 작업 기준

| 항목 | 확인 결과 |
| --- | --- |
| 저장소 | `guseoh/board`, `C:\Users\guseo\IdeaProjects\board` |
| 기준 브랜치 | `recover` |
| 기준 HEAD | `d333e3868e5bb94073030780ce0910a65b3ef4d8` (`docs: M0 프로젝트 복구 결과 보고서 작성`) |
| upstream | `origin/recover`, 기준 HEAD와 일치 |
| `origin/develop...recover` | develop 고유 0 / recover 고유 5 |
| 최초 M1 조사 HEAD | `31326e715d39d2a4af9153fb4ccbcf72dc4fb229` |
| 동기화 | M1 문서를 stash로 보관하고 `pull --ff-only`로 원격 5개 M0 커밋 반영 후 재검토 |

반영된 M0 커밋은 회원가입/OAuth 오류 수정, 마이페이지 조회·통계 수정, 테스트 복구, CI·Docker 검증 복구와 M0 보고서다. M1 본문은 동기화 후 코드와 57개 테스트를 기준으로 갱신했다.

## 조사 범위

- 빌드/설정: `build.gradle`, settings, Gradle Wrapper, 공통 properties, Logback, P6Spy
- main code: `member`, `post`, `comment`, `mypage`, `admin`, `global` Java 파일
- 요청 흐름: View Controller, 빈 API 클래스, local test Controller, DTO/Validation, Service, Repository, Entity
- 인증: filter chain, form/OAuth2 login, logout, `UnifiedPrincipal`, user services, success handler, provider adapters
- 화면: 16개 Thymeleaf template의 form/action/security 표현식과 `static/css/app.css`
- 테스트: 12개 테스트 클래스, 57개 test method와 test topology
- 운영: Actions workflow, Dockerfile, 두 Compose, Actuator, Discord notifier, monitor script
- 기존 문서: API, ERD, REQUIREMENTS, FUTURE_FEATURES, DBML과 `M0-RECOVERY-REPORT.md`

## 현재 시스템 요약

Java 17/Spring Boot 4.0.1 기반 Session 인증 SSR 게시판이다. Browser → Security Filter Chain → View Controller → Service → JPA Repository → MySQL 흐름으로 동작하고 Thymeleaf HTML 또는 Redirect를 반환한다. 게시글·댓글·회원·관리자·마이페이지 기능과 Google/Naver/Kakao OAuth2 login이 있다. 활성 JSON REST API는 없다.

M0에서 `/my/posts`가 회원·검색·page 조건을 같은 Repository query에 적용하도록 복구됐고, 회원별 오늘 글 수와 누적 조회수도 올바른 Service 결과를 사용한다. 회원가입 정책 오류는 nickname/email만 보존하고 password는 재표시하지 않으며, Kakao email 미제공 시 `kakao_{id}@oauth.local`을 사용한다.

## 생성·수정한 문서

| 구분 | 문서 |
| --- | --- |
| 생성 | `README.md`, `PROJECT-OVERVIEW.md`, `ARCHITECTURE.md`, `PACKAGE-STRUCTURE.md`, `SECURITY.md`, `TESTING-AND-OPERATIONS.md`, `M1-AS-IS-REPORT.md` |
| 갱신 | `REQUIREMENTS.md`, `API.md`, `ERD.md`, `FUTURE_FEATURES.md` |
| 제거 | `erd/board.dbml` (M1에서 금지된 DBML legacy artifact) |
| 유지 | `M0-RECOVERY-REPORT.md` (M0 실행 이력이며 명백한 사실 오류가 없어 미수정) |

## 기존 문서와 코드의 불일치

- 기존 API/요구사항 문서는 `PostController`, `MemberController`, `MyController`, `AdminController`를 현재 이름처럼 사용했지만 실제 이름은 `PostViewController`, `MemberViewController`, `MyPageViewController`, `AdminViewController`다.
- 기존 요구사항 ID는 인증과 마이페이지 체계가 현재 요청의 `REQ-AUTH-*`, `REQ-MYPAGE-*`, `REQ-OBSERVABILITY-*`와 달랐다.
- 기존 ERD 문서는 Mermaid, DBML과 미래 Entity 확장안을 포함했다.
- 기존 FUTURE_FEATURES는 좋아요·첨부·신고·카테고리 등의 상세 설계를 승인된 현재 기능과 섞었다.
- M1 최초 초안은 M0 전 코드의 테스트 컴파일 실패와 마이페이지 오류를 기록했으나, fast-forward 후 실제 복구 상태와 성공 검증으로 갱신했다.

## 패키지와 아키텍처 특징

- 기능별 package 안에 Controller/DTO/Service/Repository/Entity를 배치하고 `global`이 횡단 관심사를 담당한다.
- `mypage`와 `admin`은 자체 Service 없이 다른 기능 Service를 조합한다.
- Service가 기능 경계를 넘어 다른 package Repository를 직접 참조해 삭제·상세 DTO 조립을 수행한다.
- `MemberService`, `PostService`는 read-only 기본 + 쓰기 override이고 `CommentService`는 읽기까지 전체 read-write transaction이다.
- Post 수정, Member/Comment 수정과 role 변경은 변경 감지를 사용한다.
- 조회수는 JPQL bulk update, 삭제는 cascade 대신 명시적 bulk delete 순서를 사용한다.

## 확인된 기술 부채

### Naming과 패키지

- `PostRecent`가 request package에 있고 사용되지 않는 `MemberUpdateRequest`, `MemberResponse`, 빈 `PostApiController`가 있다.
- `EditForm`, `MemberRemovalPolicy` 메서드가 lowerCamelCase 관례와 맞지 않는다.
- `MemberViewController`가 사용하지 않는 Post/Comment Service를 주입한다.

### Validation과 기능 계약

- 회원가입 email에 `@NotBlank`가 없고 nickname update는 null을 허용한다.
- DTO title/content/comment에 Entity column length 500 제한이 없다.
- 탈퇴 확인 문구는 서버가 검증하지 않는다.
- Admin role 문자열을 enum allow-list DTO 없이 직접 변환한다.
- 가입 오류 입력 복원은 Advice가 request/model을 직접 읽는 View 전용 로직이다.

### Security

- SOCIAL password 변경 제한이 View에만 있고 Service에 없다.
- Post edit GET은 permitAll이고 소유권 검증도 없다.
- Actuator 상세 health/metrics/mappings와 local test route가 공개된다.
- Google/Naver OAuth 필수 attribute null 처리와 LOCAL/SOCIAL email 연결 정책이 없다.
- login `redirect` parameter가 success handler에서 무시된다.

### JPA와 삭제

- Entity cascade/orphanRemoval이 없고 회원 삭제는 bulk query 순서에 의존한다.
- self-reference 댓글 자식/부모 삭제 순서를 보장하는 코드와 테스트가 없다.
- bulk delete는 persistence context auto-clear가 없다.
- nickname 및 `(provider,providerId)` DB unique가 없다.
- `ddl-auto=update`이고 migration 파일이 없다.

### Pagination과 조회

- 전체 게시글 제목 검색은 pagination하지 않는다.
- `PageRequestDto`의 page/size 범위 Validation이 없다.
- 빈 페이지에서 `PageResultDto.pageList` 표현은 별도 경계 검증이 필요하다.
- 검색과 상세 DTO 변환의 LAZY association query 수를 측정·보장하는 테스트가 없다.

### 테스트와 배포

- MVC slice는 filter를 제외하므로 별도 Security 통합 테스트와 함께 보아야 한다.
- CI는 test 후 bootJar를 만들지만 artifact를 EC2에 전달하지 않고 EC2가 source를 다시 pull/build한다.
- 배포 health check는 Actuator가 아닌 root HEAD이며 rollback/무중단 절차가 없다.
- Docker 실행 stage도 JRE가 아닌 JDK image다.

## 현재 기능상 제한

- 활성 JSON REST API 없음.
- 한 단계 대댓글만 허용하며 정렬·삭제 제약에 명시적 DB 정책 없음.
- 관리자 목록 pagination, 자기 삭제/최후 ADMIN 보호 없음.
- profile별 datasource/OAuth 설정이 Git 추적 파일에 없어 저장소 단독 실행 계약이 완결되지 않음.
- SOCIAL 비밀번호 정책과 회원 탈퇴 확인이 서버에서 완전히 강제되지 않음.

## 테스트와 문서 검증

| 검증 | 결과 |
| --- | --- |
| `.\gradlew.bat clean test` (M0 코드 단독) | 성공, 57개 테스트 |
| `.\gradlew.bat clean build` (M0 코드 단독) | 성공, test/check 및 Boot JAR 생성 |
| View Controller/Mapping 검색 | 다섯 View Controller와 local test route 확인 |
| Entity annotation 검색 | Member/Post/Comment/BaseEntity와 네 관계 확인 |
| 문서 class/method/URI 교차 검색 | 현재 공개 이름과 대조 |
| `git diff --check` | whitespace 오류 없음; LF→CRLF 경고만 허용 |

문서 최종 수정 후에도 `clean test`, `clean build`, `git diff --check`를 다시 실행해 커밋 전 결과를 확정한다.

## 실행하지 못한 검증

- `bootRun`/실제 MySQL: M0 실행에서 local MySQL 연결 거부가 기록됐고 이번 M1에서는 DB를 임의 구성하지 않았다.
- 실제 Google/Naver/Kakao login: 외부 provider와 비밀값 필요.
- 실제 Discord Webhook: 외부 전송과 secret 필요.
- EC2 deploy/Docker image 실행: 배포 환경과 별도 실행 시간이 필요.
- 운영 MySQL 실행계획/대용량 성능: H2 기반 테스트 범위 밖이다.

## 후속 단계 이관

1. 남은 Validation/Security/JPA/Pagination 부채를 현재 코드 품질 단계에서 정리한다.
2. profile별 설정 계약, secret 외부화·회전과 CI artifact/health/rollback 전략을 명시한다.
3. 현재 SSR 기능을 안정화한 뒤 M2 REST 계약을 별도로 설계한다.
4. React와 풋살 도메인은 로드맵 순서까지 보류한다.

## 문서 커밋 직전 Git 범위

변경은 `docs/**`에만 한정한다. Java, 테스트, View, Gradle, Dockerfile과 `.github`의 M0 변경은 이미 `d333e38`에 포함된 기준 코드이며 M1 문서 커밋에서 다시 수정하지 않는다. M1 문서 11개를 생성·갱신하고 legacy `docs/erd/board.dbml`만 제거한다.
