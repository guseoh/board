package project.board.global.api.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import project.board.global.api.error.ApiErrorResponse;
import project.board.global.api.error.ApiFieldError;
import project.board.global.api.error.ApiErrorStatusMapper;
import project.board.global.exception.CustomException;
import project.board.post.controller.api.PostApiController;

import java.util.List;

@Slf4j
@RestControllerAdvice(basePackageClasses = PostApiController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomException(
            CustomException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = ApiErrorStatusMapper.from(exception.getErrorCode());

        ApiErrorResponse response = ApiErrorResponse.of(
                status,
                exception.getErrorCode().name(),
                exception.getMessage(),
                request.getRequestURI()
        );

        if (status.is5xxServerError()) {
            log.error(
                    "API 비즈니스 처리 중 서버 오류 - code={}, path={}",
                    exception.getErrorCode(),
                    request.getRequestURI(),
                    exception
            );
        } else {
            log.warn(
                    "API 요청 실패 - code={}, status={}, path={}",
                    exception.getErrorCode(),
                    status.value(),
                    request.getRequestURI()
            );
        }

        return ResponseEntity.status(status).body(response);
    }

    /*
     * MethodArgumentNotValidException도 BindException의 하위 타입이다.
     *
     * 따라서 @RequestBody DTO 검증과 @ModelAttribute DTO 검증의
     * BindingResult를 하나의 형식으로 변환할 수 있다.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
            BindException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldError> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ApiFieldError(
                                error.getField(),
                                error.getDefaultMessage() == null
                                        ? "올바르지 않은 값입니다."
                                        : error.getDefaultMessage()
                        ))
                        .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.validation(
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "요청 본문의 JSON 형식이 올바르지 않습니다.",
                request.getRequestURI()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT_TYPE",
                "요청 경로 또는 파라미터의 형식이 올바르지 않습니다.",
                request.getRequestURI()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "지원하지 않는 HTTP 메서드입니다.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "지원하지 않는 Content-Type입니다.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "처리되지 않은 API 예외 - path={}",
                request.getRequestURI(),
                exception
        );

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}

//todo: Enum 정리