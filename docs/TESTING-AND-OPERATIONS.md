# 테스트 및 운영 구성

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

## 테스트 기술과 현재 수

JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc, Spring Security Test와 `@DataJpaTest`를 사용한다. `src/test/java`에는 12개 테스트 클래스와 57개 `@Test` 메서드가 있다. H2는 `testRuntimeOnly`이며 저장소에 `application-test.properties`는 없다. JPA slice는 내장 DB 자동 구성을 사용하고 `JpaConfig`를 import한다.

| 테스트 클래스 | 메서드 수 | 소스가 의도한 보장 범위 |
| --- | ---: | --- |
| `BoardApplicationTests` | 1 | test profile 전체 context load |
| `EntityDomainTest` | 4 | Member 생성/변경, Post 검증·연결, Comment tree/변경 |
| `MemberRepositoryTest` | 2 | email/중복 및 provider 조회 |
| `PostRepositoryTest` | 3 | fetch 목록·검색, 내 글/count, 조회수 bulk update·회원 글 delete |
| `CommentRepositoryTest` | 2 | bulk deletes, 내 댓글 통계·projection query |
| `MemberServiceTest` | 9 | 가입 정책, 조회, profile, nickname/password, role, 삭제 순서, not found |
| `PostServiceTest` | 9 | CRUD, 상세/list, 소유권, 관리자 삭제, 조회수, 검색·내 글 |
| `CommentServiceTest` | 6 | 댓글/대댓글, parent/owner 규칙, 내 댓글 조회 |
| `ControllerMvcTest` | 6 | 다섯 View Controller와 익명 댓글 분기 |
| `ExceptionAndValidationTest` | 6 | CustomException, Advice, DTO/Bean Validation, 가입 오류의 비밀번호 제외 입력 보존 |
| `SecurityConfigTest` | 4 | 익명 제한, ADMIN 권한, CSRF, 역할별 login redirect |
| `OAuthTest` | 5 | provider 파싱, Kakao fallback, 신규/기존 OAuth 회원, Naver 오류 |

### Security filter와 외부 호출

- `ControllerMvcTest`: `@WebMvcTest`, `@AutoConfigureMockMvc(addFilters=false)`이므로 Controller 분기만 확인하고 실제 인증/CSRF filter는 제외한다.
- `SecurityConfigTest`: `@SpringBootTest`, 기본 `@AutoConfigureMockMvc`로 filter를 포함하며 mock Authentication과 CSRF request processor를 사용한다.
- `OAuthTest`: 실제 provider에 접속하지 않고 JDK `HttpServer`를 임의 localhost port에 열어 userinfo JSON을 반환한다. Repository/PasswordEncoder는 Mockito다.

## 요구 명령 실행 결과

원격 M0 복구 반영 후 2026-07-13에 다음 명령을 실행했다.

```powershell
.\gradlew.bat test
```

결과: **성공**, 57개 테스트 실행. `compileTestJava`, H2 Repository query, context load, MVC, Security와 OAuth 테스트가 모두 완료됐다. 이어서 `.\gradlew.bat clean build`도 성공하여 test/check와 Boot JAR 생성을 확인했다.

M0에서 테스트의 과거 이름을 `createComment`, `getMyCommentPage`, `countMyComment`, `createPost`, `getPostDetail`, `getPosts`, `getPostsForAdmin`, `countMyPosts`, `getMyPosts` 등 현재 공개 메서드명으로 복구했다. M1 문서 적용 후에도 같은 두 명령을 다시 실행해 성공 여부를 최종 확인한다.

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

외부 설정이 준비된 뒤 `http://localhost:8080/actuator/health`에서 health를 확인한다. 공통 설정은 `health,info,metrics,mappings`를 노출하고 health detail을 항상 표시한다. 인증 제한이 없다는 점에 주의한다.

## JPA, P6Spy와 로그

- 공통 `ddl-auto=update`, `show-sql=false`.
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
