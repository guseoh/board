# Board 문서 인덱스

> 기준: 로컬 `recover` 브랜치, commit `d333e3868e5bb94073030780ce0910a65b3ef4d8` (`d333e38`), 2026-07-13 조사

이 문서 세트는 현재 Spring Boot MVC·Thymeleaf SSR 게시판의 실제 코드, 테스트, 설정, View, 운영 구성을 기록한 M1 As-Is 결과물이다. 로컬 `recover`를 `origin/recover`와 fast-forward 동기화한 위 HEAD를 기준으로 한다.

## 판단 우선순위

1. 현재 사용자 지시
2. 현재 로컬 코드와 테스트
3. 실행 및 설정 확인 결과
4. 기존 문서
5. 과거 계획

## 현재 구현 문서

| 문서 | 역할 |
| --- | --- |
| [PROJECT-OVERVIEW.md](PROJECT-OVERVIEW.md) | 목적, 사용자, 기능, 기술 스택과 실행 범위 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 요청 흐름, 계층 책임, 트랜잭션과 기술 부채 |
| [PACKAGE-STRUCTURE.md](PACKAGE-STRUCTURE.md) | 실제 패키지 트리, 대표 클래스와 의존 관계 |
| [REQUIREMENTS.md](REQUIREMENTS.md) | 현재 구현된 기능의 요구사항과 제한 |
| [API.md](API.md) | SSR HTTP Routing / Form Contract |
| [ERD.md](ERD.md) | Entity 필드와 관계 정보(그림·DDL·DBML 제외) |
| [SECURITY.md](SECURITY.md) | Session, form login, OAuth2, 권한과 보안 제한 |
| [TESTING-AND-OPERATIONS.md](TESTING-AND-OPERATIONS.md) | 테스트, 실행, Actuator, 로깅, CI/CD와 Docker |
| [M0-RECOVERY-REPORT.md](M0-RECOVERY-REPORT.md) | M0 코드·테스트·CI 기준선 복구 결과 |
| [M1-AS-IS-REPORT.md](M1-AS-IS-REPORT.md) | 조사 기준, 불일치, 검증 결과와 인계 사항 |

## 미래 계획 문서

| 문서 | 역할 |
| --- | --- |
| [FUTURE_FEATURES.md](FUTURE_FEATURES.md) | 상세 설계가 아닌 후속 단계 로드맵 |

## M0와 M1의 관계

M0는 테스트 컴파일, 마이페이지 조회·통계, 회원가입/OAuth 오류 처리와 CI·Docker 검증 기준선을 복구했다. M1은 그 복구가 반영된 현재 코드를 As-Is로 설명한다. M0의 당시 실행 이력과 M1의 현재 계약은 각각의 문서에서 분리해 관리한다.

## 현재와 미래의 경계

현재 활성 애플리케이션은 HTML View/Redirect를 반환하는 Session 기반 SSR 애플리케이션이다. 빈 `PostApiController`는 Spring Bean도 Mapping 보유 클래스도 아니므로 활성 JSON REST API로 보지 않는다. REST 계약, React 구조와 풋살 매칭·예약 도메인은 이 문서 세트의 현재 구현 범위가 아니다.
