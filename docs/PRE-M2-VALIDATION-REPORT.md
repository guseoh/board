# PRE-M2 입력 Validation 계약 보고서

## 작업 기준

- branch: `recover`
- 시작 commit: `91d795f206694e26ee8350b6ea030517070e385e`
- 시작 working tree: clean
- 비교 기준: `origin/master` `5fc0368ce2b9c402c4d444744e1d59de4c6f5b9a`
- 적용 지침: `C:\Users\guseo\.codex\AGENTS.md` (저장소 하위 별도 지침 없음)
- `origin/master...recover`와 열린 PR을 확인했으며, 열린 PR은 없었다.

범위는 SSR 요청 경계의 Bean Validation, 게시글·댓글 도메인 길이 불변식, 민감 비밀번호 form 정리, Pagination 범위, 관리자 Role enum 바인딩이다. Thymeleaf 디자인, REST API, DB migration·column 길이, 비밀번호 정책, 검색·페이지 계산, Role 체계 확장은 제외했다.

## Validation 계약

| 입력 | 수정 전 | 변경 후 요청 경계 | Service/Entity 책임 | MVC 실패 |
| --- | --- | --- | --- | --- |
| 회원가입 email | `@Email`만 적용 | null, 빈값, 공백, 형식 오류 거부 | 중복 email | signup 화면, password 제거 |
| nickname 수정 | null 누락 허용 가능 | 필수, 2~12자, 영문·숫자·한글만 | 중복 및 기존 nickname 정책 | myEdit 화면 |
| 비밀번호 | 기존 8~20자 영문·숫자 정책 | current/new/confirm 필수 및 형식 유지 | 현재 password, 확인값, SOCIAL 정책 | myEdit 화면, 모든 password 제거 |
| 게시글 | blank만 DTO/Entity에서 검증 | title/content 1~500자 | Entity가 blank·501자 이상 거부 | form 오류, Service 미호출 |
| 댓글 | blank만 DTO 검증 | content 1~500자 | Entity가 blank·501자 이상 거부 | redirect + 오류 flash, Service 미호출 |
| page/size | 범위 없음 | page 1 이상, size 1~100, 기본 1/5 | Service는 정상 Pageable만 사용 | 400, Service/Repository 미호출 |
| 관리자 Role | String을 Service에서 `valueOf` | `Role` enum으로 바인딩, USER/ADMIN만 허용 | Entity의 `changeRole(Role)` | 변환 실패 400, USER는 403 |

email 정규화·trim은 추가하지 않았다. nickname의 중복과 같은 값 처리, 비밀번호 확인·SOCIAL 차단, 소유권은 기존 Service 정책을 유지한다.

## 민감정보 보호

회원가입 BindingResult 실패 시 `MemberCreateRequest.clearPasswords()`로 `password`, `passwordConfirm`을 비운다. 비밀번호 변경 실패 시 `MemberPasswordUpdateRequest.clearPasswords()`로 `currentPassword`, `newPassword`, `newPasswordConfirm`을 비운다. BindingResult가 참조하는 원본 객체도 함께 비워 model 재노출을 방지한다.

signup에 다시 남기는 값은 nickname과 email뿐이며, myEdit에는 안전한 profile과 nickname form만 재구성한다. 관련 Controller 로그와 flash attribute에 비밀번호 값은 추가하지 않았다.

## 변경 파일과 이유

- Member DTO 3개: email/nickname 필수성 및 password clear 메서드
- PostRequest, CommentCreateRequest, PageRequestDto: 길이·수치 Bean Validation
- Post, Comment, ErrorCode: Controller 우회 시에도 500자 불변식과 안전한 오류
- MemberService, AdminViewController: Role enum 요청 경계
- MemberViewController, MyPageViewController, PostViewController: Validation 연결과 민감 form 정리
- DTO/Entity/Service/MVC/Security 테스트: 입력 경계, 불변식, 미호출, Role 403 회귀

## 검증

| 명령 | 결과 |
| --- | --- |
| `gradlew.bat compileTestJava` | BUILD SUCCESSFUL |
| Validator·Member·Post·Comment·MVC·Security 대상 테스트 | 55 tests, failures 0, errors 0, skipped 0 |
| `gradlew.bat clean test` | 78 tests, failures 0, errors 0, skipped 0 |
| `gradlew.bat clean build` | 78 tests, failures 0, errors 0, skipped 0; Boot JAR 생성 확인 |
| `git diff --check` | commit 직전 실행 |

Gradle 실행 셸은 도구의 120초 대기 제한을 넘겼지만, test worker 종료 뒤 XML 결과와 JAR 생성으로 실제 성공을 확인했다.

실제 외부 OAuth provider, Discord webhook, 운영 배포·DB migration은 이번 범위에서 실행하지 않는다.

## 영향과 남은 위험

기존 Security 경계와 공개 조회 URL은 변경하지 않는다. Entity와 DB column의 500자 계약을 같은 값으로 맞췄지만, DB migration은 추가하지 않았으므로 운영 schema가 Entity 정의와 다르면 별도 점검이 필요하다.

page/size의 최대 100은 과도한 SSR 조회를 줄이는 입력 경계이며, 범위 초과 양수 page의 UX와 REST API의 오류 응답 형식은 다음 단계에서 결정한다. enum의 소문자·미지원 값은 Spring MVC 변환 실패로 400이 되며, Permission 확장은 제외한다.

## 학습 메모

Bean Validation은 HTTP 요청의 null·형식·범위를 일찍 거부하지만, Controller를 우회한 Service 호출까지 막지 못한다. 따라서 저장 제약과 같은 핵심 길이는 Entity 불변식도 필요하다.

BindingResult는 원본 form 객체를 model에 유지하므로 비밀번호 실패 화면에서 객체 자체를 비워야 한다. enum은 Controller에서 바인딩하면 `valueOf` 예외가 Service까지 내려가지 않고 허용값 계약이 URL 경계에 드러난다. Pagination도 `PageRequest` 생성 전에 범위를 검증해야 런타임 예외나 과도한 조회를 방지할 수 있다.

## Git 결과

- commit 메시지: `fix: PRE-M2 입력값 검증 계약 강화`
- push 대상: `origin/recover`
- 최종 SHA는 자기 참조를 피하기 위해 이 문서에 기록하지 않는다.
