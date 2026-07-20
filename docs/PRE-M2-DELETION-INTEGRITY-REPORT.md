# PRE-M2 삭제 무결성 보고서

## 작업 기준과 수정 전 위험

- branch: `recover`
- 시작 commit: `5ac12027d2db69c1e1ca322e31108fdb7fe11668`
- 시작 working tree: clean
- 비교 기준: `origin/master` `5fc0368ce2b9c402c4d444744e1d59de4c6f5b9a`
- 참고 문서: `docs/PRE-M2-QUALITY-REPORT.md`

`Comment.parent_id`는 같은 Comment 테이블을 참조하며 cascade와 orphanRemoval이 없다. 기존 게시글·회원 삭제는 답글과 부모 댓글을 구분하지 않는 넓은 bulk delete를 사용해, DB가 부모 row를 먼저 처리하면 자기 참조 FK 위반으로 전체 트랜잭션이 실패할 위험이 있었다. 부모 댓글 단건 삭제도 답글을 먼저 제거하지 않았다.

ViewController, Thymeleaf, REST API, React, cascade/orphanRemoval, soft delete, DB migration과 FK 정의는 변경하지 않았다.

## 적용한 물리 삭제 순서

### 댓글 단건 삭제

1. 기존 댓글 조회
2. 기존 postId 관계와 작성자 검증
3. `deleteRepliesByParentId(commentId)`
4. 부모 댓글 Entity 삭제

대상이 답글이면 3번 쿼리는 삭제 행 없이 종료되고 해당 답글만 삭제된다. 권한 또는 postId 검증 실패 시 어떤 delete도 실행되지 않는다.

### 게시글 삭제

일반 사용자와 관리자 경로가 같은 내부 순서를 사용한다.

1. `deleteRepliesByPostId(postId)`
2. `deleteRootCommentsByPostId(postId)`
3. 게시글 삭제

일반 사용자 경로의 작성자 검증과 관리자 전용 경로의 기존 분리는 유지했다.

### 회원 삭제와 탈퇴

1. 회원 게시글의 답글 삭제
2. 회원 게시글의 부모 댓글 삭제
3. 회원이 작성한 부모 댓글에 달린 답글 삭제
4. 회원이 작성한 답글 삭제
5. 회원이 작성한 부모 댓글 삭제
6. 회원 게시글 삭제
7. 회원 삭제

관리자 회원 삭제와 탈퇴가 같은 정책 메서드를 사용한다. 탈퇴의 정확한 `회원탈퇴` 확인 문구와 Service `@Transactional` 경계는 유지했다.

## 예상 SQL과 트랜잭션

Repository 메서드는 각각 조건이 명시된 JPQL bulk delete를 실행한다. 예상 SQL 흐름은 `delete comment where parent_id ...`, `delete comment where post_id ... and parent_id is/is not null`, `delete post where member_id ...`, `delete member where id ...` 순서다.

모든 순서는 기존 Service 트랜잭션 안에서 실행된다. 후속 작업이 실패하면 선행 bulk delete도 함께 rollback된다. bulk 직후 삭제된 Comment/Post를 재조회하거나 수정하지 않으므로 `clearAutomatically`, `flushAutomatically`, `EntityManager.clear()`는 추가하지 않았다. 통합 테스트의 인위적 후속 예외에서 게시글·부모 댓글·답글이 모두 복구됨을 확인했다.

## 변경 파일

- `CommentRepository`: 부모, 게시글, 게시글 작성자, 댓글 작성자별 답글·부모 댓글 삭제 쿼리 분리
- `PostRepository`: 회원 게시글 삭제 대상을 이름에 명시
- `CommentService`: 답글 선삭제 후 댓글 삭제
- `PostService`: 일반·관리자 경로의 공통 답글→부모→게시글 순서
- `MemberService`: 회원 관련 댓글 그래프→게시글→회원 순서
- Repository·Service 테스트: 메서드명, 호출 횟수와 순서 회귀
- `DeletionIntegrityIntegrationTest`: 실제 H2 FK, 무관 데이터 보존, rollback 검증

## 검증 결과

| 검증 | 결과 |
| --- | --- |
| Repository·Service·삭제 통합 대상 테스트 | 42 tests, failures 0, errors 0, skipped 0 |
| `gradlew.bat clean test` | 82 tests, failures 0, errors 0, skipped 0 |
| `gradlew.bat clean build` | 82 tests, failures 0, errors 0, skipped 0; Boot JAR 생성 |
| `git diff --check` | commit 직전 실행 |

Gradle 전체 실행은 도구의 120초 대기 제한을 넘겼지만 test worker 종료, XML 집계와 JAR 생성으로 실제 결과를 확인했다. 기존 소유권, 탈퇴 확인, Security, Validation 테스트가 전체 82개에 포함된다.

## 남은 위험

- 운영 DB의 실제 FK가 Entity 정의와 다른지는 migration 없이 확인할 수 없다.
- JPQL bulk delete는 영속성 컨텍스트의 관리 Entity 상태를 자동 동기화하지 않는다. 현재 흐름은 삭제 후 해당 Entity를 재사용하지 않지만, 향후 같은 트랜잭션에 후속 조회·수정이 추가되면 stale 상태 테스트와 clear/flush 정책을 다시 검토해야 한다.
- 댓글 깊이를 2단계 이상으로 확장하거나 보존·익명화 정책을 도입하면 현재 순서를 재설계해야 한다.

## Git 결과

- commit 메시지: `fix: PRE-M2 삭제 무결성 강화`
- push 대상: `origin/recover`
- 자기 참조 commit SHA는 보고서에 기록하지 않는다.
