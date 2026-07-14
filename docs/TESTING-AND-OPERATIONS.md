# 테스트 및 운영 구성

> 기준: `refactor/pre-m2-quality-baseline` / 2026-07-14

## 테스트 기술과 현재 수

JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc, Spring Security Test와 `@DataJpaTest`를 사용한다. `src/test/java`에는 13개 테스트 클래스와 69개 `@Test` 메서드가 있다. H2는 `testRuntimeOnly`이며 `application-test.properties`가 test datasource와 외부 연동 비활성 값을 제공한다. JPA slice는 내장 DB 자동 구성을 사용하고 `JpaConfig`를 import한다.

| 테스트 클래스 | 메서드 수 | 소스가 의도한 보장 범위 |
| --- | ---: | --- |
| `BoardApplicationTests` | 1 | test profile 전체 context load |
| `EntityDomainTest` | 4 | Member 생성/변경, Post 검증·연결, Comment tree/변경 |
| `MemberRepositoryTest` | 2 | email/중복 및 provider 조회 |
| `PostRepositoryTest` | 3 | fetch 목록·검색, 내 글/count, 조회수 bulk update·회원 글 delete |
| `CommentRepositoryTest` | 4 | 자기참조 답글 우선 bulk delete, 회원·게시글 삭제, 자동 clear, 내 댓글 query |
| `MemberServiceTest` | 10 | 가입 정책, 조회, SOCIAL password 차단, role, 탈퇴 확인·삭제 순서, not found |
| `PostServiceTest` | 11 | CRUD, 수정 화면 소유권, 삭제 순서, 조회수, 검색 pagination·범위 초과 page, 내 글 |
| `CommentServiceTest` | 6 | 댓글/대댓글, parent/owner 규칙, 내 댓글 조회 |
| `ControllerMvcTest` | 7 | View Controller, 검색어·page 전달, 민감 입력 제거, 잘못된 page/role, 익명 댓글 분기 |
| `ExceptionAndValidationTest` | 7 | CustomException, Advice, DTO 길이·필수값·page 범위, 가입 비밀번호 제외 입력 보존 |
| `SecurityConfigTest` | 6 | 익명/ADMIN/CSRF, 수정 GET, Actuator 접근, local route profile, login redirect |
| `OAuthTest` | 5 | provider 파싱, Kakao fallback, 신규/기존 OAuth 회원, Naver 오류 |
| `PageResultDtoTest` | 3 | 빈 결과, 10개 미만 page 블록, 첫·마지막 page 블록 |

### Security filter와 외부 호출

- `ControllerMvcTest`: `@WebMvcTest`, `@AutoConfigureMockMvc(addFilters=false)`이므로 Controller 분기만 확인하고 실제 인증/CSRF filter는 제외한다.
- `SecurityConfigTest`: `@SpringBootTest`, 기본 `@AutoConfigureMockMvc`로 filter를 포함하며 mock Authentication과 CSRF request processor를 사용한다.
- `OAuthTest`: 실제 provider에 접속하지 않고 JDK `HttpServer`를 임의 localhost port에 열어 userinfo JSON을 반환한다. Repository/PasswordEncoder는 Mockito다.

## 요구 명령 실행 결과

PRE-M2 품질 개선 후 2026-07-14에 최종 검증으로 다음 명령을 실행했다.

```powershell
.\gradlew.bat clean build
```

결과: **성공**, 69개 실행, 실패 0, error 0, skip 0. 실행 가능한 `board-0.0.1-SNAPSHOT.jar`와 plain JAR가 생성됐다.

## 로컬 실행

### 전제

- JDK 17(Gradle toolchain이 자동 탐색/다운로드할 수 있음)
- MySQL(개발 compose image는 8.0.33)
- datasource와 OAuth client 설정. 이 로컬 HEAD에는 profile별 properties가 없어 정확한 모든 환경변수 이름을 확정할 수 없다.

저장소에서 확인 가능한 변수 이름은 다음뿐이다. 값은 문서화하지 않는다.

- `SPRING_PROFILES_ACTIVE` (기본 `local`)
- `DISCORD_WEBHOOK_URL`, `DISCORD_WEBHOOK_ENABLED`
- 개발 MySQL compose: `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD`
- GitHub Actions SSH 전달: `DB_USERNAME`, `DB_PASSWORD`
- Actions secret 이름: `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY`, `EC2_PORT`

Google/Naver/Kakao client ID·secret과 datasource URL의 실제 키는 누락된 외부/profile 설정에 있으므로 이 HEAD만으로 확인할 수 없다.

### 실행과 health

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
.\gradlew.bat bootRun
```

외부 설정이 준비된 뒤 `http://localhost:8080/actuator/health`에서 health를 확인한다. 공통 설정은 `health,info,metrics,mappings`를 노출하지만 health만 익명 허용하고 나머지는 ADMIN으로 제한한다. `prod` profile은 `health,info`만 노출하고 health detail을 숨긴다.

## JPA, P6Spy와 로그

- 공통 `ddl-auto=update`, `show-sql=false`.
- 댓글과 회원 삭제는 cascade 대신 답글→부모 댓글→게시글→회원 명시 순서를 사용한다. 관련 bulk delete와 회원 게시글 bulk delete는 `clearAutomatically=true`로 실행 후 영속성 컨텍스트를 비운다. 조회수 bulk update는 기존의 자동 flush/clear를 유지한다.
- P6Spy starter 2.0.0과 `P6SpyFormatter`; local Logback profile에서 `p6spy` INFO.
- console, `logs/{app}.log`, error 전용 rolling file. 일반 로그는 30일/총 1GB, error는 60일/총 1GB 정책.
- `JpaConfig` audit는 principal nickname 또는 `system`.

## Discord 알림

`GlobalViewControllerAdvice`는 가입의 duplicate email/nickname/password mismatch를 제외한 `CustomException`을 `DiscordNotifier`에 전달한다. enabled=false, 빈 URL, 전송 예외에서는 요청 실패를 추가로 만들지 않는다. local profile의 `GET /test/discord-error`가 의도적으로 `POST_NOT_FOUND`를 발생시킨다. 실제 Webhook 통합 테스트는 없다.

## GitHub Actions CI/CD

로컬 `.github/workflows/gradle.yml`의 현재 동작이다.

- trigger: `master` 대상 pull request와 `master` push.
- build: Ubuntu, checkout, Temurin 17, Gradle setup, `./gradlew clean test` 후 `./gradlew bootJar`.
- deploy: push일 때만 build job 이후 EC2 SSH.
- EC2: `$HOME/apps/board`에서 `git pull origin master` → 다시 `./gradlew clean build` → 기존 PID에 TERM → 새 JAR를 `prod,oauth` profile로 nohup 실행.
- 검증: process 존재 확인 후 최대 30회 `curl -I http://localhost:8080`; Actuator health가 아니라 root HEAD 응답을 본다.

현재 한계:

- CI build job은 test 성공을 bootJar와 deploy의 선행 조건으로 둔다. EC2도 `clean build`로 다시 검증한다.
- CI가 만든 JAR artifact를 저장/전달하지 않고 EC2가 source를 다시 pull/build하므로 검증 산출물과 배포 산출물이 동일하지 않다.
- pull/build와 프로세스 중단 사이 실패 복구·rollback·무중단 전략이 없다.
- health 검증이 `/actuator/health`가 아니고 root HEAD의 상태 코드 내용도 확인하지 않는다.

## Docker와 Compose

- `Dockerfile`: Temurin 17 다단계, container 내부 `./gradlew clean build`, 실행 단계도 `17-jdk`, `java -jar app.jar`.
- `docker-compose.yaml`: app을 build하고 8080 노출, `.env`, `SPRING_PROFILES_ACTIVE=prod`, restart policy.
- `docker-compose.dev.yaml`: MySQL 8.0.33만 제공하고 named volume을 사용한다.
- Dockerfile/Compose는 현재 GitHub Actions EC2 실행 JAR 배포 흐름에서 사용되지 않는다.

## 모니터링 스크립트

`scripts/monitor-board.ps1`은 기본 기대 브랜치 `master`, 최근 변경·민감 파일·TODO를 조사하고 `clean test bootJar` 또는 선택된 test 제외 build를 실행한 뒤 선택적으로 Discord Webhook에 결과를 보낸다. 이번 M1에서는 외부 전송을 수행하지 않았고 스크립트도 실행하지 않았다. 필수 검증은 직접 실행한 `clean test`, `clean build`, `git diff --check`로 수행한다.
