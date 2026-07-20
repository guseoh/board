# PRE-M2 Security 경계 구현 보고서

## 1. 기준과 작업 범위

* 시작 브랜치: `recover`
* 시작 commit: `881e09c10e640c0d829d88b9b7b173f964d643ee`
* 시작 working tree: clean
* 비교 기준: 최신 `origin/master` `5fc0368ce2b9c402c4d444744e1d59de4c6f5b9a`
* 적용 지침: `C:\Users\guseo\.codex\AGENTS.md`
* 저장소 루트와 `src` 하위에는 별도 `AGENTS.md`가 없었다.
* commit, push, PR, merge, branch 생성, rebase, reset 등 Git 쓰기 작업은 수행하지 않았다.

이번 작업은 PRE-M2 Security 경계 보강을 목적으로 다음 범위를 구현했다.

* SEC-001 회귀 검증
* LOCAL 계정 전용 form login
* 게시글·댓글 소유권 검증 보강
* 댓글 URL과 실제 게시글 관계 검증
* 회원 탈퇴 확인 문구의 서버 검증
* Actuator 노출 제한
* `LocalTestController`의 profile 경계 검증
* 사용자·정책 오류의 Discord 비알림 처리
* 기본 `local` profile 자동 활성화 제거

Thymeleaf 화면, ViewController 구조, REST API, React, Validation 전반, 삭제 정책 전면 재설계는 이번 범위에 포함하지 않았다.

## 2. SEC-001 회귀 상태

SEC-001 SOCIAL 회원 비밀번호 변경 차단은 기존 구현을 유지하고 회귀 테스트로 검증했다.

`MemberService.updatePassword`는 회원 조회 직후 `LoginType.LOCAL` 여부를 검사한다.

SOCIAL 회원은 현재 비밀번호 비교보다 먼저 거부되므로 다음 동작이 발생하지 않는다.

* `PasswordEncoder.matches()` 호출
* `PasswordEncoder.encode()` 호출
* `Member.changePassword()` 호출
* 기존 password 변경

LOCAL 회원의 다음 계약은 그대로 유지된다.

* 정상 비밀번호 변경
* 현재 비밀번호 누락 거부
* 현재 비밀번호 불일치 거부
* 새 비밀번호 확인값 불일치 거부

SOCIAL 비밀번호 변경 정책 오류는 Discord로 전송되지 않는다.

## 3. 구현한 Security 경계

### 3.1. SEC-001B form login 계정 경계

#### 수정 전

`CustomUserDetailsService`는 email만으로 회원을 조회했다.

이 구조에서는 SOCIAL 회원도 `DaoAuthenticationProvider`의 password 검증 대상으로 전달될 수 있었다. SOCIAL 회원이 가진 dummy password가 외부에 알려지거나 예상하지 못한 값으로 변경된 경우 form login 경계가 약해질 수 있었다.

#### 변경 후

form login 회원 조회를 다음 조건으로 제한했다.

```text
email
+
LoginType.LOCAL
```

SOCIAL 회원과 존재하지 않는 email은 모두 같은 `UsernameNotFoundException`으로 처리된다. Spring Security의 인증 과정에서는 두 경우 모두 동일한 인증 실패로 변환되므로 계정 존재 여부나 로그인 유형이 외부에 노출되지 않는다.

LOCAL 회원은 기존과 동일하게 `UnifiedPrincipal`로 변환되고 password 검증을 거친다.

#### OAuth2 영향

`CustomOauth2UserService`와 provider별 사용자 정보 변환은 변경하지 않았다.

OAuth2 회귀 테스트에서 다음 동작을 확인했다.

* 신규 SOCIAL 회원 생성
* 기존 SOCIAL 회원 재사용
* provider와 provider ID 기반 회원 식별

form login 조회만 LOCAL 회원으로 제한했으므로 SOCIAL 회원은 기존 OAuth2 로그인 경로를 계속 사용한다.

### 3.2. 게시글 소유권

게시글 Service는 기존부터 수정과 삭제 전에 작성자 ID를 검증하고 있었다. 동일한 검증을 다시 구현하지 않고 테스트를 보강했다.

다음 계약을 테스트로 고정했다.

* 작성자만 게시글 수정 가능
* 작성자만 게시글 삭제 가능
* 비작성자는 수정·삭제 불가
* ADMIN 역할이라도 일반 사용자용 Service 흐름에서는 소유권 우회 불가
* 검증 실패 시 게시글 내용 불변
* 검증 실패 시 삭제 Repository 미호출

관리자 삭제는 일반 사용자용 Service에 role 우회를 추가하지 않고 별도의 관리자 경로로 유지한다.

### 3.2.1. 게시글 수정 화면 GET 경계

수정 전에는 `GET /post/{id}/edit`가 공개되어 익명·비작성자도 수정 form을 조회할 수 있었다. 이번 보강에서는 SecurityConfig에서 `GET /post/*/edit`만 authenticated로 제한하고, `PostService.getPostForEdit(postId, memberId)`가 게시글을 한 번 조회한 뒤 작성자 ID를 검증하도록 했다.

비작성자와 ADMIN 비작성자는 `NOT_POST_OWNER`, 게시글 부재는 `POST_NOT_FOUND`로 거부한다. Controller는 `UnifiedPrincipal.memberId`만 전달하며, 공개 `getPostDetail()`과 `GET /post/{id}` 및 기존 POST 수정 계약은 변경하지 않았다.

### 3.3. 댓글 소유권과 게시글 관계

#### 수정 전

댓글 Service에는 작성자 검증이 있었지만, 요청 URL의 `postId`와 댓글이 실제로 속한 게시글 ID를 비교하지 않았다.

따라서 다른 게시글의 URL과 댓글 ID를 조합한 요청이 작성자 검증만 통과할 가능성이 있었다.

#### 변경 후

댓글 수정과 삭제에서 댓글 조회 직후 다음 관계를 검증한다.

```text
요청 path의 postId
=
댓글이 실제로 속한 게시글 ID
```

다른 게시글 URL과 댓글 ID를 조합한 요청은 `COMMENT_NOT_FOUND`로 처리한다.

별도의 관계 오류를 반환하지 않고 현재 요청 경로에서는 해당 댓글을 찾을 수 없는 것처럼 처리함으로써 다른 게시글에 속한 댓글 ID의 존재 여부를 노출하지 않는다.

관계 또는 소유권 검증이 실패하면 다음 동작이 발생하지 않는다.

* 댓글 내용 변경
* 댓글 삭제
* 삭제 Repository 호출

ViewController는 요청 parameter의 memberId를 사용하지 않고, 인증 Principal의 memberId만 Service에 전달한다.

### 3.4. 회원 탈퇴 확인 문구

회원 탈퇴 확인 문구는 기존 View와 PRE-M2 문서에 명시된 다음 문자열을 사용한다.

```text
회원탈퇴
```

#### 검증 규칙

* 정확한 문자열 일치
* 자동 공백 제거 없음
* 대소문자 변환 없음
* 유사 문구 허용 없음
* 빈 문자열 거부
* null 값 거부

ViewController는 이미 존재하던 `confirmText` form parameter를 Service에 전달하는 최소 변경만 수행했다.

Service는 회원을 조회한 뒤 삭제 작업 전에 확인 문구를 검사한다. 문구가 일치하지 않으면 `WITHDRAW_CONFIRMATION_MISMATCH`를 발생시킨다.

검증 실패 시 다음 Repository 작업은 모두 실행되지 않는다.

* 회원이 작성한 댓글 삭제
* 회원 게시글에 달린 댓글 삭제
* 회원이 작성한 게시글 삭제
* 회원 삭제

검증 성공 시에는 기존 삭제 순서와 `@Transactional` 경계를 유지한다.

비밀번호 재인증, OAuth 재인증, soft delete는 이번 범위에 포함하지 않았다.

### 3.5. Actuator 접근 경계

Actuator 설정은 다음과 같이 제한했다.

* web exposure는 `health`만 허용
* health 상세 정보는 `never`
* `/actuator/health`만 익명 접근 허용
* 나머지 `/actuator/**`는 Security에서 차단

접근 결과는 다음과 같다.

| 요청                   | 익명 사용자          | 인증 사용자 |
| -------------------- | --------------- | ------ |
| `/actuator/health`   | 접근 가능           | 접근 가능  |
| `/actuator/metrics`  | 로그인 화면 redirect | 403    |
| `/actuator/mappings` | 로그인 화면 redirect | 403    |
| `/actuator/info`     | 로그인 화면 redirect | 403    |
| 기타 `/actuator/**`    | 로그인 화면 redirect | 403    |

익명 요청이 302 redirect가 되는 것은 form login entry point의 동작이다. 핵심 계약은 Actuator 내부 정보가 응답에 노출되지 않는 것이다.

일반 애플리케이션 URL의 인증 정책은 변경하지 않았다.

### 3.6. LocalTestController profile 경계

`LocalTestController`는 기존 `@Profile("local")`을 유지한다.

테스트에서 다음 경계를 확인했다.

* `local` profile에서는 Bean 등록
* `prod` profile에서는 Bean 미등록
* 일반 non-local test profile에서는 Bean 미등록
* 신규 운영 테스트 endpoint 추가 없음
* 예외 처리 테스트용 endpoint는 테스트 소스의 내부 Controller 사용

#### 기본 local profile 자동 활성화 제거

공통 `application.properties`에 있던 다음 설정을 제거했다.

```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
```

이 설정은 환경 변수 `SPRING_PROFILES_ACTIVE`가 없을 때 `local` profile을 자동 활성화했다. 운영 환경에서 profile 설정이 누락되면 `LocalTestController`가 등록될 수 있으므로 fail-open 위험이 있었다.

변경 후 공통 설정은 기본 profile을 강제로 지정하지 않는다.

각 환경은 실행 시 active profile을 명시해야 한다.

로컬 개발 예시:

```text
SPRING_PROFILES_ACTIVE=local,oauth
```

운영 환경 예시:

```text
SPRING_PROFILES_ACTIVE=prod,oauth
```

테스트는 각 테스트 클래스 또는 테스트 설정에서 `test` 등 필요한 profile을 명시한다.

이 변경으로 profile을 지정하지 않은 실행 환경에서는 `local` profile과 `LocalTestController`가 활성화되지 않는다.

### 3.7. Discord 알림 분류

현재 `CustomException`의 ErrorCode는 다음과 같은 사용자·도메인 오류를 표현한다.

* 입력 검증 실패
* 인증 실패
* 권한 및 소유권 위반
* 조회 대상 없음
* 제품 정책 위반

현재 목록에는 실제 운영 장애를 나타내는 ErrorCode가 없다.

따라서 `GlobalViewControllerAdvice`에 빈 명시적 allow-list를 적용해 현재 `CustomException`은 Discord로 전송하지 않도록 변경했다.

현재 알림 대상으로 유지한 ErrorCode는 없다.

`POST_NOT_FOUND`는 공개 URL의 정상적인 부재나 잘못된 사용자 요청으로도 발생할 수 있으므로 운영 장애 알림에서 제외했다.

Discord payload는 기존과 동일하게 다음 정적 정보만 사용한다.

* ErrorCode 이름
* ErrorCode의 정적 메시지
* redirect URL

다음 민감정보는 포함하지 않는다.

* 요청 본문
* password
* OAuth attributes
* Principal 전체 정보
* 세션 정보

일반 `Exception` handler나 광범위한 500 오류 알림은 추가하지 않았다.

기존 `/test/discord-error`는 local profile의 테스트 route로 유지된다. 현재 이 endpoint가 발생시키는 `POST_NOT_FOUND`는 비알림 대상이므로 Discord 전송을 유발하지 않는다.

운영 장애 알림은 배포·관측성 단계에서 별도 정책으로 설계한다.

## 4. 변경 파일과 이유

| 파일                                 | 변경 이유                                                        |
| ---------------------------------- | ------------------------------------------------------------ |
| `MemberRepository`                 | form login 회원 조회를 email과 LOCAL 유형으로 제한                       |
| `CustomUserDetailsService`         | SOCIAL 회원과 미존재 계정을 동일한 인증 실패로 처리                             |
| `MemberService`                    | 회원 탈퇴 확인 문구를 Service에서 최종 검증                                 |
| `MyPageViewController`             | 확인 문구를 Service로 전달하기 위한 최소 연결                                |
| `CommentService`                   | 댓글 ID와 path postId의 관계 검증                                    |
| `ErrorCode`                        | 탈퇴 확인 실패의 안전한 MVC 오류 계약 추가                                   |
| `SecurityConfig`                   | health만 익명 공개하고 나머지 Actuator 경로 차단                           |
| `application.properties`           | Actuator exposure 축소, health details 제한, 기본 local profile 제거 |
| `GlobalViewControllerAdvice`       | 현재 CustomException에 빈 Discord allow-list 적용                  |
| `PostService`, `PostViewController`, 관련 테스트 | 수정 화면 GET의 인증·소유권 경계와 공개 상세 회귀 고정 |
| Service·Security·Exception·MVC 테스트 | 인증, 소유권, 탈퇴, Actuator, profile, Discord 회귀 고정                |
| `PRE-M2-IMPLEMENTATION-REPORT.md`  | 구현 의도, 영향과 검증 결과 기록                                          |

## 5. 검증

### 구현 과정

`gradlew.bat compileTestJava`를 실행한 초기 red 단계에서는 신규 테스트가 요구하는 계약이 구현되지 않아 9개의 compile error를 확인했다.

구현 후 compile과 대상 테스트를 순차적으로 통과시켰다.

### 대상 테스트

다음 테스트를 실행했다.

* `MemberServiceTest`
* `CustomUserDetailsServiceTest`
* `PostServiceTest`
* `CommentServiceTest`
* `SecurityConfigTest`
* OAuth2 관련 테스트
* `ExceptionAndValidationTest`
* `ControllerMvcTest`
* profile 경계 테스트

Actuator 테스트는 실제 Spring Security form login 동작에 맞게 다음 계약으로 고정했다.

* 익명 요청: login entry point에 의한 redirect
* 인증 요청: 403
* endpoint 내용: 노출되지 않음

### 전체 검증

Codex 구현 완료 시점의 검증 결과:

| 명령                        | 결과                                        |
| ------------------------- | ----------------------------------------- |
| `gradlew.bat clean test`  | 70 tests, failures 0, errors 0, skipped 0 |
| `gradlew.bat clean build` | 66 tests, failures 0, errors 0, skipped 0 |
| Boot JAR 생성               | 성공                                        |
| `git diff --check`        | 통과                                        |

전체 테스트 실행 셸은 120초 도구 대기 제한을 초과했지만, Gradle 자식 프로세스가 정상 종료된 뒤 XML 테스트 결과와 JAR 생성 상태를 확인했다. 이는 테스트 실패가 아니라 실행 도구의 대기 제한이었다.

기본 `local` profile 제거 후 사용자가 다시 실행한 최종 검증 결과:

| 명령                         | 결과                 |
| -------------------------- | ------------------ |
| `.\gradlew.bat clean test` | `BUILD SUCCESSFUL` |
| `git diff --check`         | 공백 오류 없이 통과        |

`git diff --check`에서는 일부 Java 파일에 대해 다음 줄바꿈 경고가 출력됐다.

이번 게시글 수정 GET 변경 후 대상 검증 결과:

| 명령 | 결과 |
| --- | --- |
| `gradlew.bat compileTestJava` | BUILD SUCCESSFUL |
| `gradlew.bat test --tests project.board.post.service.PostServiceTest --tests project.board.global.security.SecurityConfigTest --tests project.board.controller.ControllerMvcTest` | BUILD SUCCESSFUL |
| `gradlew.bat clean test` | 테스트 XML 14개, 70 tests, failures 0, errors 0, skipped 0 확인 |
| `git diff --check` | 통과 |

```text
LF will be replaced by CRLF the next time Git touches it
```

이는 Windows의 Git 줄바꿈 설정에 따른 경고이며 trailing whitespace나 patch 오류는 아니다. 이번 Security 변경에서는 줄바꿈 정책을 일괄 수정하지 않았다.

기본 `local` profile 제거 후 `clean build`는 별도로 다시 실행하였다. profile 제거 전 전체 build와 Boot JAR 생성은 성공했고, 최종 변경 후에는 `clean test`로 회귀를 검증했다.

### 실행하지 않은 검증

다음 검증은 수행하지 않았다.

* 실제 외부 OAuth provider 로그인
* 실제 Discord Webhook 전송
* 운영 서버 배포
* 운영 profile 기반 실행
* 운영 데이터의 기존 SOCIAL password 상태 점검

OAuth2 provider 변환은 로컬 HTTP user-info 테스트로 회귀를 확인했다.

Discord는 mock을 이용해 호출 여부와 payload 경계를 확인했다.

## 6. MVC와 OAuth2 영향

MVC 변경은 회원 탈퇴 POST에서 이미 존재하던 `confirmText` form parameter를 Service로 전달하는 연결뿐이다.

다음 항목은 변경하지 않았다.

* redirect 구조
* flash message
* Thymeleaf template
* password View model
* ViewController 패키지 구조

OAuth2 영역에서는 다음을 변경하지 않았다.

* provider별 사용자 정보 parsing
* OAuth 회원 생성
* 기존 OAuth 회원 재사용
* provider와 provider ID 식별
* OAuth2 로그인 성공 흐름

form login 회원 조회만 LOCAL 유형으로 좁혔으므로 SOCIAL 회원은 OAuth2 경로를 계속 사용한다.

## 7. 남은 위험과 후속 결정

### 7.1. Discord 운영 장애 알림

현재 `CustomException`의 Discord allow-list는 비어 있다.

이는 현재 ErrorCode가 사용자·도메인 오류만 표현하기 때문이다.

향후 운영 장애 알림이 필요해지면 배포·관측성 단계에서 다음 항목을 별도로 결정해야 한다.

* 운영 장애용 ErrorCode
* 장애 등급
* 전송 채널
* 중복 알림 억제
* 민감정보 제거
* Discord 전송 실패 격리
* application log와 모니터링 시스템의 역할 분담

이번 PRE-M2 Security 완료를 막는 항목은 아니다.

### 7.2. 기존 SOCIAL 회원 데이터

SEC-001 적용 전에 SOCIAL 회원의 password가 외부에 알려졌거나 예상하지 못한 값으로 변경됐는지에 대한 운영 데이터 점검은 수행하지 않았다.

신규 form login 경계에서는 SOCIAL 회원을 조회하지 않으므로 현재 코드 수준의 form login 진입은 차단된다.

실제 운영 데이터 점검은 배포 전 별도 운영 작업으로 수행할 수 있다.

### 7.3. 회원 탈퇴 정책

다음 항목은 이번 범위에서 제외했다.

* 비밀번호 재인증
* OAuth 재인증
* soft delete
* 복구 유예 기간
* 삭제 무결성 전면 재설계

회원 탈퇴 확인 문구 `회원탈퇴`는 기존 코드와 문서에서 명확하게 확인됐으므로 이번 구현에 추가 제품 결정은 필요하지 않았다.

### 7.4. 줄바꿈 정책

일부 파일에서 LF와 Windows CRLF 변환 경고가 발생한다.

현재 Security 변경에는 영향을 주지 않으므로 수정하지 않았다.

필요한 경우 `.gitattributes`를 이용한 저장소 줄바꿈 통일을 별도 작업으로 진행한다.

## 8. 학습 메모

### URL 보안과 Service 보안

Spring Security의 URL rule은 HTTP 요청의 첫 진입을 통제한다.

Service 검증은 Controller를 우회한 호출, 다른 Controller의 재사용, 향후 REST API 호출까지 포함해 최종 비즈니스 경계를 보장한다.

따라서 인증·소유권·탈퇴 정책은 Service에서도 반드시 강제해야 한다.

### DaoAuthenticationProvider와 사용자 조회

`DaoAuthenticationProvider`는 존재하지 않는 사용자 인증에서 timing attack을 완화하기 위해 synthetic password 검증을 수행할 수 있다.

이는 SOCIAL 회원의 실제 dummy hash를 Repository에서 조회해 검증 대상으로 전달하는 것과는 다르다.

`CustomUserDetailsService`에서 LOCAL 회원만 조회하도록 제한하면 form login 인증 대상 자체를 명확히 구분할 수 있다.

### 소유권 검증

소유권은 role 표시가 아니라 다음 값을 비교해 검증한다.

```text
Entity의 작성자 ID
=
인증 Principal의 회원 ID
```

ADMIN 여부가 일반 사용자용 수정·삭제 흐름의 소유권을 자동 우회해서는 안 된다.

관리자 권한은 별도의 관리자 Service와 endpoint에서 명시적으로 처리하는 것이 안전하다.

### 상위 리소스 관계 검증

댓글 수정·삭제에서는 댓글 작성자뿐 아니라 요청 URL의 게시글과 댓글이 실제로 속한 게시글의 관계도 검증해야 한다.

```text
URL의 postId
=
Comment가 속한 Post의 ID
```

이 검증이 없으면 다른 게시글 URL과 댓글 ID를 조합한 요청이 통과할 수 있다.

### 트랜잭션과 탈퇴 검증

트랜잭션 안에서 삭제보다 먼저 정책 검증을 끝내면 거부 경로에서 delete query가 실행되지 않는다.

성공 경로에서는 기존 삭제 순서를 하나의 트랜잭션 안에서 유지할 수 있다.

### Actuator 방어

Actuator는 다음 두 경계를 함께 제한해야 한다.

* Spring Boot의 endpoint exposure
* Spring Security의 URL 접근 정책

exposure에서 제외하면 endpoint 자체가 외부에 노출되지 않는다.

Security rule은 노출된 endpoint에 누가 접근할 수 있는지를 제어한다.

두 설정을 함께 적용해야 의도하지 않은 내부 정보 노출을 줄일 수 있다.

### Profile 기본값

공통 설정에서 `local`을 기본값으로 활성화하면 실행 환경의 profile 설정 누락이 테스트용 Bean 활성화로 이어질 수 있다.

운영과 로컬 환경은 active profile을 외부에서 명시하고, 공통 설정은 안전한 기본 상태를 유지하는 편이 좋다.

## 9. 최종 결과

PRE-M2 Security 경계 구현 결과는 다음과 같다.

* SEC-001 SOCIAL 비밀번호 변경 차단 유지
* SOCIAL 회원 form login 차단
* LOCAL 회원 form login 유지
* OAuth2 로그인 회귀 없음
* 게시글 작성자 소유권 회귀 테스트 보강
* 댓글 URL과 실제 게시글 관계 검증 추가
* 회원 탈퇴 확인 문구 Service 검증
* Actuator health 외 노출 차단
* LocalTestController profile 경계 검증
* 기본 local profile 자동 활성화 제거
* 현재 CustomException의 Discord 전송 차단
* 게시글 수정 GET 인증·소유권 경계 추가
* 70개 테스트 통과
* Boot JAR 생성 확인
* 최종 profile 변경 후 `clean test` 성공
* `git diff --check` 통과
* commit `0b634ef` (`fix: 게시글 수정 화면 접근 경계 보강`) 생성 완료
* `git push origin recover` 성공, 원격 `recover`가 `0b634ef30e2d7778b8874caa1535db700c31736c`를 가리킴
