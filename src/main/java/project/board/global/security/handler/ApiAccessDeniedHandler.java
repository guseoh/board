package project.board.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;
import project.board.global.api.error.ApiErrorResponse;
import project.board.global.api.handler.ApiErrorResponseWriter;

import java.io.IOException;

/**
 * 인증은 완료되었으니 해당 엔드포인트에 접근할 권한이 없다면, 403 Forbidden 오류가 발생
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorResponseWriter responseWriter;

    /*
        처리기 작동 이후 어떤 형식으로 반환할지를 정한다.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String code;
        String message;

        if (accessDeniedException instanceof MissingCsrfTokenException) {
            code = "CSRF_TOKEN_MISSING";
            message = "CSRF 토큰이 필요합니다";
        } else if (accessDeniedException instanceof InvalidCsrfTokenException) {
            code = "CSRF_TOKEN_INVALID";
            message = "CSRF 토큰이 올바르지 않습니다.";
        } else {
            code = "ACCESS_DENIED";
            message = "요청을 수행할 권한이 없습니다.";
        }

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN,
                code,
                message,
                request.getRequestURI()
        );

        responseWriter.write(
                response,
                HttpStatus.FORBIDDEN,
                body
        );
    }
}


//todo: instanceof, 처리기