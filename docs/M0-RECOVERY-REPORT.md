# M0 프로젝트 복구 보고서

## 1. 작업 개요

- 작업 목적: 중단된 Board 프로젝트에서 메인 코드, 테스트, 로컬 빌드, CI 검증 흐름의 기준선을 다시 일치시킨다.
- 기준 브랜치: `master`
- 기준 커밋: `31326e7` (`chore: add scripts`)
- 로컬 경로: `C:\Users\guseo\IdeaProjects\board`
- 수정 전 작업 트리: 추적·미추적 변경 없음
- 시스템 Java: OpenJDK Temurin 21.0.11
- Gradle 실행 Java: 시스템 명령은 Java 21을 사용하며, 프로젝트 Java toolchain은 Java 17이다. `bootRun`은 설치된 Java 17.0.6을 사용했다.
- Gradle: 9.2.1
- Spring Boot: 4.0.1
- 문서 위치: 기존 `docs`가 주제별 파일을 직하위에 두는 평면 구조이므로 `docs/M0-RECOVERY-REPORT.md`를 사용했다.

## 2. 수정 전 상태

### 최초 테스트 결과

`\.\gradlew.bat clean test`는 `compileTestJava`에서 실패했다. 테스트는 한 건도 실행되지 않았다.

- 테스트 컴파일 오류: 26건
- 직접 원인: 서비스 메서드 네이밍 정리 이후 테스트의 호출과 Mockito 설정이 과거 이름을 유지했다.
- 주요 불일치:
  - `save` → `createPost`
  - `findOne` → `getPostDetail`
  - `findAll` → `getPosts`
  - `findAllAdmin` → `getPostsForAdmin`
  - `myPostCount` → `countMyPosts`
  - `myPosts` → `getMyPosts`
  - `create` → `createComment`
  - `myCommentCount` → `countMyComment`
  - `myCommentPage` → `getMyCommentPage`

### 최초 빌드 결과

`\.\gradlew.bat clean build`도 `compileTestJava`에서 같은 26건의 오류로 실패했다. 메인 소스 컴파일과 `bootJar` 조립은 그 전 단계에서 성공했지만, 전체 `build`는 실패 상태였다.

### 추가로 확인한 기존 기능 오류

- `/my/posts`가 전체 게시글의 페이지 정보와 현재 회원의 전체 게시글 `List`를 함께 사용했다. 목록, 검색 조건, 페이지 번호가 하나의 조회 결과를 나타내지 않았다.
- 마이페이지의 “오늘 작성”은 전체 회원의 오늘 게시글 수를, “총 조회수”는 현재 회원의 오늘 게시글 수를 표시했다.
- 회원가입 중복 예외 처리에서 기존 요청 객체를 새 객체로 덮어 닉네임과 이메일까지 유실했다.
- Kakao가 이메일을 제공하지 않을 때 `@`가 없고 `local`의 철자가 틀린 대체 이메일을 만들었다.

### CI 문제

- GitHub Actions build job이 `clean build -x test`를 실행해 테스트를 제외했다.
- EC2 deploy 스크립트도 `clean build -x test`를 사용했다.
- Docker build 단계도 `bootJar -x test`를 사용했다.
- 따라서 테스트 실패가 원격 build/deploy 또는 이미지 빌드를 막지 못했다.

## 3. 구현한 부분

M0는 기준선 복구 단계이므로 신규 사용자 기능은 구현하지 않았다. 기존 마이페이지 기능을 정상화하기 위한 내부 조회 두 가지를 구현했다.

### 회원별 게시글 페이지 조회

- 목적: 현재 회원, 검색어, 페이지 정보가 같은 DB 조회 결과를 사용하게 한다.
- 관련 파일: `PostRepository`, `PostService`, `MyPageViewController`
- 동작: Repository가 회원 ID와 제목 검색어를 조건으로 `Page<Post>`를 반환하고, Service가 `PageResultDto<PostListResponse, Post>`로 변환한다. Controller는 같은 객체의 DTO 목록과 페이지 메타데이터를 화면에 전달한다.
- 영향: `/my/posts`만 변경하며 전체 게시글 목록과 관리자 게시글 목록의 조회 흐름은 유지한다.

### 회원별 게시글 조회수 합계

- 목적: 마이페이지의 “총 조회수”에 오늘 작성 글 개수가 표시되던 오류를 복구한다.
- 관련 파일: `PostRepository`, `PostService`, `MyPageViewController`
- 동작: `sum(p.viewCount)`를 회원 ID로 집계하고 게시글이 없을 때 `0`을 반환한다.
- 영향: 기존 원자적 조회수 증가 쿼리는 변경하지 않았다.

## 4. 추가한 부분

- `PostRepository.findMyPosts`: 회원·검색어·페이징을 하나의 JPQL 조회와 count query로 처리한다.
- `PostRepository.sumViewCountByMemberId`: 회원별 누적 조회수를 계산한다.
- `PostService.countMyPostViews`: 인증 회원 식별자 검증 후 조회수 합계를 반환한다.
- 회원가입 중복 오류에서 안전한 입력값만 유지하는 회귀 테스트 1건을 추가했다.
- Repository 테스트에 회원별 검색·페이징과 조회수 합계 검증을 추가했다.
- Controller 테스트에 동일 페이지 결과 사용과 마이페이지 통계 매핑 검증을 추가했다.
- CI에 명시적인 `Run tests`와 `Build boot jar` 단계를 추가했다.
- 이 M0 복구 보고서를 추가했다.

공통 `TestFixtures`는 현재 Entity 팩토리, `UnifiedPrincipal`, `Role`, `LoginType`과 일치했다. ID와 감사 필드에만 제한적으로 reflection을 사용하므로 변경하지 않았다.

## 5. 개선한 부분

- Service와 Controller 테스트의 Mockito 호출을 실제 공개 메서드 이름과 일치시켰다.
- `/my/posts`가 전체 데이터를 한 번에 `List`로 읽지 않고 DB 페이징을 사용하도록 개선했다.
- 게시글 목록과 페이지 메타데이터가 동일한 쿼리 결과에서 나오게 했다.
- 회원가입 오류 시 닉네임·이메일은 유지하되 비밀번호와 확인 비밀번호는 모델에 다시 넣지 않도록 했다.
- Repository 테스트는 실제 H2 DB를 flush/clear한 뒤 쿼리 결과를 검증한다. 조회수 증가의 원자적 bulk update 테스트도 유지했다.
- CI는 테스트 실패 시 다음 build step과 `needs: build`인 deploy job으로 진행하지 않는 fail-fast 흐름이 됐다.
- Docker build도 테스트가 포함된 `clean build`를 사용해 로컬·CI·이미지 검증 기준을 일치시켰다.

## 6. 수정한 부분

- 오래된 Post/Comment Service 테스트 메서드 호출 26곳을 현재 메인 코드의 이름으로 수정했다.
- Controller MVC 테스트의 Service mock과 verify 대상을 현재 Controller 호출에 맞췄다.
- `/my/posts` Controller가 전체 게시글 페이지와 회원별 전체 목록을 혼합하던 로직을 회원별 페이지 하나로 수정했다.
- “오늘 작성”은 회원별 오늘 게시글 수, “총 조회수”는 회원별 누적 조회수를 사용하도록 수정했다.
- Kakao 대체 이메일을 `kakao_{providerId}@oauth.local` 형식으로 수정했다. provider ID를 포함하므로 회원 간 충돌도 방지한다.
- 회원가입 중복 예외에서 비밀번호를 제외한 닉네임·이메일을 보존하도록 수정했다.
- GitHub Actions와 EC2 deploy의 `-x test`를 제거했다.
- Docker build의 `-x test`를 제거했다.

## 7. 변경 파일 목록

| 파일 | 유형 | 주요 변경 및 이유 |
| --- | --- | --- |
| `.github/workflows/gradle.yml` | 수정·개선 | 테스트와 Boot JAR 단계를 분리하고 deploy의 테스트 제외를 제거했다. |
| `Dockerfile` | 수정 | 이미지 빌드도 전체 검증을 수행하도록 변경했다. |
| `src/main/java/project/board/global/exception/handler/GlobalViewControllerAdvice.java` | 수정 | 가입 오류 입력 보존과 비밀번호 재표시 방지를 적용했다. |
| `src/main/java/project/board/global/security/oauth/KakaoUserInfo.java` | 수정 | 이메일 미제공 시 유효하고 고유한 대체 이메일을 생성한다. |
| `src/main/java/project/board/mypage/controller/view/MyPageViewController.java` | 수정 | 회원별 목록·페이지·통계를 올바른 Service 결과로 연결한다. |
| `src/main/java/project/board/post/repository/PostRepository.java` | 구현·추가 | 회원별 검색/페이징과 조회수 합계 쿼리를 추가했다. |
| `src/main/java/project/board/post/service/PostService.java` | 구현·수정 | 회원별 페이지 DTO 변환과 조회수 합계 조회를 제공한다. |
| `src/test/java/project/board/comment/service/CommentServiceTest.java` | 수정 | 현재 CommentService 이름으로 호출을 복구했다. |
| `src/test/java/project/board/controller/ControllerMvcTest.java` | 수정·개선 | 현재 메서드 mock과 마이페이지 결과 매핑을 검증한다. |
| `src/test/java/project/board/global/exception/ExceptionAndValidationTest.java` | 추가 | 가입 오류의 안전한 입력 보존을 검증한다. |
| `src/test/java/project/board/global/security/oauth/OAuthTest.java` | 수정 | 올바른 Kakao 대체 이메일을 검증한다. |
| `src/test/java/project/board/post/repository/PostRepositoryTest.java` | 추가·개선 | 회원별 검색/페이징과 조회수 합계 DB 쿼리를 검증한다. |
| `src/test/java/project/board/post/service/PostServiceTest.java` | 수정·개선 | 현재 이름과 페이지/조회수 반환값을 검증한다. |
| `docs/M0-RECOVERY-REPORT.md` | 문서 | 기준 상태, 변경, 검증, 영향, 후속 작업을 기록한다. |

## 8. 테스트와 검증

### 실행 결과

| 명령 | 결과 |
| --- | --- |
| `\.\gradlew.bat compileTestJava` | 성공 |
| `\.\gradlew.bat test` | 복구 중 50개 성공 후 변경 테스트를 포함해 최종 57개 성공 |
| 관련 테스트 5개 클래스 선택 실행 | 성공 |
| `\.\gradlew.bat clean test` | 성공, 57개 성공, 실패 0, skipped 0 |
| `\.\gradlew.bat clean build` | 성공, 57개 성공, Boot JAR 생성 |
| `\.\gradlew.bat bootRun` | 실패: 로컬 MySQL `127.0.0.1:3307` 연결 거부 |
| `git diff --check` | 오류 없음 |
| `-x test`, `continue-on-error`, `@Disabled` 검색 | 저장소 소스·CI에서 발견되지 않음 |

### 생성 산출물

- 실행 가능 Boot JAR: `build/libs/board-0.0.1-SNAPSHOT.jar` (67,648,985 bytes)
- plain JAR: `build/libs/board-0.0.1-SNAPSHOT-plain.jar`

`bootRun`은 `oauth,local` 프로필로 Spring Context와 Tomcat 초기화, Repository 검색까지 진행했지만 로컬 MySQL이 실행 중이지 않아 JPA `EntityManagerFactory` 생성 단계에서 종료됐다. 임의 DB 설정이나 비밀값은 만들지 않았다.

### 테스트 범위

- Entity 도메인 규칙: Member, Post, Comment 생성과 변경
- Service 단위 테스트: Member, Post, Comment의 성공·예외·권한·DTO 변환
- JPA Repository 테스트: Member, Post, Comment 조회·검색·집계·bulk update/delete
- MVC 슬라이스 테스트: 회원, 게시글, 댓글, 마이페이지, 관리자 요청 매핑과 모델/리다이렉트
- Security 통합 테스트: 익명·회원·관리자 접근, CSRF, 로그인 성공 처리
- OAuth2 테스트: Google/Naver/Kakao 변환, 신규·기존 소셜 회원, 잘못된 Naver 응답
- 예외·Validation 테스트: `CustomException`, View Advice, Bean Validation, 안전한 입력 보존

실제 Google/Naver/Kakao 서버, 실제 EC2, 실제 MySQL에는 연결하지 않았다.

## 9. 주요 코드 동작 원리

### Service 테스트

Mockito로 Repository 경계를 격리하되 호출 여부만 보지 않고 반환 DTO, 오류 코드, 연관 객체 변경을 함께 검증한다. 이번 복구에서는 메인 코드가 변경된 것이 아니라 리팩터링 이후 테스트 호출 이름이 오래된 상태였으므로 테스트를 현재 공개 계약에 맞췄다.

### Repository 테스트

`@DataJpaTest`와 H2 MySQL mode로 실제 JPQL을 실행한다. 회원별 게시글 쿼리는 목록 쿼리와 count query가 같은 회원·검색 조건을 사용한다. 조회수 원자적 증가는 엔티티를 읽어 `viewCount++` 하지 않고 기존 bulk update를 유지하며, 테스트는 영속성 컨텍스트를 초기화한 뒤 DB 값을 다시 읽는다.

### Controller 테스트

Security filter를 끈 MVC 슬라이스 테스트는 URI, HTTP method, principal 전달, 모델, view, redirect, Service 인자를 검증한다. 비즈니스 규칙은 Service 테스트와 중복 검증하지 않는다. `/my/posts`는 한 번 받은 회원별 `PageResultDto`에서 목록과 페이지를 함께 모델에 넣는지 확인한다.

### Security 테스트

별도 `SecurityConfigTest`는 filter chain을 활성화한다. 공개 GET, 인증이 필요한 게시글 POST와 `/my/**`, 관리자 역할이 필요한 `/admin/**`, CSRF가 필요한 쓰기 요청을 구분한다. 세션 기반 `UnifiedPrincipal`과 기존 OAuth2 login 설정은 유지했다.

### OAuth2 테스트

Provider별 응답 변환은 테스트 Map으로 검증한다. `CustomOauth2UserService` 테스트는 로컬 임시 HTTP user-info endpoint와 mock Repository를 사용하므로 실제 외부 OAuth 서버나 비밀값에 의존하지 않는다. Kakao 이메일 미제공 회귀 테스트는 provider ID 기반 대체 이메일을 고정 검증한다.

### CI 흐름

build job은 checkout → JDK 17 → Gradle 설정 → wrapper 실행 권한 → `clean test` → `bootJar` 순서다. 앞 단계가 실패하면 뒤 단계는 실행되지 않는다. deploy job은 `needs: build`이므로 build 성공 후에만 시작하며, EC2에서도 `clean build`를 수행한다.

## 10. 적용된 개념

- 계층형 관심사 분리: 회원별 검색 조건은 Repository, 페이지 DTO 변환과 인증 식별자 검증은 Service, 화면 모델 조립은 Controller에 배치했다.
- 회귀 테스트: 이름 변경, 회원별 페이지, 통계, 가입 입력 보존, Kakao 대체 이메일을 각각 재발 방지 대상으로 고정했다.
- JPA 페이징: 데이터 쿼리와 count query가 동일한 필터를 사용해 목록과 페이지 수의 불일치를 막는다.
- DTO 경계: View에는 Entity가 아니라 기존 `PostListResponse` 목록을 전달한다.
- Bulk update와 영속성 컨텍스트: 조회수 증가는 동시성에 유리한 원자적 UPDATE를 유지하고 Repository 테스트에서 clear 후 확인한다.
- Bean Validation과 예외 변환: 형식 오류는 `BindingResult`, 중복 같은 비즈니스 오류는 Service의 `CustomException`과 View Advice가 담당한다.
- 인증과 인가: `UnifiedPrincipal`로 회원 ID를 전달하고 Security filter chain과 `@PreAuthorize`가 접근 권한을 담당한다.
- CI fail-fast: `test` 성공을 `bootJar`와 deploy의 선행 조건으로 만들었다. `test`는 테스트 검증, `bootJar`는 실행 JAR 생성, `build`는 assemble과 check를 포함한다.

## 11. 기존 기능에 미치는 영향

- 회원: 가입 성공 흐름은 그대로다. 중복 오류 시 비밀번호를 제외한 입력 편의만 복구했다.
- 게시글: 생성·상세·수정·삭제와 전체 목록은 그대로다. 내 게시글만 회원·검색·페이지 조건을 일치시켰다.
- 댓글: 메인 동작 변경 없이 오래된 테스트 이름만 복구했다.
- 마이페이지: 회원별 게시글 목록과 통계 정확도가 개선됐다. 댓글·프로필·탈퇴 흐름은 유지했다.
- 관리자: 메인 동작 변경 없이 현재 Service 이름으로 MVC 테스트를 복구했다.
- 인증·인가: 세션, form login, CSRF, 역할 구조를 변경하지 않았다.
- OAuth2: provider 처리 구조는 유지하고 Kakao 이메일 미제공의 재현 가능한 문자열 버그만 수정했다.
- 배포: 기존 EC2 pull/build/restart 구조를 유지했다. 검증을 건너뛰지 않으므로 배포 시간이 늘 수 있지만 실패 코드 배포는 차단된다.

## 12. 남은 문제

- 로컬 MySQL이 실행 중이지 않아 실제 `local` 프로필 기동과 브라우저 스모크는 완료하지 못했다.
- GitHub Actions와 EC2 deploy는 로컬에서 실행할 수 없었으므로 원격 성공을 확인하지 않았다. Workflow의 단계·의존성과 YAML 들여쓰기는 로컬 검토했지만 실제 Actions 결과가 최종 확인 기준이다.
- Docker 이미지 빌드는 실행하지 않았다. Dockerfile 명령은 로컬에서 성공한 동일 `clean build`를 사용한다.
- `application-local.properties`와 `application-oauth.properties`에는 저장소에 이미 포함된 자격 증명 기본값이 있다. M0 기능 복구에서는 값을 변경하지 않았지만 외부화와 노출된 값의 회전이 필요하다. 보고서에는 실제 값을 재기록하지 않는다.
- 운영 MySQL과 H2 MySQL mode 사이의 모든 문법·실행계획 차이는 이번 로컬 테스트만으로 보장하지 않는다.
- 빈 페이지에서 `PageResultDto`의 페이지 번호 목록 표현 등 일반화된 pagination 품질 개선은 M0 범위를 넘긴다.
- EC2가 source를 다시 pull/build하는 현재 배포 구조는 CI에서 만든 동일 artifact를 배포한다는 보장이 약하다. 이번 작업에서는 구조를 재설계하지 않았다.

## 13. 후속 단계 이관

### M1: As-Is 문서화

- local/prod/oauth 설정과 필요한 환경변수 목록
- 기존 자격 증명 외부화 및 노출 값 회전 절차
- 세션 인증, OAuth2 provider별 회원 식별 정책
- EC2 pull/build/restart 운영 절차와 실패 복구 절차
- 현재 페이지·검색·예외 처리 계약

### M2: 게시판 REST API

- 현재 비어 있는 `PostApiController`의 API 설계와 구현
- JSON 오류 응답과 API 전용 예외 처리
- View DTO와 API DTO의 명확한 분리

### M3: 게시판 품질 개선

- 빈 페이지를 포함한 pagination 경계 조건 정리
- 메서드명과 통계 용어의 추가 정리
- OAuth2 이메일·닉네임 미제공 정책 결정
- 테스트 데이터 빌더와 Fixture 가독성 개선
- CI artifact를 deploy에 전달하는 구조 검토

### M4: 성능 검증

- 게시글·댓글 검색과 페이지 count query의 운영 데이터 성능
- 회원 탈퇴 bulk delete 순서와 대용량 영향
- 조회수 원자적 update의 동시성 및 lost update 방지 검증
- 회원별 조회수 집계 쿼리의 인덱스와 실행계획 검토
