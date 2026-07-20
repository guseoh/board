# PRE-M2 운영 DB 스키마 계약 검증 보고서

## 1. 작업 기준

- 로드맵 위치: PRE-M2 마감
- 작업 브랜치: `recover`
- 시작 commit: `5b6206273a44f21f5cb510d5f27a22612b2c7f69`
- 검증 절차 작성 commit: `5cdc6dc04271c8a833925a361bb87424fecd5c79`
- 목적: 현재 JPA·Validation 계약과 실제 MySQL 스키마가 일치하는지 읽기 전용으로 검증
- 실제 검증 대상: 로컬 Docker MySQL `board` 데이터베이스
- 제외: REST API, React, ViewController·Thymeleaf 변경, Entity 변경, DDL·DML 실행, DB migration 적용

이번 검증은 사용자가 MySQL Client에서 직접 실행한 `SELECT`와 `information_schema` 조회 결과를 기준으로 한다. 확인된 host 값 `dea039c41add`는 Docker 컨테이너 식별자 형태이므로 운영 RDS가 아닌 로컬 개발 DB 결과로 판정한다.

운영 RDS는 접속 정보와 실행 결과가 확보되지 않아 아직 검증하지 않았다.

## 2. 코드 기준 기대 계약

세 Entity에는 `@Table`이 없으므로 물리 테이블명은 naming strategy에 의해 결정된다. 로컬 DB에서 실제 테이블명은 `member`, `post`, `comment`로 확인됐다.

### 공통 audit 컬럼

`BaseEntity` 기준으로 세 테이블은 다음 컬럼을 가진다.

| 논리 필드 | 물리 컬럼 | 코드 계약 |
| --- | --- | --- |
| `createdAt` | `created_at` | 생성 시 기록, nullable 명시 없음 |
| `createdBy` | `created_by` | `NOT NULL` |
| `updatedAt` | `updated_at` | 수정 시 기록, nullable 명시 없음 |
| `updatedBy` | `updated_by` | `NOT NULL` |

### `member`

| 컬럼 | 코드 계약 |
| --- | --- |
| `id` | PK, IDENTITY 자동 증가 |
| `nickname` | `NOT NULL`, 최대 100자 |
| `password` | `NOT NULL`, 길이 미지정 |
| `email` | `NOT NULL`, UNIQUE, 길이 미지정 |
| `role` | `NOT NULL`, 문자열 Enum |
| `provider` | nullable 허용 |
| `provider_id` | nullable 허용, DB UNIQUE 미지정 |
| `login_type` | 문자열 Enum, nullable 제한 미지정 |

### `post`

| 컬럼 | 코드 계약 |
| --- | --- |
| `id` | PK, IDENTITY 자동 증가 |
| `title` | `NOT NULL`, 최대 500자 |
| `content` | `NOT NULL`, 최대 500자 |
| `view_count` | Java primitive `int`, 신규 객체 기본값 0 |
| `member_id` | `NOT NULL`, `member.id` FK |

### `comment`

| 컬럼 | 코드 계약 |
| --- | --- |
| `id` | PK, IDENTITY 자동 증가 |
| `content` | `NOT NULL`, 최대 500자 |
| `member_id` | `NOT NULL`, `member.id` FK |
| `post_id` | `NOT NULL`, `post.id` FK |
| `parent_id` | nullable, `comment.id` 자기참조 FK |

Entity association에는 remove cascade와 orphanRemoval이 없다. 현재 Service가 자식 댓글을 먼저 물리 삭제하므로 FK의 `DELETE_RULE`은 `CASCADE`를 전제로 하지 않는다.

## 3. 실행 환경

| 항목 | 실제 값 |
| --- | --- |
| database | `board` |
| host | `dea039c41add` |
| MySQL | `8.0.33` |
| 배포판 | MySQL Community Server - GPL |
| character set | `utf8mb4` |
| collation | `utf8mb4_0900_ai_ci` |
| 검증 대상 구분 | 로컬 Docker MySQL |

최초 실행에서는 DB가 선택되지 않아 `DATABASE()`가 `NULL`이었고 모든 `information_schema` 결과가 비어 있었다. `USE board;`로 대상 DB를 명시한 뒤 결과를 다시 수집했다. DB 선택 전 결과는 계약 판정에 사용하지 않았다.

## 4. 실제 스키마 검증 결과

### 4.1. 테이블과 엔진

| 테이블 | 엔진 | collation | `TABLE_ROWS` 참고값 |
| --- | --- | --- | ---: |
| `comment` | InnoDB | `utf8mb4_0900_ai_ci` | 198,405 |
| `member` | InnoDB | `utf8mb4_0900_ai_ci` | 2 |
| `post` | InnoDB | `utf8mb4_0900_ai_ci` | 99,134 |

세 테이블이 모두 존재하고 트랜잭션과 FK를 지원하는 InnoDB를 사용한다. `information_schema.tables.TABLE_ROWS`는 InnoDB 통계 기반 참고값이므로 정확한 건수로 기록하지 않는다.

### 4.2. 핵심 컬럼 계약

| 컬럼 | 실제 DB | 코드 계약 | 판정 |
| --- | --- | --- | --- |
| `member.id` | `bigint`, PK, auto increment, NOT NULL | IDENTITY PK | 일치 |
| `member.nickname` | `varchar(100) NOT NULL` | 최대 100자, 필수 | 일치 |
| `member.password` | `varchar(255) NOT NULL` | 필수 | 일치 |
| `member.email` | `varchar(255) NOT NULL`, UNIQUE | 필수, UNIQUE | 일치 |
| `member.provider` | `varchar(255) NULL` | nullable | 일치 |
| `member.provider_id` | `varchar(255) NULL` | nullable | 일치 |
| `member.role` | `enum('ADMIN','USER') NOT NULL` | 문자열 Enum, 필수 | 일치 |
| `member.login_type` | `enum('LOCAL','SOCIAL') NULL` | 문자열 Enum, nullable 제한 미지정 | 일치 |
| `post.id` | `bigint`, PK, auto increment, NOT NULL | IDENTITY PK | 일치 |
| `post.title` | `varchar(500) NOT NULL` | 최대 500자, 필수 | 일치 |
| `post.content` | `varchar(500) NOT NULL` | 최대 500자, 필수 | 일치 |
| `post.view_count` | `int NOT NULL` | primitive `int` | 일치 |
| `post.member_id` | `bigint NOT NULL` | 필수 FK | 일치 |
| `comment.id` | `bigint`, PK, auto increment, NOT NULL | IDENTITY PK | 일치 |
| `comment.content` | `varchar(500) NOT NULL` | 최대 500자, 필수 | 일치 |
| `comment.member_id` | `bigint NOT NULL` | 필수 FK | 일치 |
| `comment.post_id` | `bigint NOT NULL` | 필수 FK | 일치 |
| `comment.parent_id` | `bigint NULL` | nullable 자기참조 FK | 일치 |

공통 audit 컬럼도 코드 선언과 일치했다.

- `created_at`, `updated_at`: `datetime(6) NULL`
- `created_by`, `updated_by`: `varchar(255) NOT NULL`

### 4.3. PK·UNIQUE·INDEX

확인된 인덱스는 다음과 같다.

| 테이블 | 인덱스 | 컬럼 | unique | 판정 |
| --- | --- | --- | --- | --- |
| `member` | PRIMARY | `id` | 예 | 일치 |
| `member` | `UKmbmcqelty0fbrvxp1q58dn57t` | `email` | 예 | 일치 |
| `post` | PRIMARY | `id` | 예 | 일치 |
| `post` | `FK83s99f4kx8oiqm3ro0sasmpww` | `member_id` | 아니오 | 일치 |
| `comment` | PRIMARY | `id` | 예 | 일치 |
| `comment` | `FKmrrrpi513ssu63i2783jyiv9m` | `member_id` | 아니오 | 일치 |
| `comment` | `FKs1slvnkuemjsq2kj4h3vhx7i1` | `post_id` | 아니오 | 일치 |
| `comment` | `FKde3rfu96lep00br5ov0mdieyt` | `parent_id` | 아니오 | 일치 |

`member.email`은 `NOT NULL`과 UNIQUE index가 함께 존재하므로 DB가 중복 이메일 저장을 차단한다. `member.nickname`과 `(provider, provider_id)`는 현재 코드 기준 DB UNIQUE 필수 계약이 아니며 이번 작업에서 추가하지 않는다.

### 4.4. FK와 삭제·갱신 규칙

| FK 컬럼 | 참조 대상 | UPDATE | DELETE | 판정 |
| --- | --- | --- | --- | --- |
| `post.member_id` | `member.id` | NO ACTION | NO ACTION | 일치 |
| `comment.member_id` | `member.id` | NO ACTION | NO ACTION | 일치 |
| `comment.post_id` | `post.id` | NO ACTION | NO ACTION | 일치 |
| `comment.parent_id` | `comment.id` | NO ACTION | NO ACTION | 일치 |

네 FK가 모두 의도한 테이블과 `id`를 참조한다. DB cascade는 없으며 현재 Service가 답글과 자식 데이터를 먼저 삭제하는 정책과 일치한다.

특히 자기참조 `comment.parent_id`가 `NO ACTION`이므로 부모 댓글보다 답글을 먼저 삭제하지 않으면 FK 위반이 발생한다. PRE-M2 삭제 무결성 작업에서 적용한 답글 선삭제 순서는 실제 로컬 DB 제약과 부합한다.

## 5. 실제 데이터 계약 검증

### 5.1. 문자열 길이와 공백

| 항목 | 최대 실제 길이 | 빈 문자열·공백 count | 제한 | 판정 |
| --- | ---: | ---: | ---: | --- |
| `post.title` | 14 | 0 | 500 | 통과 |
| `post.content` | 25 | 0 | 500 | 통과 |
| `comment.content` | 30 | 0 | 500 | 통과 |
| `member.nickname` | 5 | 0 | 100 | 통과 |

컬럼이 `NOT NULL`이고 최대 길이가 실제 `varchar` 제한보다 작으므로 NULL·길이 초과 데이터는 존재할 수 없다. 애플리케이션의 `isBlank()` 계약과 대조하기 위해 확인한 공백 데이터도 0건이다.

`member.login_type IS NULL`도 0건으로 확인됐다. DB는 nullable을 허용하지만 현재 로컬 데이터에는 로그인 유형이 누락된 회원이 없다.

### 5.2. 댓글 구조 규칙

| 검사 | 결과 | 기대 | 판정 |
| --- | ---: | ---: | --- |
| 부모와 다른 게시글을 참조하는 답글 | 0 | 0 | 통과 |
| 2단계보다 깊은 답글 | 0 | 0 | 통과 |

현재 Service의 다음 도메인 계약과 실제 데이터가 일치한다.

- 답글의 부모 댓글은 같은 게시글에 속한다.
- 부모 댓글은 root 댓글이어야 한다.
- 답글 깊이는 1단계로 제한한다.

### 5.3. 고아 데이터

| 검사 | 결과 | 기대 | 판정 |
| --- | ---: | ---: | --- |
| 존재하지 않는 회원을 참조하는 게시글 | 0 | 0 | 통과 |
| 존재하지 않는 회원을 참조하는 댓글 | 0 | 0 | 통과 |
| 존재하지 않는 게시글을 참조하는 댓글 | 0 | 0 | 통과 |
| 존재하지 않는 부모를 참조하는 답글 | 0 | 0 | 통과 |

모든 연관관계에서 고아 데이터가 발견되지 않았다.

## 6. 최종 판정

### 로컬 Docker MySQL

**통과**

다음을 모두 확인했다.

- `member`, `post`, `comment` 테이블과 필수 컬럼 존재
- 핵심 문자열 길이와 `NOT NULL` 계약 일치
- PK·auto increment와 `member.email` UNIQUE 존재
- 네 FK가 의도한 대상 참조
- FK `DELETE_RULE`이 모두 `NO ACTION`이며 현재 수동 삭제 순서와 일치
- 실제 데이터에 공백 문자열, 잘못된 답글 관계, 2단계 이상 답글, 고아 데이터 없음
- 현재 회원 데이터에 `login_type` NULL 없음

로컬 DB에는 이번 계약을 맞추기 위한 migration이 필요하지 않다.

### 운영 RDS

**미검증**

이번 실행 환경은 Docker 컨테이너였으므로 로컬 통과 결과를 운영 RDS에 그대로 적용할 수 없다. 운영 RDS에서도 같은 읽기 전용 조회를 수행한 뒤 별도로 판정해야 한다.

## 7. 남은 위험과 후속 판단

- `spring.jpa.hibernate.ddl-auto=update`는 명시적인 schema 변경 이력을 제공하지 않는다.
- `role`, `login_type`이 MySQL `ENUM`으로 생성돼 있어 Java Enum 값 추가 시 DB 정의도 함께 검토해야 한다.
- `login_type`은 DB에서 nullable이므로 향후 계약을 필수로 강화하려면 기존 데이터 확인과 migration이 필요하다.
- 운영 RDS의 실제 스키마·FK·데이터 상태는 아직 확인하지 않았다.
- 검색 인덱스와 쿼리 성능 최적화는 M4에서 측정 근거를 바탕으로 진행하며 이번 계약 검증 범위에 포함하지 않는다.

## 8. 재실행용 읽기 전용 SQL

운영 RDS 검증 시 아래 항목을 같은 순서로 실행한다.

```sql
SELECT
    DATABASE() AS database_name,
    @@hostname AS db_host,
    @@version AS mysql_version,
    @@version_comment AS version_comment,
    @@character_set_database AS database_charset,
    @@collation_database AS database_collation;

SELECT
    table_name,
    engine,
    table_collation,
    table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND LOWER(table_name) IN ('member', 'post', 'comment')
ORDER BY table_name;

SELECT
    table_name,
    ordinal_position,
    column_name,
    column_type,
    data_type,
    is_nullable,
    column_default,
    character_maximum_length,
    collation_name,
    column_key,
    extra
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND LOWER(table_name) IN ('member', 'post', 'comment')
ORDER BY table_name, ordinal_position;

SELECT
    table_name,
    index_name,
    non_unique,
    seq_in_index,
    column_name,
    index_type
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND LOWER(table_name) IN ('member', 'post', 'comment')
ORDER BY table_name, index_name, seq_in_index;

SELECT
    kcu.table_name,
    kcu.constraint_name,
    kcu.column_name,
    kcu.referenced_table_name,
    kcu.referenced_column_name,
    rc.update_rule,
    rc.delete_rule
FROM information_schema.key_column_usage kcu
JOIN information_schema.referential_constraints rc
  ON rc.constraint_schema = kcu.constraint_schema
 AND rc.constraint_name = kcu.constraint_name
WHERE kcu.constraint_schema = DATABASE()
  AND LOWER(kcu.table_name) IN ('post', 'comment')
  AND kcu.referenced_table_name IS NOT NULL
ORDER BY kcu.table_name, kcu.column_name;
```

모든 쿼리는 조회 전용이다. 불일치가 확인돼도 이 검증 작업에서는 `ALTER TABLE`, 데이터 수정, `ddl-auto` 변경을 수행하지 않는다. migration은 원인과 실제 데이터 영향을 기록한 뒤 독립 작업으로 설계한다.

## 9. 검증 상태

| 단계 | 상태 |
| --- | --- |
| Entity·BaseEntity 코드 계약 확인 | 완료 |
| 공통 JPA 설정 확인 | 완료: `spring.jpa.hibernate.ddl-auto=update` |
| 읽기 전용 SQL 작성 | 완료 |
| 로컬 Docker MySQL 실행 | 완료 |
| 로컬 코드·스키마·데이터 대조 | 통과 |
| 로컬 migration 필요 여부 | 불필요 |
| 운영 RDS 실행 | 미실행 |
| 운영 RDS 최종 판정 | 대기 |
