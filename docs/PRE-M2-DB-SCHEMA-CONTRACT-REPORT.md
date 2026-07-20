# PRE-M2 운영 DB 스키마 계약 검증 보고서

## 1. 작업 기준

- 로드맵 위치: PRE-M2 마감
- 작업 브랜치: `recover`
- 시작 commit: `5b6206273a44f21f5cb510d5f27a22612b2c7f69`
- 목적: 현재 JPA·Validation 계약과 실제 MySQL 스키마가 일치하는지 읽기 전용으로 검증
- 변경 대상: 검증 문서만 추가
- 제외: REST API, React, ViewController·Thymeleaf 변경, Entity 변경, DDL·DML 실행, DB migration 적용

현재 ChatGPT 실행 환경에는 운영 RDS 접속 정보와 사용자의 로컬 MySQL 세션이 없으므로 실제 DB 조회는 아직 수행하지 않았다. 이 문서는 저장소 코드에서 기대 계약을 확정하고, 사용자가 운영 또는 개발 MySQL에서 실행할 읽기 전용 SQL과 판정 기준을 제공한다.

## 2. 코드 기준 기대 계약

세 Entity에는 `@Table`이 없으므로 물리 테이블명은 naming strategy에 의해 결정된다. 현재 문서와 일반적인 Spring Boot naming strategy 기준 이름은 `member`, `post`, `comment`이지만 실제 DB 대소문자와 이름은 조회 결과로 확정한다.

### 공통 audit 컬럼

`BaseEntity` 기준으로 모든 Entity 테이블은 다음 필드를 가진다.

| 논리 필드 | 일반적인 물리 컬럼 | 코드 계약 |
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

Entity association에는 remove cascade와 orphanRemoval이 없다. 현재 Service가 자식 댓글을 먼저 물리 삭제하므로 FK의 `DELETE_RULE`은 `CASCADE`를 전제로 하지 않는다. 실제 규칙은 조회 결과로 확정한다.

## 3. 접속 방법

비밀번호를 명령줄 인자로 직접 남기지 않고 MySQL Client의 password prompt를 사용한다.

```powershell
mysql `
  --host=$env:BOARD_DB_HOST `
  --port=3306 `
  --user=$env:BOARD_DB_USER `
  --password `
  --database=$env:BOARD_DB_NAME
```

환경 변수를 사용하지 않는 경우 host, user, database 값만 직접 입력하고 password 값은 `--password` 뒤에 붙이지 않는다.

MySQL Workbench를 사용해도 되며, 아래 SQL은 모두 조회 전용이다.

## 4. 읽기 전용 검증 SQL

### 4.1. 현재 접속 대상과 DB 버전

```sql
SELECT
    DATABASE() AS database_name,
    @@hostname AS db_host,
    @@version AS mysql_version,
    @@version_comment AS version_comment,
    @@character_set_database AS database_charset,
    @@collation_database AS database_collation;
```

`database_name`이 의도한 운영 또는 개발 DB인지 먼저 확인한다. 값이 `NULL`이면 DB를 선택하지 않은 상태이므로 이후 결과를 신뢰하지 않는다.

### 4.2. 실제 테이블 이름과 엔진

```sql
SELECT
    table_name,
    engine,
    table_collation,
    table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND LOWER(table_name) IN ('member', 'post', 'comment')
ORDER BY table_name;
```

세 테이블이 모두 존재해야 한다. FK 검증을 위해 일반적으로 InnoDB여야 한다.

### 4.3. 전체 컬럼 타입·길이·NULL·기본값

```sql
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
```

특히 다음 값을 확인한다.

```text
post.title       character_maximum_length = 500, is_nullable = NO
post.content     character_maximum_length = 500, is_nullable = NO
comment.content character_maximum_length = 500, is_nullable = NO
member.nickname character_maximum_length = 100, is_nullable = NO
member.email    is_nullable = NO
post.member_id  is_nullable = NO
comment.member_id is_nullable = NO
comment.post_id   is_nullable = NO
comment.parent_id is_nullable = YES
```

### 4.4. PK·UNIQUE·INDEX

```sql
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
```

판정 기준:

- 각 테이블 `id`에 PRIMARY KEY 존재
- `member.email`에 단일 또는 복합 UNIQUE index 존재
- FK 컬럼의 index 존재 여부 확인
- `member.nickname`, `(provider, provider_id)`는 현재 코드상 DB UNIQUE 필수 계약이 아님

### 4.5. FK와 삭제·갱신 규칙

```sql
SELECT
    kcu.constraint_name,
    kcu.table_name,
    kcu.column_name,
    kcu.referenced_table_name,
    kcu.referenced_column_name,
    rc.update_rule,
    rc.delete_rule
FROM information_schema.key_column_usage kcu
LEFT JOIN information_schema.referential_constraints rc
       ON rc.constraint_schema = kcu.constraint_schema
      AND rc.constraint_name = kcu.constraint_name
      AND rc.table_name = kcu.table_name
WHERE kcu.constraint_schema = DATABASE()
  AND LOWER(kcu.table_name) IN ('post', 'comment')
  AND kcu.referenced_table_name IS NOT NULL
ORDER BY kcu.table_name, kcu.column_name;
```

반드시 확인할 관계:

```text
post.member_id     -> member.id
comment.member_id  -> member.id
comment.post_id    -> post.id
comment.parent_id  -> comment.id
```

`comment.parent_id` 자기참조 FK가 실제로 존재하는지와 `delete_rule`을 정확히 기록한다. 현재 삭제 Service는 답글을 부모보다 먼저 지우므로 DB `ON DELETE CASCADE`에 의존하지 않는다.

### 4.6. 실제 저장 데이터의 길이·NULL 위반 여부

```sql
SELECT
    'post.title' AS contract,
    COUNT(*) AS row_count,
    MAX(CHAR_LENGTH(title)) AS max_length,
    SUM(title IS NULL) AS null_count,
    SUM(CHAR_LENGTH(title) > 500) AS over_limit_count
FROM `post`

UNION ALL

SELECT
    'post.content',
    COUNT(*),
    MAX(CHAR_LENGTH(content)),
    SUM(content IS NULL),
    SUM(CHAR_LENGTH(content) > 500)
FROM `post`

UNION ALL

SELECT
    'comment.content',
    COUNT(*),
    MAX(CHAR_LENGTH(content)),
    SUM(content IS NULL),
    SUM(CHAR_LENGTH(content) > 500)
FROM `comment`

UNION ALL

SELECT
    'member.nickname',
    COUNT(*),
    MAX(CHAR_LENGTH(nickname)),
    SUM(nickname IS NULL),
    SUM(CHAR_LENGTH(nickname) > 100)
FROM `member`;
```

모든 `null_count`와 `over_limit_count`는 0이어야 한다. 빈 테이블에서는 `max_length`가 `NULL`일 수 있으며 이는 오류가 아니다.

### 4.7. email 중복 여부

```sql
SELECT COUNT(*) AS duplicated_email_group_count
FROM (
    SELECT email
    FROM `member`
    GROUP BY email
    HAVING COUNT(*) > 1
) duplicated_email;
```

결과는 0이어야 한다.

### 4.8. 원본 DDL 보조 확인

```sql
SHOW CREATE TABLE `member`;
SHOW CREATE TABLE `post`;
SHOW CREATE TABLE `comment`;
```

`SHOW CREATE TABLE`은 위 information_schema 결과에서 불일치나 해석이 필요한 경우 보조 근거로 사용한다.

## 5. 판정 기준

### 일치

다음을 모두 만족하면 운영 DB 스키마 계약이 현재 코드와 일치한다고 판정한다.

- 세 테이블과 필수 컬럼 존재
- `post.title`, `post.content`, `comment.content`가 500자이며 `NOT NULL`
- `member.nickname`이 100자이며 `NOT NULL`
- PK·IDENTITY와 `member.email` UNIQUE 존재
- 네 개의 FK가 의도한 테이블·컬럼을 참조
- 필수 FK는 `NOT NULL`, `parent_id`는 nullable
- 실제 데이터에 NULL·길이 초과·email 중복 없음

### 불일치

다음 중 하나라도 확인되면 별도 migration 작업이 필요하다.

- 컬럼 길이가 코드 계약보다 작거나 다름
- nullable 계약 불일치
- PK, UNIQUE 또는 FK 누락
- FK가 다른 컬럼을 참조
- 실제 데이터가 신규 제약을 위반
- 운영 DB에만 존재하는 수동 변경으로 코드와 schema가 갈라짐

불일치가 확인돼도 이번 검증 작업에서는 `ALTER TABLE`, 데이터 정리, `ddl-auto` 변경을 수행하지 않는다. 실제 데이터 분포와 영향 범위를 먼저 기록하고 migration을 독립 작업으로 설계한다.

## 6. 현재 검증 상태

| 단계 | 상태 |
| --- | --- |
| Entity·BaseEntity 코드 계약 확인 | 완료 |
| 공통 JPA 설정 확인 | 완료: `spring.jpa.hibernate.ddl-auto=update` |
| 읽기 전용 SQL 작성 | 완료 |
| 실제 운영 또는 개발 MySQL 실행 | 미실행 |
| 코드와 실제 스키마 대조 | 대기 |
| migration 필요 여부 결정 | 대기 |

`ddl-auto=update`는 명시적인 migration 이력을 제공하지 않으므로 실제 조회를 대체하지 못한다. 위 SQL 결과를 확보한 뒤 이 보고서에 실제 테이블·컬럼·제약 결과와 최종 판정을 추가한다.
