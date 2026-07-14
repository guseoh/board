# M1 As-Is 조사 보고서

> 현재 기준: `master` / `d06ee0a71e2f35315c223b5e6975430387f51bed` / 갱신일 2026-07-14 (Asia/Seoul)

## 작업 기준

| 항목               | 확인 결과                                                                     |
| ---------------- | ------------------------------------------------------------------------- |
| 저장소              | `guseoh/board`, `C:\Users\guseo\IdeaProjects\board`                       |
| 현재 기준 브랜치        | `master`                                                                  |
| 현재 기준 HEAD       | `d06ee0a71e2f35315c223b5e6975430387f51bed` (`Merge pull request #98`)     |
| M1 작업 브랜치        | `recover`                                                                 |
| M1 문서 작성 기준 HEAD | `d333e3868e5bb94073030780ce0910a65b3ef4d8` (`docs: M0 프로젝트 복구 결과 보고서 작성`) |
| 최초 M1 조사 HEAD    | `31326e715d39d2a4af9153fb4ccbcf72dc4fb229`                                |
| 동기화              | M1 문서를 stash로 보관하고 `pull --ff-only`로 원격 M0 복구 커밋 5개를 반영한 뒤 재검토            |
| 통합 결과            | PR #97로 `recover → develop`, PR #98로 `develop → master` 병합 완료                  |
| `master` 반영 상태   | 반영 완료. PR #98 merge commit이 현재 `master` 기준선                                 |

반영된 M0 커밋은 회원가입·OAuth 오류 처리, 마이페이지 조회·통계, 테스트 코드, CI·Docker 검증과 M0 보고서를 복구한다. M1 문서는 동기화된 코드와 57개 테스트를 기준으로 작성했으며, PR #97을 통해 `develop`에 통합했다.

현재 M0 복구 코드와 M1 문서는 PR #98을 통해 안정·배포 기준 브랜치인 `master`에 반영됐다.

## 조사 범위

* 빌드·설정: `build.gradle`, settings, Gradle Wrapper, 공통 properties, Logback, P6Spy
* main code: `member`, `post`, `comment`, `mypage`, `admin`, `global` Java 파일
* 요청 흐름: View Controller, 빈 API 클래스, local test Controller, DTO·Validation, Service, Repository, Entity
* 인증: Security Filter Chain, form/OAuth2 login, logout, `UnifiedPrincipal`, user services, success handler, provider adapters
* 화면: 16개 Thymeleaf template의 form/action/security 표현식과 `static/css/app.css`
* 테스트: 12개 테스트 클래스, 57개 test method와 테스트 구성
* 운영: GitHub Actions workflow, Dockerfile, 두 Compose 파일, Actuator, Discord notifier, monitor script
* 기존 문서: API, ERD, REQUIREMENTS, FUTURE_FEATURES, DBML과 `M0-RECOVERY-REPORT.md`

## 현재 시스템 요약

현재 시스템은 Java 17과 Spring Boot 4.0.1 기반의 Session 인증 SSR 게시판이다.

요청은 다음 흐름으로 처리된다.

```text
Browser
→ Spring Security Filter Chain
→ View Controller
→ Service
→ JPA Repository
→ MySQL
→ Thymeleaf HTML 또는 Redirect
```

게시글, 댓글·대댓글, 회원, 관리자, 마이페이지 기능과 Google·Naver·Kakao OAuth2 login이 구현돼 있다. 현재 활성화된 JSON REST API는 없다.

M0에서 `/my/posts`가 회원, 검색어와 페이지 조건을 하나의 Repository query에 적용하도록 복구됐다. 회원별 오늘 작성 글 수와 누적 조회수도 올바른 Service 결과를 사용한다.

회원가입 정책 오류가 발생하면 nickname과 email만 복원하고 password는 다시 노출하지 않는다. Kakao에서 email을 제공하지 않으면 `kakao_{id}@oauth.local` 형식의 대체 이메일을 사용한다.

## 생성·수정한 문서

| 구분 | 문서                                                                                                                                              |
| -- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| 생성 | `README.md`, `PROJECT-OVERVIEW.md`, `ARCHITECTURE.md`, `PACKAGE-STRUCTURE.md`, `SECURITY.md`, `TESTING-AND-OPERATIONS.md`, `M1-AS-IS-REPORT.md` |
| 갱신 | `REQUIREMENTS.md`, `API.md`, `ERD.md`, `FUTURE_FEATURES.md`                                                                                     |
| 제거 | `erd/board.dbml` — M1 범위에서 제외된 DBML legacy artifact                                                                                             |
| 유지 | `M0-RECOVERY-REPORT.md` — M0 실행 이력이며 명백한 사실 오류가 없어 미수정                                                                                          |

## 기존 문서와 코드의 불일치

* 기존 API·요구사항 문서는 `PostController`, `MemberController`, `MyController`, `AdminController`를 현재 클래스명처럼 사용했지만 실제 이름은 `PostViewController`, `MemberViewController`, `MyPageViewController`, `AdminViewController`다.
* 기존 요구사항 ID는 인증과 마이페이지 체계가 현재 문서의 `REQ-AUTH-*`, `REQ-MYPAGE-*`, `REQ-OBSERVABILITY-*`와 달랐다.
* 기존 ERD 문서는 Mermaid, DBML과 미래 Entity 확장안을 현재 구조와 함께 포함했다.
* 기존 `FUTURE_FEATURES.md`는 좋아요, 첨부, 신고, 카테고리 등의 미래 설계를 현재 승인 기능과 혼합했다.
* 최초 M1 초안은 M0 이전 코드의 테스트 컴파일 실패와 마이페이지 오류를 기록했으나, 원격 M0 복구 커밋 반영 후 실제 복구 상태와 성공 검증으로 갱신했다.

## 패키지와 아키텍처 특징

* 기능별 package 안에 Controller, DTO, Service, Repository와 Entity를 배치하고 `global`이 횡단 관심사를 담당한다.
* `mypage`와 `admin`은 자체 Service 없이 회원, 게시글과 댓글 Service를 조합한다.
* 일부 Service는 다른 기능 package의 Repository를 직접 참조해 삭제 흐름이나 상세 DTO 조립을 수행한다.
* `MemberService`, `PostService`는 read-only 트랜잭션을 기본으로 적용하고 쓰기 메서드에서 별도로 트랜잭션을 연다.
* `CommentService`는 조회 메서드를 포함해 클래스 전체가 read-write 트랜잭션이다.
* 게시글, 회원, 댓글 수정과 권한 변경은 JPA 변경 감지를 사용한다.
* 조회수 증가는 JPQL bulk update를 사용한다.
* 연관 Entity 삭제는 cascade보다 명시적인 bulk delete 순서에 의존한다.

## 확인된 기술 부채

### Naming과 패키지

* `PostRecent`가 request package에 위치한다.
* 사용되지 않는 `MemberUpdateRequest`, `MemberResponse`와 빈 `PostApiController`가 존재한다.
* `EditForm`, `MemberRemovalPolicy` 메서드는 Java lowerCamelCase 관례와 맞지 않는다.
* `MemberViewController`가 사용하지 않는 Post·Comment Service를 주입한다.

### Validation과 기능 계약

* 회원가입 email 필드에 `@NotBlank`가 없다.
* nickname 수정 요청이 null을 허용한다.
* 게시글 title·content와 댓글 content DTO에 Entity의 500자 제한이 반영되지 않았다.
* 회원 탈퇴 확인 문구를 서버에서 검증하지 않는다.
* 관리자 Role 변경 요청은 허용된 Enum 값만 받는 DTO 없이 문자열을 직접 변환한다.
* 회원가입 오류 입력 복원은 Controller Advice가 request와 model을 직접 읽는 View 전용 로직이다.

### Security

* SOCIAL 회원의 비밀번호 변경 제한이 View에만 있고 Service에서 강제되지 않는다.
* 게시글 수정 화면 GET 요청은 Security 설정상 공개되며 작성자 소유권 검증도 수행하지 않는다.
* Actuator의 상세 health, metrics, mappings는 익명 사용자에게 공개된다. local test route는 `local` profile에서만 등록되지만 해당 profile에서는 공개된다.
* Google·Naver OAuth 필수 attribute의 null 처리 정책이 충분하지 않다.
* 동일 이메일을 사용하는 LOCAL·SOCIAL 계정 연결 정책이 없다.
* 로그인 form의 `redirect` parameter를 success handler가 사용하지 않는다.

### JPA와 삭제

* Entity에 cascade와 orphanRemoval이 없으며 회원 삭제는 bulk query 실행 순서에 의존한다.
* 자기참조 댓글의 자식·부모 삭제 순서를 보장하는 코드와 테스트가 없다.
* bulk delete 이후 영속성 컨텍스트를 자동으로 비우지 않는다.
* nickname과 `(provider, providerId)`에 DB unique 제약이 없다.
* `ddl-auto=update`를 사용하고 있으며 migration 파일이 없다.

### Pagination과 조회

* 전체 게시글 제목 검색은 pagination을 사용하지 않는다.
* `PageRequestDto`의 page와 size 범위 Validation이 없다.
* 빈 결과 페이지에서 `PageResultDto.pageList`가 올바르게 표현되는지 경계 검증이 필요하다.
* 검색과 상세 DTO 변환 과정의 LAZY association query 수를 측정하거나 보장하는 테스트가 없다.

### 테스트와 배포

* MVC slice 테스트는 Security Filter를 제외하므로 별도 Security 통합 테스트와 함께 해석해야 한다.
* CI는 테스트 후 Boot JAR를 생성하지만 해당 artifact를 EC2에 전달하지 않는다.
* EC2는 배포 시 source를 다시 pull하고 `clean build`를 실행한다.
* 배포 health check는 Actuator가 아니라 root 경로의 HEAD 요청을 사용한다.
* rollback과 무중단 배포 절차가 없다.
* Docker 실행 stage도 JRE가 아닌 JDK image를 사용한다.

## 현재 기능상 제한

* 활성 JSON REST API가 없다.
* 대댓글은 한 단계만 허용한다.
* 대댓글 정렬과 삭제 제약에 명시적인 DB 정책이 없다.
* 관리자 회원·게시글 목록에 pagination이 없다.
* 관리자의 자기 계정 삭제와 최후 ADMIN 보호 정책이 없다.
* profile별 datasource와 OAuth 설정이 Git 추적 파일에 포함되지 않아 저장소 단독 실행 계약이 완결되지 않았다.
* SOCIAL 비밀번호 정책과 회원 탈퇴 확인이 서버에서 완전히 강제되지 않는다.

## 테스트와 문서 검증

| 검증                          | 결과                                              |
| --------------------------- | ----------------------------------------------- |
| `.\gradlew.bat clean test`  | 성공, 57개 테스트                                     |
| `.\gradlew.bat clean build` | 성공, 57개 테스트와 Boot JAR 생성                        |
| View Controller·Mapping 검색  | 다섯 View Controller와 local test route 확인         |
| Entity annotation 검색        | Member, Post, Comment, BaseEntity와 네 개의 연관관계 확인 |
| 문서 class·method·URI 교차 검색   | 현재 공개 클래스명과 Mapping을 코드와 대조                     |
| `git diff --check`          | whitespace 오류 없음. LF→CRLF 경고만 확인                |
| PR #97                      | `recover → develop` merge commit 방식으로 병합 완료     |
| PR #98                      | `develop → master` merge commit `d06ee0a71e2f35315c223b5e6975430387f51bed`로 병합 완료 |
| master Push Workflow        | Run `29306357220`의 build Job에서 테스트와 Boot JAR 생성 성공 |

M0 복구 코드와 M1 문서에 대한 정적 검증, 테스트, 빌드와 `master` 기준선 반영은 완료됐다.

## 런타임·외부 환경 검증

### 로컬 MySQL과 `bootRun`

로컬 MySQL과 Docker를 함께 실행할 때 개발 장비의 리소스 부담이 예상돼 사용자 승인에 따라 실제 실행을 생략했다.

이 항목은 실제 실행 성공을 의미하지 않는다. 사용자 승인에 따라 M1 완료를 막는 조건에서는 제외했으며, 로컬 애플리케이션 기동은 검증하지 않았다.

### 외부 연동

다음 검증은 외부 계정이나 Secret이 필요해 실행하지 않았다.

* Google·Naver·Kakao 실제 OAuth2 login
* 실제 Discord Webhook 전송

해당 설정이 준비되지 않은 경우 M1 완료를 막는 필수 조건으로 취급하지 않는다.

### 배포와 운영 환경

PR #98로 `develop → master` 병합을 완료했고, master Push Workflow Run `29306357220`의 build Job에서 테스트와 Boot JAR 생성이 성공했다.

Deploy Job은 사용자가 비용과 운영 관리를 위해 의도적으로 중지한 EC2 인스턴스에 SSH 연결을 시도하다 timeout됐다. 따라서 애플리케이션 배포 명령은 실행되지 않았고, 애플리케이션 기동, 배포 후 Health Check와 외부 서비스 기본 화면은 실제로 검증하지 않았다. 이는 애플리케이션 오류로 판정하지 않으며, EC2를 다시 운영할 때 별도 운영 작업으로 검증한다.

다음 항목은 미검증 또는 후속 운영 범위다.

* EC2 애플리케이션 배포와 기동
* 배포 후 애플리케이션 Health Check
* 외부 서비스 기본 화면 확인
* Docker image 직접 실행
* 운영 MySQL 실행계획과 대용량 성능 검증

Docker image 직접 실행과 운영 MySQL 성능 검증은 각각 운영 개선 단계와 M4 성능 검증 단계로 이관할 수 있다.

## M1 완료 결과

M1은 다음 근거로 완료 처리한다.

* M0 코드 복구와 M1 As-Is 문서화 완료
* 관련 57개 테스트와 `clean build` 성공
* PR #97의 `recover → develop` 및 PR #98의 `develop → master` 병합 완료
* master Push Workflow Run `29306357220`에서 테스트와 Boot JAR 생성 성공
* 로컬 MySQL, Docker, `bootRun`, 실제 OAuth2 login과 Discord Webhook 미검증 사실 기록

EC2 Deploy Job 실패는 의도적으로 중지된 인스턴스의 SSH timeout이며 애플리케이션 배포나 기동 실패가 아니다. EC2 애플리케이션 기동과 Health Check는 검증되지 않았으므로 성공으로 기록하지 않고, 인스턴스를 다시 운영할 때 수행할 별도 운영 작업으로 이관한다.

## 후속 단계 이관

1. Validation과 Security 계약을 기존 SSR 동작을 유지하는 범위에서 우선 정리한다.
2. JPA 삭제 무결성과 Pagination·검색 계약을 독립된 작업으로 정리한다.
3. profile별 설정 계약과 Secret 외부화·회전 정책을 명시한다.
4. EC2를 다시 운영할 때 배포, 애플리케이션 기동과 Health Check를 별도 운영 작업으로 검증한다.
5. CI artifact 전달, Actuator Health Check와 rollback 전략을 순차적으로 개선한다.
6. 현재 SSR 기능과 서버 계약을 안정화한 뒤 M2 REST API 계약을 별도로 설계한다.
7. React 전환과 풋살 도메인 설계는 로드맵 순서까지 보류한다.

## 현재 Git 상태와 기준선

M0 복구와 M1 문서 변경은 PR #97을 통해 `develop`에, PR #98을 통해 `master`에 병합됐다.

현재 기준은 다음과 같다.

```text
master (PR #98 merge commit d06ee0a7...)
└── develop (PR #97 merge result)
    └── M0 프로젝트 복구
    └── M1 As-Is 문서화
```

`master`는 M1 코드·문서 기준선을 포함한다. 다음 작업은 M2 REST API 설계 전에 확정 가능한 Validation, Security, JPA와 Pagination 품질 부채를 정리하는 것이다.
