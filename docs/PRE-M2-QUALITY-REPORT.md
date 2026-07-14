# PRE-M2 품질 기준선 정리 보고서

> 작업 브랜치: `refactor/pre-m2-quality-baseline`  
> 시작 기준: `master` / `d06ee0a71e2f35315c223b5e6975430387f51bed` (PR #98 merge commit)  
> 작업일: 2026-07-14 (Asia/Seoul)

## 확인한 기술 부채

- Validation: 가입 email·닉네임 수정의 필수값, Post/Comment 500자, page/size 범위, 탈퇴 확인과 Role 허용값 계약이 불완전했다.
- Security: SOCIAL 비밀번호 변경의 Service 강제, 게시글 수정 GET 인증·작성자 검증, Actuator 공개 범위가 부족했다.
- JPA: 자기참조 댓글의 답글·부모 삭제 순서와 회원 탈퇴 삭제 순서가 한 bulk query에 숨겨져 있었고 bulk delete 후 영속성 컨텍스트가 남았다.
- Pagination: 전체 제목 검색이 목록 page 흐름과 분리됐고 빈 결과, page 블록과 실제 범위 초과 요청 계약이 명시되지 않았다.

## 실제 변경 범위와 파일별 이유

| 파일·영역 | 변경 이유 |
| --- | --- |
| `member/dto/request/*`, `PostRequest`, `CommentCreateRequest`, `PageRequestDto` | 필수값, 500자와 page/size 입력 경계를 HTTP 입력 계약에 반영 |
| `MemberService`, `ErrorCode`, `MyPageViewController` | 탈퇴 확인, SOCIAL 비밀번호 차단, 민감한 비밀번호 재표시 방지 |
| `AdminViewController` | 임의 문자열 대신 `Role` enum만 바인딩 |
| `SecurityConfig`, `application.properties` | 수정 GET 인증, Actuator health/ADMIN 분리와 prod 노출 축소 |
| `PostViewController`, `PostService`, `PostRepository`, `post/list.html` | 제목 검색을 기존 page 흐름에 통합하고 keyword를 페이지 이동에 유지 |
| `CommentRepository`, `CommentService`, `PostService`, `MemberService` | 답글 우선 명시 삭제와 필요한 bulk query의 자동 clear 적용 |
| `PageResultDto` | 결과가 없을 때 빈 페이지 블록을 명시 |
| 관련 Controller·Service·Repository·pagination 테스트 | 입력·권한·삭제 순서·영속성 컨텍스트·검색 page 경계 회귀 방지 |
| `M1-AS-IS-REPORT.md` | PR #98, master build와 의도적으로 중지된 EC2 배포 상태로 M1 완료 근거 갱신 |
| `REQUIREMENTS.md`, `API.md`, `SECURITY.md`, `TESTING-AND-OPERATIONS.md` | 변경 후 SSR 계약, 보안 규칙, 테스트·운영 기준 반영 |

## 계약 변화

### Validation

- 회원가입 email과 닉네임 수정은 null·blank를 거부한다.
- 게시글 title/content와 댓글 content는 Entity column과 같은 최대 500자다.
- page는 1 이상, size는 1~100이며 기존 기본값 page 1/size 5는 유지한다.
- Bean Validation은 필수값·형식·길이·수치 범위를 담당한다. 탈퇴 확인 문구, SOCIAL 비밀번호 제한, 중복과 소유권은 Service 비즈니스 규칙이다.
- 비밀번호 form Validation 실패 시 request의 current/new/confirm을 null로 비운 뒤 View를 반환한다. 가입 정책 오류도 nickname/email만 보존한다.

### Security

- SOCIAL 회원은 화면 우회 호출도 Service에서 차단된다.
- `GET /post/{id}/edit`는 인증이 필요하고 Service가 SecurityContext principal의 member ID로 작성자를 검증한다. POST 수정·삭제도 같은 principal만 사용한다.
- `/test/discord-error`는 기존처럼 `local` profile에서만 Bean으로 등록된다.
- Actuator health는 익명 운영 확인용으로 유지한다. 그 외 Actuator는 ADMIN이며, prod는 `health,info`만 노출하고 health detail을 숨긴다.
- LOCAL/SOCIAL 동일 이메일 연결, 로그인 후 redirect parameter, provider별 계정 통합은 제품 결정 항목으로 남겼다.

### JPA 삭제와 트랜잭션

명시적 삭제 정책과 Service 트랜잭션을 유지하며 cascade/orphanRemoval은 추가하지 않았다.

```text
단일 댓글: 해당 부모의 답글 → 요청 댓글
게시글: 답글 → 부모 댓글 → 게시글
회원:
  회원 게시글의 답글 → 부모 댓글
  회원 부모 댓글에 달린 답글 → 회원의 답글 → 회원의 부모 댓글
  회원 게시글 → 회원
```

대댓글은 현재 한 단계만 허용되므로 위 순서가 자기참조 FK를 만족한다. 댓글과 회원 게시글 bulk delete에는 `clearAutomatically=true`를 적용해 실행 직후 관리 Entity와 DB 상태가 어긋나지 않게 했다. 조회수 bulk update는 기존 자동 flush/clear를 유지했고, 추가 flush는 넣지 않았다. Repository 테스트의 `flushAndClear`는 삭제 전 fixture를 DB에 확정하는 경계에서만 사용하고 bulk delete 뒤에는 자동 clear 자체를 검증한다.

### Pagination·검색

- `/posts/search`를 별도 List 조회에서 `PageRequestDto`와 `PageResultDto`를 사용하는 목록 흐름으로 합쳤다.
- keyword는 query와 page 링크에서 유지된다. blank keyword는 전체 목록으로 정규화한다.
- 검색 결과가 없으면 posts/pageList가 비어 있고 start/end는 0이다.
- 첫·마지막 page, 10개 미만 page 블록과 다음·이전 블록을 테스트했다.
- 실제 total page보다 큰 양수 page는 검색 조건의 마지막 page로 다시 조회한다.

## 실행한 테스트와 실제 결과

| 명령 | 결과 |
| --- | --- |
| 변경 전 관련 9개 테스트 클래스 | 47개 성공, 실패 0, skip 0 |
| Validation·Security 관련 5개 클래스 | 최초 40개 중 39 성공·1 실패. 기존 password 오류 분기의 잘못된 `nicknameRequest` 모델 타입을 수정 |
| `ControllerMvcTest` 재실행 | 7개 성공, 실패 0, skip 0 |
| JPA·Pagination 관련 7개 클래스 | 44개 성공, 실패 0, skip 0 |
| 검색 Repository 보강 후 `PostRepositoryTest` | 3개 성공, 실패 0, skip 0 |
| 최종 `.\gradlew.bat clean build` | 성공(5분 3초), 69개 실행, 실패 0, error 0, skip 0; Boot JAR 생성 |
| 최종 `git diff --check` | 성공, whitespace 오류 없음 |

로컬 MySQL, Docker와 `bootRun`은 사용자 승인에 따라 실행하지 않았다. EC2를 시작하거나 Deploy Job을 재실행하지 않았고, 실제 OAuth2 login과 Discord Webhook도 검증하지 않았다.

## 제외·보류 항목

- DB migration, 운영 데이터 변경, Flyway, nickname 및 `(provider, providerId)` unique 제약
- cascade/orphanRemoval, Entity 관계 전면 재설계, soft delete
- Querydsl, cursor pagination, 무한 스크롤, 검색 인덱스, N+1·쿼리 수·대용량 성능 검증
- LOCAL/SOCIAL 동일 이메일 연결, redirect parameter와 OAuth provider별 통합 정책
- REST API, React와 풋살 도메인
- EC2 배포·기동·Health Check는 인스턴스를 다시 운영할 때 수행할 별도 운영 작업

## M2 REST 계약 설계 전 남은 항목

- SSR에서 확정한 Validation 오류를 REST JSON 오류 코드·필드 오류 구조로 변환하는 계약
- 인증 방식, CSRF/JWT 선택과 REST 소유권 오류의 HTTP status 계약
- LOCAL/SOCIAL 계정 연결과 OAuth provider 통합 제품 정책
- DB unique/migration 도입 시점과 운영 데이터 정합성 계획
- 내 게시글·내 댓글의 실제 범위 초과 page 정규화 여부

## Commit·push·PR 결과

- `3f824d8` `docs: M1 마일스톤 완료 결과 반영`
- `dfc3045` `fix: 입력 검증과 권한 계약 강화`
- `ca4d7ef` `refactor: JPA 삭제 무결성과 페이징 계약 정리`
- 이 보고서와 계약 문서: `docs: M2 전 품질 개선 결과 문서화`
- `origin/refactor/pre-m2-quality-baseline` 신규 push와 upstream 설정 성공
- Draft PR #99 `[PRE-M2] M1 완료 기록 및 핵심 품질 부채 정리` 생성 성공
  - base: `master`
  - head: `refactor/pre-m2-quality-baseline`
  - URL: https://github.com/guseoh/board/pull/99
- 이 보고서를 포함하는 마지막 문서 commit은 같은 원격 브랜치에 추가 push되어 PR #99에 포함된다.
