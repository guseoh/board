package project.board.global.api.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<ApiFieldError> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiErrorResponse of(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                path,
                List.of()
        );
    }

    public static ApiErrorResponse validation(String path, List<ApiFieldError> fieldErrors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "VALIDATION_ERROR",
                "요청값이 올바르지 않습니다.",
                path,
                fieldErrors
        );
    }
}

/*
    Instant
    - 서버의 로컬 시간만 반환할 수도 있다.
    - 하지만 LocalDateTime은 시간대 정보가 없다. ex) 2026-07-21T10:30:00
    - 한국 시간인지 UTC인지 클라이어늩가 알 수 없다.
    - Instant를 사용하면 2026-07-21T01:30:00Z UTC 기준 시각이 명확하다.

    fieldErrors를 배열로 반환하는 이유
    - 한 번의 요청에서 여러 필드의 검증이 동시에 실패할 수 있기 때문
    - ex) 회원가입
 */