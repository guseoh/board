# PRE-M2 품질 부채 분석 및 개선 방향

> 분석 기준: `master` / `d06ee0a71e2f35315c223b5e6975430387f51bed`
> 문서화 브랜치: `docs/pre-m2-quality-direction`
> 작성일: 2026-07-14 (Asia/Seoul)

## 작업 원칙과 범위

Project-Board의 분석, 리뷰, 설계와 품질 부채 정리 작업에서는 사용자가 작성한 구현 코드를 직접 수정하지 않는다. 코드 변경이 필요하면 문제, 영향, 개선 방향, 예상 변경 파일과 검증 방법을 이 문서처럼 제안하고, 실제 Java·테스트·Thymeleaf·설정 변경은 사용자의 별도 구현 요청과 승인을 받은 작업으로 분리한다.

이 문서는 최신 `master`에서 시작한 문서 전용 작업으로, M2 REST API 설계 전에 확인된 Validation, Security, JPA와 Pagination 부채를 분석한다. 완성된 구현 코드나 적용 결과를 포함하지 않으며, 최종 변경 범위는 이 보고서와 M1 완료 보고서뿐이다. Java·테스트·View·설정 상태는 `master`와 동일하다.

## Validation

### 입력 경계와 허용값

| 품질 부채 | 현재 문제 | 영향 | 권장 방향 | 예상 변경 위치 | 검증 방법 | 보류 조건 |
| --- | --- | --- | --- | --- | --- | --- |
| 회원가입 email | `@Email`만으로는 null·blank 입력을 필수값으로 명확히 거부하지 못한다. | DB nullable/unique 제약까지 잘못된 입력이 도달하거나 화면 오류 계약이 불명확해진다. | email 형식 검증과 별도로 필수값 검증을 선언한다. | `MemberCreateRequest`, 회원가입 Controller Validation 테스트 | null, 빈 문자열, 공백, 정상 email을 Bean Validation과 MVC 테스트로 구분한다. | email 선택 가입 같은 제품 정책을 도입할 경우 필수값 여부를 먼저 결정한다. |
| nickname 수정 | `@Pattern`은 null을 허용하므로 누락 요청이 변경 없음으로 성공할 수 있다. | 사용자는 성공 응답을 받지만 실제 변경이 없고 Controller·Service 책임이 흐려진다. | 필수값 검증 후 현재 형식 규칙과 Service 중복 검사를 유지한다. | `MemberNicknameUpdateRequest`, `MyPageViewController`, nickname Validation 테스트 | null·blank·공백·길이·문자 조합과 기존 nickname 유지 요청을 구분한다. | nickname 미변경을 허용하는 복합 profile PATCH 계약을 도입할 경우 별도 DTO가 필요하다. |
| 게시글 길이 | title과 content DTO에는 Entity column의 500자 제한이 없다. | DB 저장 단계에서 실패해 사용자 입력 오류가 서버 오류처럼 보일 수 있다. | title/content에 최대 500자 계약을 두고 Entity/DB 길이와 한 곳에서 교차 확인한다. | `PostRequest`, `Post`, 게시글 작성·수정 MVC 테스트 | 500자 성공, 501자 실패, blank와 500자 경계 조합을 확인한다. | DB column 길이 변경이나 본문 대용량 타입 전환이 결정되면 DTO와 migration을 함께 설계한다. |
| 댓글 길이 | comment content DTO에는 Entity column의 500자 제한이 없다. | 게시글과 동일하게 저장 시점 오류와 사용자 오류가 혼재한다. | 댓글 작성·수정에 공통 최대 500자 계약을 적용한다. | `CommentCreateRequest`, `Comment`, 댓글 Controller 테스트 | 500/501자, blank, 답글 작성과 수정 경계를 검증한다. | 댓글 저장 타입이나 허용 길이 제품 정책이 바뀌면 DB 변경 계획과 함께 보류한다. |
| page·size | `PageRequestDto`에 page 최소값과 size 범위가 없다. | 0·음수 page 또는 과도한 size가 `PageRequest` 예외나 부하로 이어질 수 있다. | 기존 page 1/size 5 기본값을 유지하고 page≥1, size의 명시적 상·하한을 정한다. | `PageRequestDto`, 이를 받는 네 View Controller, pagination Validation 테스트 | 누락 기본값, page 0/음수, size 0/상한 초과, 정상 경계를 검증한다. | 허용 최대 size는 UI·운영 부하 기준이 필요하므로 수치 확정 전에는 구현하지 않는다. |
| 회원 탈퇴 확인 | `confirmText`를 화면 JavaScript에서만 확인하고 서버가 받지 않는다. | JavaScript 우회나 직접 POST로 의도하지 않은 탈퇴 요청이 가능하다. | Controller가 확인 문구를 받고 Service가 정확한 비즈니스 문구를 검증한 뒤 삭제를 시작한다. | `MyPageViewController`, 탈퇴 요청 DTO 또는 인자, `MemberService`, 탈퇴 View·Service 테스트 | 누락·공백·오타에서는 삭제 Repository가 호출되지 않고 정확한 문구에서만 삭제 흐름이 시작되는지 확인한다. | 확인 문구, 비밀번호 재확인, 재인증 중 어떤 정책을 사용할지 제품 결정이 바뀌면 보류한다. |
| 관리자 Role | 문자열을 Service에서 `Role.valueOf`로 변환한다. | 임의 문자열이 처리되지 않은 변환 예외가 되고 허용값 계약이 Controller 밖에 숨는다. | 요청 경계에서 `Role` enum 또는 enum 필드를 가진 입력 객체로 제한한다. | `AdminViewController`, 관리자 role 요청 계약, `MemberService`, 관리자 MVC 테스트 | USER/ADMIN 성공, 대소문자·미지원 문자열·누락 400, 권한 없는 사용자 403을 확인한다. | 향후 세분화된 Role/Permission 모델이 확정될 때 enum 범위를 다시 설계한다. |

### Validation과 비즈니스 검증 책임

1. 현재 문제: 형식 오류, 중복, 비밀번호 일치, 탈퇴 문구와 소유권 검증이 DTO·Controller·Service에 혼재한다. 오류 화면에서 request 객체를 그대로 재사용하면 password 같은 민감 입력이 다시 View model에 남을 가능성도 있다.
2. 영향: 같은 입력이 경로마다 다르게 처리되고, 정책 오류가 400인지 redirect인지 불명확하며, 민감정보 재표시 위험이 생긴다.
3. 권장 방향:
   - Bean Validation은 null·blank, 형식, 길이와 수치 범위를 담당한다.
   - Service는 중복, 현재 비밀번호, SOCIAL 제한, 탈퇴 확인, 소유권처럼 DB·인증·제품 정책이 필요한 규칙을 담당한다.
   - Validation 실패 화면에는 password/currentPassword/newPassword/confirm 값을 복사하거나 flash로 전달하지 않는다. 오류 정보와 안전한 필드만 새 View model로 구성한다.
4. 예상 변경 위치: member/post/comment request DTO, 각 View Controller, `MemberService`, `GlobalViewControllerAdvice`, signup·myEdit template의 field binding 검토.
5. 검증 방법: DTO Validator 테스트, Controller BindingResult 테스트, Service 정책 테스트와 렌더링 model에서 민감 필드가 null/미포함인지 확인한다.
6. 보류 조건: REST 오류 응답 구조나 공통 form error abstraction까지 확대해야 한다면 M2 계약과 분리해 별도 설계한다.

## Security

| 품질 부채 | 현재 문제 | 영향 | 권장 방향 | 예상 변경 위치 | 검증 방법 | 보류 조건 |
| --- | --- | --- | --- | --- | --- | --- |
| SOCIAL 비밀번호 변경 | View만 form을 숨기고 Service는 `LoginType.SOCIAL`을 차단하지 않는다. | endpoint 직접 호출로 dummy password 변경을 시도할 수 있다. | 회원 조회 직후 Service에서 LOCAL만 허용하고, 비밀번호 비교·인코딩 전에 거부한다. | `MemberService.updatePassword`, `ErrorCode`, mypage Controller·Service 테스트 | SOCIAL 요청은 encoder와 변경 로직을 호출하지 않고 정책 오류가 되는지 확인한다. | SOCIAL 계정에 로컬 비밀번호를 추가하는 제품 기능을 결정하면 별도 계정 전환 흐름이 필요하다. |
| 게시글 수정 GET | `GET /post/{id}/edit`가 기본 permitAll이고 작성자 검증 없이 상세를 반환한다. | 비작성자와 익명 사용자가 수정 form과 원문을 편집 경로로 조회할 수 있다. | Security rule에서 인증을 요구하고, Service가 현재 인증 member ID와 Post 작성자를 비교한 뒤 form 데이터를 반환한다. | `SecurityConfig`, `PostViewController.editForm`, `PostService`, Security·Service·MVC 테스트 | 익명 login redirect, 비작성자 거부, 작성자 성공을 각각 확인한다. | 공개 preview 같은 별도 요구가 있다면 수정 route와 분리한다. |
| 수정·삭제 소유권 | 화면 표시나 임의 request 값에 의존할 여지가 있다. | 요청 변조로 다른 사용자의 자원을 변경할 위험이 있다. | Controller는 SecurityContext의 principal member ID만 Service에 전달하고 Service가 Entity 작성자와 비교한다. ADMIN 우회는 관리자 전용 route에만 둔다. | Post/Comment View Controller와 Service, `UnifiedPrincipal` 사용부 | request에 다른 member ID를 주입할 수 없는지, USER·ADMIN의 일반 route 소유권이 동일한지 검증한다. | 관리자 대리 수정 정책이 필요하면 명시적 관리자 API와 감사 로그를 먼저 설계한다. |
| local test route | `LocalTestController`는 현재 `@Profile("local")`이다. | profile 조건이 제거되거나 잘못 활성화되면 의도적 오류 route가 운영에 노출될 수 있다. | 기존 profile 제한을 유지하고 non-local context에서 Bean과 mapping이 없음을 회귀 테스트로 고정한다. | `LocalTestController`, profile 설정, Security 통합 테스트 | test/prod profile에서 404 또는 mapping 부재, local에서만 의도된 예외 흐름을 확인한다. | 운영 profile 조합이 확정되지 않았거나 local이 운영과 함께 활성화될 수 있으면 배포 profile부터 정리한다. |
| Actuator | 공통 설정이 health 상세, metrics, mappings를 노출하고 Security의 `anyRequest().permitAll()` 영향을 받는다. | 내부 Bean mapping, 지표와 health detail이 익명 사용자에게 공개될 수 있다. | health는 운영 확인용 익명 접근을 유지하되 detail을 제한한다. metrics/mappings/info의 노출 여부와 ADMIN 인증을 profile 설정과 Security rule 양쪽에서 제한한다. | `application.properties` 및 profile 설정, `SecurityConfig`, Security 통합 테스트 | 익명 health 성공, 익명 상세 endpoint 거부/404, ADMIN 허용, prod health detail 비공개를 확인한다. | 실제 모니터링 수집 주체와 인증 방식이 정해지지 않으면 endpoint 제거 대신 접근 제한만 제안한다. |
| Discord 장애 알림 | 일부 가입 정책 오류만 제외하고 다른 사용자 입력·정책 위반 `CustomException`은 장애처럼 전송될 수 있다. | 알림 소음으로 실제 장애 대응력이 낮아지고 사용자 입력이 외부 채널에 과도하게 노출될 수 있다. | 사용자 입력·인증·소유권·정책 위반은 기본 비알림으로 분류하고, 서버 불변식·외부 연동·운영 장애만 allow-list 방식으로 알린다. payload에 사용자 입력과 비밀값을 포함하지 않는다. | `GlobalViewControllerAdvice`, `DiscordNotifier`, `ErrorCode` 분류 또는 알림 정책 객체, exception 테스트 | 각 ErrorCode 분류, notifier 호출 여부, payload 민감정보 부재와 notifier 실패 시 원 요청 유지 여부를 검증한다. | 장애 등급과 운영 알림 채널 정책이 확정되기 전에는 ErrorCode를 임의로 재분류하지 않는다. |

### 제품 결정이 필요한 보류 항목

1. 현재 문제: LOCAL과 SOCIAL이 같은 email을 사용할 때 연결 여부, 로그인 후 `redirect` parameter 허용 범위, provider별 계정 통합 기준이 정의되지 않았다.
2. 영향: 중복 계정, 계정 탈취, open redirect와 provider 간 예기치 않은 병합 위험이 있다.
3. 권장 방향: email 신뢰도·재인증·명시적 연결 동의, 내부 경로 allow-list, `(provider, providerId)` 기준을 제품·보안 정책으로 먼저 결정한다.
4. 예상 변경 위치: `CustomOauth2UserService`, provider adapter, login success handler, member repository/DB 제약, Security 테스트.
5. 검증 방법: LOCAL 선가입/후가입, provider 두 개의 동일 email, 검증되지 않은 email, 외부 redirect URL과 상대 경로를 시나리오 테스트한다.
6. 보류 조건: 정책 결정과 운영 데이터 정리 계획이 승인되기 전에는 계정 자동 연결이나 redirect 처리를 구현하지 않는다.

## JPA 삭제 무결성

### 삭제 순서

1. 현재 문제: Comment가 parent를 참조하지만 cascade/orphanRemoval이 없고, 게시글·회원 삭제는 넓은 bulk delete 순서에 의존한다. 부모를 먼저 삭제하면 자식 FK가 남을 수 있다.
2. 영향: 데이터에 답글이 존재할 때 제약 위반으로 트랜잭션 전체가 rollback되거나 삭제 정책이 DB 동작에 종속된다.
3. 권장 방향: 현재 한 단계 답글 계약과 명시적 삭제 방식을 유지한다면 다음 순서를 Repository 메서드 이름과 Service 흐름에 드러낸다.

```text
부모 댓글 삭제
  답글 삭제 → 부모 댓글 삭제

게시글 삭제
  해당 게시글의 답글 삭제 → 부모 댓글 삭제 → 게시글 삭제

회원 삭제
  회원 게시글의 답글 삭제 → 부모 댓글 삭제
  회원이 쓴 부모 댓글의 답글 삭제
  회원이 쓴 답글 삭제 → 회원이 쓴 부모 댓글 삭제
  회원 게시글 삭제 → 회원 삭제
```

4. 예상 변경 위치: `CommentRepository`, `PostRepository`, `CommentService`, `PostService`, `MemberService`.
5. 검증 방법: 다른 회원이 작성한 답글이 달린 부모 댓글, 회원 게시글의 혼합 작성자 댓글, 다른 게시글에 회원이 작성한 부모·답글을 fixture로 구성해 Repository와 Service 호출 순서, 최종 row와 rollback을 확인한다.
6. 보류 조건: 대댓글 깊이, 댓글 보존/익명화, soft delete 정책이 바뀌면 위 물리 삭제 순서를 적용하지 않고 모델을 다시 설계한다.

### bulk 연산과 영속성 컨텍스트

| 판단 항목 | 현재 문제 | 영향 | 권장 방향 | 예상 변경 위치 | 검증 방법 | 보류 조건 |
| --- | --- | --- | --- | --- | --- | --- |
| `clearAutomatically` | JPQL bulk update/delete는 관리 Entity를 거치지 않아 DB와 1차 캐시가 달라질 수 있다. | 같은 트랜잭션의 후속 조회·검증이 삭제 전 상태를 볼 수 있다. | bulk 이후 같은 트랜잭션에서 영향을 받은 Entity를 다시 사용하거나 조회한다면 해당 Repository 메서드에 선택적으로 적용한다. | `CommentRepository`·`PostRepository`의 `@Modifying` 메서드 | bulk 전 관리 상태를 만든 뒤 실행 후 `EntityManager.contains`와 재조회 결과를 확인한다. | bulk 직후 트랜잭션이 종료되고 관리 Entity를 다시 쓰지 않는 메서드에는 기계적으로 추가하지 않는다. |
| flush | clear 전에 미반영 변경이 있으면 사라질 수 있다. | 정상 변경이 유실되거나 bulk 조건에 반영되지 않는다. | bulk 전에 반드시 반영해야 할 관리 변경이 있는 흐름에만 명시적 flush 또는 `flushAutomatically`를 검토한다. | Service 트랜잭션 경계와 해당 bulk Repository 메서드 | pending 변경이 있는/없는 두 경로에서 SQL 순서와 최종 값을 확인한다. | 단지 테스트를 통과시키기 위한 무조건 flush는 금지한다. |
| 명시적 clear | 여러 bulk 사이에서 clear 시점을 세밀하게 제어해야 할 수 있다. | 너무 이른 clear는 변경 감지 대상을 분리하고, 늦은 clear는 stale state를 남긴다. | Repository annotation으로 의도가 충분하지 않은 복합 흐름에서만 Service의 EntityManager clear를 고려하고 이유를 주석·테스트로 고정한다. | 복합 삭제 Service와 제한적인 persistence helper | 각 bulk 경계 전후 관리 상태와 후속 삭제가 같은 트랜잭션에서 성공하는지 확인한다. | 간단한 단일 bulk나 annotation으로 충분한 경우 명시적 clear를 추가하지 않는다. |

### 즉시 적용하지 않을 DB·관계 변경

1. 현재 문제: cascade/orphanRemoval, nickname unique, `(provider, providerId)` unique와 Flyway migration이 없다.
2. 영향: 애플리케이션 삭제 순서와 중복 검사에 의존하고 운영 schema 재현성이 낮다.
3. 권장 방향: 삭제 소유권과 보존 정책이 확정된 뒤 cascade/orphanRemoval을 별도 설계한다. unique 제약은 운영 중복 데이터 조사·정리와 함께 migration으로 추가하고, Flyway baseline/버전 정책을 먼저 정한다.
4. 예상 변경 위치: `Member`, `Post`, `Comment`, repository, schema/migration 디렉터리, 배포 설정과 운영 문서.
5. 검증 방법: 운영 데이터 사전 진단, migration dry-run, 중복 삽입 실패, rollback, FK 삭제 시나리오와 신규·기존 환경 재현을 확인한다.
6. 보류 조건: 운영 데이터 변경 승인, 백업·rollback과 배포 창구가 없으면 이번 PRE-M2 문서 범위에서 구현하지 않는다.

## Pagination과 검색

| 품질 부채 | 현재 문제 | 영향 | 권장 방향 | 예상 변경 위치 | 검증 방법 | 보류 조건 |
| --- | --- | --- | --- | --- | --- | --- |
| 전체 제목 검색 | `/posts/search`가 List 검색으로 분리돼 `PageRequestDto`·`PageResultDto`와 목록 통계를 사용하지 않는다. | 검색 결과가 많아질수록 한 번에 로드하고 목록 화면의 page model 계약이 달라진다. | `/`와 `/posts/search`가 같은 pageable Service/Repository 흐름을 사용하고 keyword만 선택 조건으로 전달되게 한다. | `PostViewController`, `PostService`, `PostRepository`, `post/list.html` | 검색 없음/있음, page/size 전달, 정렬과 total count, 기존 URL 호환을 Controller·Service·Repository 테스트로 확인한다. | 검색 의미·정렬 정책이 확정되지 않으면 Querydsl 도입과 함께 확대하지 않는다. |
| 검색어 유지 | page 링크에 keyword가 없다. | 2페이지 이동 시 전체 목록으로 돌아간다. | 이전·번호·다음 링크에 기존 keyword와 size를 함께 유지하고 URL encoding을 template engine에 맡긴다. | `post/list.html`, 목록 Controller model | 공백·한글·특수문자 keyword로 렌더링된 링크와 다음 요청의 binding을 확인한다. | POST 검색이나 복합 filter UI로 바뀌면 query parameter 계약을 다시 정한다. |
| 빈 결과 | `PageResultDto`의 totalPage 0일 때 start/end/pageList 의미가 명시되지 않았다. | template가 존재하지 않는 1페이지 링크를 그리거나 경계 계산이 흔들릴 수 있다. | dtoList/pageList는 빈 목록, totalPage/totalCount/start/end는 0, prev/next는 false라는 계약을 문서화한다. 요청 page 표시 방식은 별도로 정한다. | `PageResultDto`, 목록 template, pagination 단위 테스트 | 빈 전체 목록과 빈 검색 결과에서 예외 없이 빈 상태가 렌더링되는지 확인한다. | UI가 항상 page 1을 표시해야 한다면 start/end 계약을 먼저 선택한다. |
| 범위 초과 page | 양수지만 totalPage보다 큰 요청의 처리 정책이 없다. | 빈 화면, 비정상 page block 또는 route별 다른 동작이 생긴다. | SSR UX 기준 권장은 total이 있으면 마지막 페이지의 canonical URL로 redirect하고, total 0이면 빈 첫 화면을 반환하는 것이다. page<1·size 범위 위반은 400으로 구분한다. | 네 Controller, Post/Comment Service, `PageResultDto` | 실제 범위 초과, keyword가 있는 초과 요청, redirect URL의 keyword/size 보존과 추가 조회 횟수를 확인한다. | strict 400 또는 빈 page 유지가 제품 정책이면 네 route에 동일하게 적용할 때까지 구현을 보류한다. |
| route 일관성 | `/`, `/posts/search`, `/my/posts`, `/my/comments`가 page·keyword를 서로 다르게 받는다. | 공통 DTO를 써도 사용자 경험과 오류 처리가 달라진다. | page/size Validation, 범위 초과 정책과 빈 결과 계약을 네 route에 공통 적용하되 검색 대상 필드는 각 화면에 맞게 유지한다. | `PageRequestDto`, 네 View Controller, Post/Comment Service와 templates | 동일한 경계 입력을 네 route에 반복해 status, model, redirect를 비교한다. | 마이페이지 전용 UX 요구가 있으면 차이를 명시한 뒤 승인한다. |

### 필요한 페이징 경계 테스트

1. 첫 페이지: prev=false, 시작 번호 1, 정상 size와 정렬.
2. 마지막 페이지: next=false, 마지막 block end가 totalPage를 넘지 않음.
3. 빈 검색 결과: 빈 dtoList/pageList, count 0, template 빈 상태.
4. 10페이지 block: 10→11, 20→21 경계에서 prev/next와 start/end.
5. 범위 초과: 전체 목록과 검색 결과 각각 마지막 page redirect 또는 선택한 정책.
6. 검색 조건이 있는 범위 초과: keyword와 size가 보존되고 전체 목록으로 바뀌지 않음.
7. 잘못된 입력: page 0·음수, size 0·상한 초과가 일관된 400.

예상 테스트 위치는 `ControllerMvcTest`, `PostServiceTest`, `PostRepositoryTest`, Comment pagination Service/Repository 테스트와 별도 `PageResultDto` 단위 테스트다.

Querydsl과 cursor pagination은 검색 조건과 데이터 증가 패턴이 확정된 후 별도 단계에서 비교한다. 검색 인덱스, 쿼리 수 계측과 대용량 성능 검증은 M4로 이관하며 PRE-M2에서 구현하지 않는다.

## M2 REST 계약 설계 전에 결정할 항목

- REST field error와 비즈니스 오류의 JSON 구조·HTTP status
- Session+CSRF 유지 또는 별도 API 인증 방식
- LOCAL/SOCIAL 계정 연결, provider 통합과 redirect allow-list 정책
- page/size 상한과 범위 초과 page의 SSR·REST별 처리 차이
- 명시적 삭제 유지, cascade/orphanRemoval 또는 soft delete 선택
- unique 제약과 Flyway 도입을 위한 운영 데이터 정리·rollback 계획

위 결정 전까지 이 문서는 구현 승인으로 해석하지 않는다.
