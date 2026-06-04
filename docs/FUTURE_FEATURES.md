# Board 추가 기능 및 개선 제안

## 1. 현재 프로젝트 기능 요약

현재 코드 기준으로 구현된 기능은 다음과 같다.

| 영역 | 구현된 기능 |
|---|---|
| 회원 | 회원가입, form login, logout |
| OAuth2 | Google, Kakao, Naver 로그인 |
| 보안 | Spring Security 기반 인증/인가, `ROLE_USER`, `ROLE_ADMIN` |
| 게시글 | 목록 조회, 검색, 상세 조회, 작성, 수정, 삭제 |
| 댓글 | 댓글 작성, 수정, 삭제, 대댓글 작성 |
| 조회수 | 게시글 상세 조회 시 쿠키 기반 중복 방지 후 DB update |
| 마이페이지 | 내 정보, 내 게시글 수, 내 댓글 수, 최근 작성 글, 최근 작성 댓글, 내 글 목록, 내 댓글 목록 |
| 관리자 | 관리자 대시보드, 게시글 목록/삭제, 회원 목록, 회원 권한 변경, 회원 삭제 |
| 예외 처리 | `CustomException`, `ErrorCode`, `GlobalControllerAdvice` |
| 로깅/알림 | Slf4j, P6Spy Formatter, Discord 알림 컴포넌트 |
| 화면 | Thymeleaf + Bootstrap 기반 SSR 화면 |

현재 코드에 없는 기능:

- 게시판/카테고리 분리
- 게시글/댓글 좋아요
- 파일 첨부
- 신고 기능
- 실시간 알림
- REST API 응답 표준화
- Swagger/OpenAPI 또는 Spring REST Docs
- Redis 기반 조회수 처리

## 2. 추가하면 좋은 기능 목록

### 게시판/카테고리 기능

현재 프로젝트는 모든 게시글이 하나의 목록에 표시된다. 게시판/카테고리 기능을 추가하면 자유게시판, 공지사항, 질문게시판처럼 게시글을 분류할 수 있다.

기능 후보:

- 자유게시판, 공지사항, 질문게시판 등 게시판 분리
- 관리자만 게시판/카테고리 생성, 수정, 삭제 가능
- 게시글 작성 시 게시판 선택
- 게시글 목록에서 게시판별 필터링
- 게시판별 공지글 또는 권한 정책

포트폴리오 관점 도입 가치:

- 도메인 모델 확장 능력을 보여주기 좋다.
- 관리자 기능과 게시글 도메인을 자연스럽게 연결할 수 있다.
- 검색/페이징/권한 정책을 함께 개선할 수 있다.

예상 변경 범위:

| 계층 | 예상 변경 |
|---|---|
| Entity | 신규 `Board`, `Category`, `Post.board` 또는 `Post.category` |
| DTO | 게시글 작성/수정 요청에 boardId/categoryId 추가 |
| Controller | 게시판별 목록, 관리자 게시판 관리 |
| Service | 게시판 존재 검증, 게시판별 조회 |
| Repository | boardId/categoryId 조건 검색 |
| Template | 게시판 선택 UI, 게시판 필터 |

### 좋아요 기능

게시글과 댓글에 좋아요 기능을 추가할 수 있다.

기능 후보:

- 게시글 좋아요
- 댓글 좋아요
- 회원당 1회 좋아요 제한
- 좋아요 취소 기능
- 좋아요 수 표시

도입 시 고려사항:

- `post_like(member_id, post_id)` unique 제약 필요
- `comment_like(member_id, comment_id)` unique 제약 필요
- 동시에 여러 요청이 들어올 때 중복 insert를 방지해야 한다.
- 좋아요 수를 매번 count할지, Post/Comment에 count 컬럼을 둘지 결정해야 한다.

예상 ERD:

```text
member 1:N post_like N:1 post
member 1:N comment_like N:1 comment
```

예상 변경 범위:

- 신규 Entity: `PostLike`, `CommentLike`
- 신규 Repository: `PostLikeRepository`, `CommentLikeRepository`
- 신규 Service: `LikeService`
- Controller: 게시글/댓글 좋아요 요청
- Template: 좋아요 버튼, 좋아요 수 표시

### 조회수 개선

현재 조회수는 게시글 상세 조회 시 쿠키 `View_Count`를 사용해 12시간 중복 조회를 방지하고, DB update query로 증가시킨다.

현재 방식의 장점:

- 구현이 단순하다.
- DB update query라 엔티티 read-modify-write보다 동시성에 유리하다.

현재 방식의 문제점:

- 쿠키 삭제 또는 브라우저 변경 시 중복 조회를 막기 어렵다.
- 비회원과 회원을 정교하게 구분하지 않는다.
- 모든 유효 조회가 DB update를 발생시킨다.
- 인기 게시글 같은 통계 확장에 한계가 있다.

개선 방향:

- Redis Set으로 `postId:userKey` 조회 여부 저장
- Redis Counter로 조회수 증가분 임시 누적
- 스케줄러로 DB에 주기적 반영
- 회원은 memberId, 비회원은 IP/User-Agent hash 등을 기준으로 중복 방지
- 인기 게시글 기능으로 확장

포트폴리오 관점 도입 가치:

- Redis 활용 이유를 명확히 설명할 수 있다.
- 동시성, 캐싱, 배치 반영 설계를 보여줄 수 있다.

### 파일 첨부 기능

게시글에 이미지 또는 파일을 첨부할 수 있도록 확장할 수 있다.

기능 후보:

- 게시글 이미지 첨부
- 첨부파일 다운로드
- 파일 메타데이터 테이블 분리
- 썸네일 표시

저장소 비교:

| 저장소 | 장점 | 단점 |
|---|---|---|
| 로컬 | 구현이 단순하고 학습용으로 빠름 | 서버 교체/스케일아웃에 취약 |
| AWS S3 | 운영 환경에 적합, 확장성 좋음 | 비용, IAM, presigned URL 등 추가 학습 필요 |

보안 검증:

- 파일 크기 제한
- 확장자 allow-list
- MIME type 검증
- 파일명 직접 사용 금지
- 다운로드 권한 검증
- 실행 가능한 파일 업로드 차단

예상 변경 범위:

- 신규 Entity: `Attachment`
- Service: 파일 저장/삭제/다운로드
- Controller: multipart form 처리
- Template: 파일 업로드 input, 첨부 목록

### 알림 기능

댓글/대댓글 이벤트를 기반으로 사용자 알림을 제공할 수 있다.

기능 후보:

- 내 게시글에 댓글이 달렸을 때 알림
- 내 댓글에 답글이 달렸을 때 알림
- 알림 읽음 처리
- 알림 목록 조회
- 실시간 알림 확장

실시간 확장 방식:

| 방식 | 특징 |
|---|---|
| SSE | 서버에서 클라이언트로 단방향 이벤트 전달. 알림에 적합 |
| WebSocket | 양방향 통신. 채팅 등 상호작용이 많은 기능에 적합 |

예상 변경 범위:

- 신규 Entity: `Notification`
- Service: 알림 생성/읽음 처리
- Controller: 알림 목록, 읽음 처리
- Template: 알림 아이콘, 알림 목록

### 신고/관리 기능

운영 관점에서 게시글/댓글 신고와 관리자 처리 기능을 추가할 수 있다.

기능 후보:

- 게시글 신고
- 댓글 신고
- 관리자 신고 목록 조회
- 신고 처리 상태 관리
- 숨김 처리 또는 삭제 처리

설계 선택지:

- 단일 `report` 테이블에 `targetType`, `targetId`를 저장
- `post_report`, `comment_report`로 분리

상태 예시:

- `PENDING`
- `ACCEPTED`
- `REJECTED`
- `HIDDEN`

예상 변경 범위:

- 신규 Entity: `Report`
- 관리자 Controller/Service/Repository 추가
- 게시글/댓글 화면에 신고 버튼 추가

### 관리자 기능 고도화

현재 관리자는 게시글/회원 목록, 삭제, 권한 변경을 수행할 수 있다. 운영 기능을 강화할 수 있다.

기능 후보:

- 관리자 통계 대시보드
- 가입자 증가 추이
- 최근 게시글/댓글 모니터링
- 신고 관리
- 관리자 작업 로그
- 마지막 관리자 삭제 방지
- 자기 자신 삭제 방지

예상 변경 범위:

- AdminController 분리
- AdminService 추가
- 관리자 전용 DTO 도입
- 관리자 template 개선

### 검색 기능 개선

현재 게시글 검색은 제목 기준만 지원한다. 내 댓글 검색은 댓글 내용 또는 게시글 제목 기준으로 구현되어 있다.

개선 후보:

- 제목/내용 검색
- 작성자 검색
- 카테고리별 검색
- 날짜 조건 검색
- 정렬 조건
- 검색 결과 페이징

Querydsl 도입 가능성:

- 동적 조건 조합이 쉬워진다.
- 검색 조건이 늘어날 때 Repository method 이름이 과도하게 길어지는 문제를 줄인다.

인덱스 설계 후보:

- `post(title)`
- `post(member_id, created_at)`
- `post(created_at)`
- `comment(member_id, created_at)`
- `comment(post_id)`
- `member(email)`
- `member(nickname)`

### 테스트 코드 보강

현재 테스트 파일은 존재하지만 최신 메인 코드와 맞지 않아 컴파일 실패한다. 가장 먼저 복구할 가치가 높다.

보강 대상:

- Service 단위 테스트
- Controller 테스트
- Repository 테스트
- Security 인증/인가 테스트
- OAuth2 로그인 테스트
- 예외 처리 테스트
- 관리자 기능 테스트

우선순위:

1. 현재 컴파일 실패 테스트 복구
2. 게시글/댓글/회원 Service 핵심 테스트
3. Security 접근 제어 테스트
4. Controller MVC 테스트
5. Repository query 테스트

### API 서버 분리

현재 SSR 구조는 단순 게시판에 적합하다. 프론트엔드를 React/Vue로 분리하거나 모바일 앱을 지원하려면 REST API가 필요하다.

SSR 유지 장점:

- 구현이 단순하다.
- form validation과 redirect 흐름이 자연스럽다.
- SEO와 초기 렌더링이 쉽다.

프론트엔드 분리 시 고려사항:

- REST API 설계
- DTO 응답 구조 표준화
- API 예외 응답 표준화
- 인증 방식 재검토: Session, JWT, OAuth2
- CORS/CSRF 정책
- Swagger/OpenAPI 또는 Spring REST Docs 도입

### 배포/운영 개선

개선 후보:

- 운영/개발 프로필 분리 문서화
- Docker Compose 구성 개선
- GitHub Actions CI/CD
- AWS EC2/RDS 배포
- Nginx Reverse Proxy
- HTTPS 적용
- 로그 관리
- Actuator 모니터링
- 장애 대응 문서화

운영 문서에 포함하면 좋은 항목:

- 환경 변수 목록
- DB migration 전략
- 배포 절차
- 롤백 절차
- 장애 확인 방법
- 로그 확인 방법

## 3. 기능별 우선순위

### 테스트 코드 보강

- 우선순위: 높음
- 도입 이유: 현재 테스트가 컴파일 실패하므로 품질 기준이 무너져 있다.
- 포트폴리오 어필 포인트: 리팩터링 안정성, TDD/테스트 전략 설명 가능
- 예상 난이도: 중간
- 예상 변경 범위: `src/test/java/project/board/**`
- 관련 도메인: 전체
- 구현 시 주의사항: 현재 메인 코드 시그니처에 맞게 테스트 fixture를 먼저 정리해야 한다.

### 게시판/카테고리 기능

- 우선순위: 높음
- 도입 이유: 단일 게시판 구조를 실제 서비스에 가까운 구조로 확장할 수 있다.
- 포트폴리오 어필 포인트: ERD 확장, 관리자 기능, 검색/필터링 설계
- 예상 난이도: 중간
- 예상 변경 범위: Entity, DTO, Controller, Service, Repository, Template
- 관련 도메인: Post, Admin
- 구현 시 주의사항: 기존 게시글 데이터의 기본 게시판 migration이 필요하다.

### 조회수 개선

- 우선순위: 높음
- 도입 이유: 현재 쿠키 + DB update 방식의 중복 방지와 DB 부하 한계를 개선한다.
- 포트폴리오 어필 포인트: Redis, 동시성, 캐싱, 배치 반영 설계
- 예상 난이도: 중간~높음
- 예상 변경 범위: Post 조회 흐름, Redis 설정, 스케줄러
- 관련 도메인: Post
- 구현 시 주의사항: Redis 장애 시 fallback, DB 반영 누락 방지, 중복 기준 정의가 필요하다.

### 좋아요 기능

- 우선순위: 중간
- 도입 이유: 사용자 참여 기능을 추가할 수 있다.
- 포트폴리오 어필 포인트: unique constraint, 동시성 제어, 토글 API 설계
- 예상 난이도: 중간
- 예상 변경 범위: Like Entity/Repository/Service/Controller, 게시글/댓글 화면
- 관련 도메인: Member, Post, Comment
- 구현 시 주의사항: 중복 좋아요 요청과 취소 요청의 동시성 처리가 필요하다.

### 파일 첨부

- 우선순위: 중간
- 도입 이유: 게시판 기능 완성도를 높인다.
- 포트폴리오 어필 포인트: multipart 처리, S3 연동, 보안 검증
- 예상 난이도: 중간~높음
- 예상 변경 범위: Attachment Entity, FileService, Controller, Template
- 관련 도메인: Post
- 구현 시 주의사항: 파일명 sanitizing, MIME 검증, 저장소 장애 처리, 삭제 정책이 필요하다.

### 검색 고도화

- 우선순위: 중간
- 도입 이유: 현재 제목 검색만 지원하므로 실사용 검색 품질이 낮다.
- 포트폴리오 어필 포인트: Querydsl, 인덱스 설계, 페이징 최적화
- 예상 난이도: 중간
- 예상 변경 범위: Repository, Service, 검색 DTO, Template
- 관련 도메인: Post, Comment, Member
- 구현 시 주의사항: 검색 조건 증가에 따른 쿼리 복잡도와 count 쿼리 비용을 관리해야 한다.

### 신고/관리 기능

- 우선순위: 중간
- 도입 이유: 운영자 관점의 관리 기능을 강화한다.
- 포트폴리오 어필 포인트: 상태 모델링, 관리자 워크플로우, soft delete 정책
- 예상 난이도: 중간
- 예상 변경 범위: Report Entity/Repository/Service, Admin UI
- 관련 도메인: Member, Post, Comment, Admin
- 구현 시 주의사항: 신고 대상 모델링과 중복 신고 제한이 필요하다.

### 실시간 알림

- 우선순위: 낮음
- 도입 이유: 댓글/대댓글 반응을 사용자에게 알려 UX를 높인다.
- 포트폴리오 어필 포인트: SSE/WebSocket, 이벤트 기반 설계
- 예상 난이도: 높음
- 예상 변경 범위: Notification Entity/Service, SSE/WebSocket 설정, UI
- 관련 도메인: Member, Post, Comment
- 구현 시 주의사항: 읽음 처리, 재연결, 알림 중복 방지가 필요하다.

### API 서버 분리

- 우선순위: 낮음
- 도입 이유: 프론트엔드 분리나 모바일 앱 확장에 대응한다.
- 포트폴리오 어필 포인트: REST API 설계, 오류 응답 표준화, 문서화
- 예상 난이도: 높음
- 예상 변경 범위: API Controller, Response DTO, Exception Handler, Security 설정
- 관련 도메인: 전체
- 구현 시 주의사항: 기존 SSR Controller와 책임이 섞이지 않도록 분리해야 한다.

## 4. 추천 개발 로드맵

### 1단계: 문서화 및 테스트 기반 정리

- README.md 작성
- API 문서 작성
- 요구사항 정의서 작성
- ERD 정리
- 현재 실패하는 테스트 컴파일 복구
- Service 단위 테스트 작성
- Security 접근 제어 테스트 추가

목표:

- 프로젝트를 설명 가능한 상태로 만든다.
- 기능 추가 전 안정적인 테스트 기반을 확보한다.

### 2단계: 게시판 도메인 확장

- 게시판/카테고리 Entity 추가
- 게시글 작성 시 게시판 선택
- 게시판별 목록 조회
- 관리자 게시판 관리 기능
- 검색/페이징과 게시판 필터 연동

목표:

- 단일 게시판을 실제 서비스형 구조로 확장한다.

### 3단계: 성능/동시성 개선

- 조회수 중복 방지 기준 재설계
- Redis 기반 조회수 저장
- 스케줄러로 DB 반영
- 인기 게시글 기능 추가
- 주요 조회 쿼리 N+1 점검

목표:

- 단순 CRUD를 넘어 성능 개선 경험을 확보한다.

### 4단계: 사용자 참여 기능 추가

- 게시글 좋아요
- 댓글 좋아요
- 신고 기능
- 관리자 신고 처리
- 알림 기능의 기본 DB 구조 추가

목표:

- 사용자가 상호작용하는 게시판으로 발전시킨다.

### 5단계: 운영/배포 개선

- Docker Compose 정리
- GitHub Actions CI 구성
- AWS EC2/RDS 배포
- Nginx Reverse Proxy
- HTTPS 적용
- Actuator 기반 모니터링
- 장애 대응 문서화

목표:

- 포트폴리오에서 운영 가능한 프로젝트로 보이게 만든다.

### 6단계: API 확장

- SSR Controller와 REST Controller 분리
- 공통 API 응답 형식 정의
- 공통 API 예외 응답 정의
- Swagger/OpenAPI 또는 Spring REST Docs 도입
- React/Vue 분리 가능성 검토

목표:

- 프론트엔드 분리 또는 외부 클라이언트 대응 구조로 확장한다.

## 5. 면접 대비 포인트

### 왜 Redis를 조회수에 도입하려고 하는가?

답변 방향:

- 현재 방식은 쿠키와 DB update에 의존해 중복 방지와 DB 부하에 한계가 있다.
- Redis Set으로 조회 여부를 빠르게 판단하고, Counter로 증가분을 누적한 뒤 주기적으로 DB에 반영하면 DB write 부하를 줄일 수 있다.
- 인기 게시글 산정에도 Redis 자료구조를 활용할 수 있다.

### 좋아요 기능에서 동시성 문제를 어떻게 해결할 것인가?

답변 방향:

- DB unique constraint로 회원당 1회 좋아요를 보장한다.
- 애플리케이션에서 먼저 조회 후 insert하는 방식은 race condition이 있으므로 DB 제약을 최종 방어선으로 둔다.
- 좋아요 수 denormalization을 한다면 optimistic lock, atomic update query, 이벤트 기반 집계를 고려한다.

### 게시판/카테고리 도메인을 어떻게 설계할 것인가?

답변 방향:

- 게시판 단위가 크고 공지/질문/자유게시판처럼 독립 정책이 필요하면 `Board`를 둔다.
- 게시판 내부 세부 분류가 필요하면 `Category`를 둔다.
- `Post`는 `board_id` 또는 `category_id`를 FK로 가지고, 목록 조회는 해당 FK 기준으로 필터링한다.

### 회원 삭제 시 게시글과 댓글은 어떻게 처리할 것인가?

답변 방향:

- 현재 코드는 회원 댓글, 회원 게시글의 댓글, 회원 게시글, 회원 순서로 hard delete한다.
- 운영 환경에서는 작성 콘텐츠를 보존해야 할 수 있으므로 soft delete 또는 탈퇴 회원 익명화 정책을 고려한다.
- FK 제약, 감사 로그, 신고 처리 내역과 함께 정책을 정해야 한다.

### Soft Delete를 도입할 것인가?

답변 방향:

- 게시글/댓글/회원 관리에서는 복구와 감사가 필요할 수 있어 soft delete가 유리하다.
- 단순 개인 프로젝트나 데이터 보존 요구가 없으면 hard delete도 가능하다.
- 도입 시 모든 조회 쿼리에 `deleted = false` 조건을 일관되게 적용해야 한다.

### SSR 구조와 REST API 구조의 차이는 무엇인가?

답변 방향:

- SSR은 서버가 View를 렌더링하고 Form POST 후 redirect하는 흐름이 자연스럽다.
- REST API는 JSON 응답을 제공하고 프론트엔드가 화면 상태를 관리한다.
- 현재 프로젝트는 Thymeleaf SSR이므로 Controller가 View 이름과 Redirect를 반환한다.
- API 분리 시 응답 DTO, 오류 응답 표준, 인증/CSRF/CORS 정책을 별도로 설계해야 한다.

### 테스트 코드는 어떤 기준으로 작성할 것인가?

답변 방향:

- 우선 Service 단위 테스트로 핵심 비즈니스 규칙을 검증한다.
- 그 다음 Controller 테스트로 validation, redirect, model attribute를 확인한다.
- Repository 테스트로 fetch join, projection, 검색 쿼리를 검증한다.
- Security 테스트로 비회원/회원/관리자 접근 제어를 검증한다.

### OAuth2 로그인 회원과 일반 로그인 회원을 어떻게 구분했는가?

답변 방향:

- `Member.loginType`으로 `LOCAL`, `SOCIAL`을 구분한다.
- OAuth2 회원은 `provider`, `providerId`를 저장해 provider 계정과 매핑한다.
- 일반 회원은 이메일/비밀번호 기반이며 비밀번호는 BCrypt로 저장한다.
- 소셜 회원은 서비스 내부 비밀번호 변경 UI를 제공하지 않는다.

### 댓글의 대댓글 구조를 어떻게 설계했는가?

답변 방향:

- `Comment.parent` self reference로 대댓글을 표현한다.
- `parent_id`가 null이면 root comment, 값이 있으면 reply이다.
- 현재 서비스는 부모가 대댓글이면 추가 답글을 막아 1단계 대댓글만 허용한다.
- 부모 댓글 삭제 정책은 개선 필요 지점이다.

### N+1 문제는 어디에서 발생할 수 있는가?

답변 방향:

- 게시글 목록은 작성자 fetch join을 적용했다.
- 상세 조회에서는 `post.comments`, comment member, replies 접근 시 Lazy loading으로 N+1 가능성이 있다.
- 해결 방법은 fetch join, EntityGraph, DTO projection, 전용 query 분리이다.

### 관리자 기능에서 보완할 점은 무엇인가?

답변 방향:

- 현재 관리자는 게시글/회원 조회와 삭제, 권한 변경을 할 수 있다.
- 보완할 점은 페이징, 검색, 자기 자신 삭제 방지, 마지막 관리자 삭제 방지, 관리자 작업 로그이다.
- 신고 처리와 콘텐츠 숨김 기능을 추가하면 운영 기능으로 확장할 수 있다.
