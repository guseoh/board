# PRE-M2 페이지·검색 계약 보고서

## 작업 기준

- branch: `recover`
- 시작 commit: `96b20a3542680240d98515a999c4bf5357b6311e`
- 범위: 게시글 목록·제목 검색의 Service/Repository 페이지 계약, `PageResultDto` 경계와 관련 테스트
- 제외: 모든 ViewController, Thymeleaf, REST API, React, Querydsl, 인덱스·쿼리 최적화

## 수정 전 문제

전체 게시글 목록은 `Page<Post>`로 조회했지만 제목 검색은 `List<Post>`로 전체 결과를 한 번에 조회했다. 이 때문에 목록과 검색이 서로 다른 반환 계약을 사용했고, 향후 REST API에서 page, size, total count를 일관되게 제공하기 어려웠다.

`PageResultDto`는 결과가 없을 때 `totalPage=0`, `end=0`이지만 `start=1`로 남아 빈 페이지 계약이 명확하지 않았다.

## 변경 내용

### 게시글 목록·검색 통합

`PostRepository.findPosts(keyword, pageable)`를 추가했다.

- keyword가 null 또는 빈 문자열이면 전체 게시글 조회
- keyword가 있으면 제목 부분 검색
- 목록과 검색 모두 같은 `Page<Post>` 계약 사용
- 작성자 nickname 변환을 위해 member fetch join 유지
- 별도 count query로 전체 검색 결과 수 계산
- 정렬은 기존처럼 Service가 전달하는 `id DESC` Pageable 사용

`PostService.getPosts()`는 `PageRequestDto.keyword`를 Repository에 전달한다. 따라서 이후 REST API는 별도 검색 Service를 만들지 않고 같은 메서드를 사용할 수 있다.

현재 SSR의 `/posts/search`는 ViewController를 수정하지 않는다는 범위 때문에 기존 비페이징 `search(String)` 흐름을 유지한다.

### 빈 페이지 계약

결과가 없으면 `PageResultDto`는 다음 값을 반환한다.

```text
start = 0
end = 0
prev = false
next = false
pageList = []
```

10→11, 20→21 페이지 블록 경계도 테스트로 고정했다.

## 변경 파일

- `PostRepository`: keyword 선택 조건과 count query를 가진 pageable 조회 추가
- `PostService`: 전체 목록과 keyword 검색을 같은 페이지 조회로 연결
- `PageResultDto`: 빈 결과 계약 명시
- `PostRepositoryTest`: 전체·검색 total count, 정렬, member fetch, 기존 검색 호환 검증
- `PostServiceTest`: keyword 전달과 페이지 응답 변환 검증
- `PageResultDtoTest`: 빈 결과와 10페이지 단위 블록 경계 검증

## 검증 상태

연결된 GitHub 저장소에는 변경을 반영했지만, 현재 ChatGPT 실행 환경에서는 사용자의 Windows 로컬 저장소와 Gradle dependency cache에 접근할 수 없어 Gradle 테스트를 실행하지 못했다.

다음 명령은 아직 성공으로 기록하지 않는다.

```powershell
.\gradlew.bat test --tests project.board.post.repository.PostRepositoryTest --tests project.board.post.service.PostServiceTest --tests project.board.global.pagination.PageResultDtoTest
.\gradlew.bat clean test
.\gradlew.bat clean build
git diff --check 96b20a3542680240d98515a999c4bf5357b6311e..recover
```

## 영향과 남은 위험

- `/` 요청에 keyword가 함께 전달되면 새 pageable 검색 계약을 사용할 수 있다.
- 기존 `/posts/search` 화면은 ViewController를 수정하지 않아 여전히 비페이징이다. M2 REST API 또는 React 전환 시 새 `getPosts(PageRequestDto)` 계약으로 교체한다.
- `LIKE '%keyword%'`는 데이터가 많을 때 전체 스캔 가능성이 있다. 인덱스와 검색 성능 최적화는 M4 측정 이후 진행한다.
- totalPage보다 큰 양수 page의 처리 정책은 아직 정하지 않았다. REST 오류 응답 정책과 함께 M2에서 결정한다.
- GitHub 연결 쓰기 API 특성상 파일별 commit으로 반영됐다. 이후 `recover`를 병합할 때 squash 여부를 결정한다.

## 다음 단계 판단

위 Gradle 검증이 통과하면 PRE-M2의 Service/Repository 페이지·검색 계약은 완료로 처리할 수 있다. 이후에는 M2 게시판 REST API 계약 설계로 진입할 수 있다.
