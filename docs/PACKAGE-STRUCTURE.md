# 패키지 구조

> 기준: 로컬 `recover` / `d333e3868e5bb94073030780ce0910a65b3ef4d8` / 2026-07-13

## 실제 트리

```text
project.board
├─ BoardApplication
├─ admin
│  └─ controller.view.AdminViewController
├─ comment
│  ├─ controller.view.CommentViewController
│  ├─ dto.request / dto.response
│  ├─ entity.Comment
│  ├─ repository.CommentRepository
│  └─ service.CommentService
├─ member
│  ├─ controller.view.MemberViewController
│  ├─ dto.request / dto.response
│  ├─ entity.Member, Role, LoginType
│  ├─ repository.MemberRepository
│  └─ service.MemberService
├─ mypage
│  └─ controller.view.MyPageViewController
├─ post
│  ├─ controller.view.PostViewController
│  ├─ controller.api.PostApiController (빈 일반 클래스)
│  ├─ dto.request / dto.response
│  ├─ entity.Post
│  ├─ repository.PostRepository
│  └─ service.PostService
└─ global
   ├─ entity.BaseEntity
   ├─ exception / exception.handler
   ├─ logging.P6SpyFormatter
   ├─ notification.config / notification.discord
   ├─ pagination.PageRequestDto, PageResultDto
   ├─ security.config / handler / oauth / principal / user
   └─ test.LocalTestController
```

## 책임과 대표 클래스

| 패키지 | 책임 | 주요 외부 의존 |
| --- | --- | --- |
| `member` | 가입, 프로필 변경, 탈퇴·관리자 삭제 정책, 인증 대상 회원 조회 | `post`/`comment` Repository, `global.exception` |
| `post` | 게시글 조회·CRUD·조회수·통계 | `member` Repository, `comment` Repository/DTO, pagination |
| `comment` | 댓글/대댓글, 내 댓글 통계·검색 | `member`와 `post` Repository, pagination |
| `mypage` | 여러 기능 Service를 조합해 마이페이지 View 구성, 인증 갱신·로그아웃 | `member`, `post`, `comment`, `global.security` |
| `admin` | 관리자 View와 member/post Service 조합 | `member`, `post` |
| `global` | 보안, 예외, audit, 페이지네이션, 로깅, 알림, local 테스트 route | 대부분의 기능 패키지에서 참조 |

## 계층 배치와 의존 관계

- View Controller는 request/form 객체를 바인딩하고 Service를 호출한 뒤 Model과 View/Redirect를 만든다.
- Service는 Entity 조회·검증·변경과 트랜잭션을 맡으며 다른 기능의 Repository를 직접 참조하기도 한다.
- Repository는 Spring Data 파생 쿼리, JPQL fetch join, DTO projection, bulk update/delete를 함께 보유한다.
- Entity는 기능 패키지에 있고 공통 audit 필드는 `BaseEntity`에 있다.
- response DTO는 View model용이며 Admin Controller는 DTO 대신 `Member`, `Post` Entity 목록을 View에 직접 전달한다.

## 경계와 명명상 관찰

- `mypage`와 `admin`에는 자체 Service/DTO가 없고 타 기능 Service의 조합 Controller만 있다.
- `MemberViewController`는 사용하지 않는 `PostService`, `CommentService`를 생성자 주입한다.
- `PostRecent`는 응답 projection인데 `post.dto.request`에 위치한다.
- `MemberUpdateRequest`, `MemberResponse`는 현재 main 코드에서 사용되지 않는다.
- `PostApiController`는 이름과 디렉터리만 API를 암시하며 실제 Spring Controller가 아니다.
- `global.test.LocalTestController`는 `local` 프로필에서 `/test/discord-error` JSON 문자열 route를 제공하지만 정상 기능 REST API는 아니다.
- 메서드 이름 `MyPageViewController.EditForm`과 `MemberService.MemberRemovalPolicy`는 Java 관례의 lowerCamelCase와 맞지 않는다.
- 과거 문서의 `PostController`, `MemberController`, `MyController`, `AdminController`는 현재 클래스명이 아니다.

## View와 정적 리소스

- 템플릿: `templates/admin`, `member`, `my`, `post`, `fragments` 아래 16개 HTML.
- 작성/수정/삭제 폼은 Thymeleaf action과 Spring Security CSRF hidden field 자동 통합을 사용한다.
- 정적 리소스는 `static/css/app.css` 하나이며 별도 JavaScript 파일은 없다. 회원 탈퇴 확인 JavaScript는 템플릿에 인라인으로 있다.
