package project.board.global.api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import project.board.global.exception.ErrorCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorModelTest {

    @Test
    @DisplayName("게시글 API ErrorCode를 계약된 HTTP 상태로 변환한다")
    void mapsPostErrorCodesToHttpStatus() {
        assertThat(ApiErrorStatusMapper.from(ErrorCode.LOGIN_REQUIRED))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.POST_NOT_FOUND))
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.NOT_POST_OWNER))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.POST_NOT_TITLE))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.POST_NOT_CONTENT))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.POST_TITLE_TOO_LONG))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.POST_CONTENT_TOO_LONG))
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.POST_WRITER_CANNOT_CHANGE))
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ApiErrorStatusMapper.from(ErrorCode.POST_NOT_MEMBER))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("아직 API 계약을 정하지 않은 ErrorCode는 안전하게 500으로 처리한다")
    void unmappedErrorCodeFallsBackToInternalServerError() {
        assertThat(ApiErrorStatusMapper.from(ErrorCode.COMMENT_NOT_FOUND))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("일반 API 오류 응답은 fieldErrors를 빈 배열로 제공한다")
    void normalErrorResponseHasEmptyFieldErrors() {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND,
                "POST_NOT_FOUND",
                "해당 게시글이 존재하지 않습니다.",
                "/api/posts/999"
        );

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.code()).isEqualTo("POST_NOT_FOUND");
        assertThat(response.fieldErrors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("fieldErrors가 null이면 빈 불변 목록으로 정규화한다")
    void nullFieldErrorsAreNormalizedToEmptyList() {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.EPOCH,
                400,
                "VALIDATION_ERROR",
                "요청값이 올바르지 않습니다.",
                "/api/posts",
                null
        );

        assertThat(response.fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("API 오류 응답은 전달받은 fieldErrors 목록을 방어적으로 복사한다")
    void fieldErrorsAreDefensivelyCopied() {
        List<ApiFieldError> source = new ArrayList<>();
        source.add(new ApiFieldError("title", "제목은 필수입니다."));

        ApiErrorResponse response = ApiErrorResponse.validation(
                "/api/posts",
                source
        );

        source.add(new ApiFieldError("content", "내용은 필수입니다."));

        assertThat(response.fieldErrors())
                .containsExactly(new ApiFieldError("title", "제목은 필수입니다."));
    }
}
