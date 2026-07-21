package project.board.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import project.board.global.api.error.ApiErrorResponse;
import project.board.global.api.handler.ApiErrorResponseWriter;

import java.io.IOException;

/**
 * 인증이 안된 익명의 사용자가 인증이 필요한 엔드포인트로 접근한다면 401 UNAUTHORIZED 오류 발생
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorResponseWriter responseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "로그인이 필요합니다.",
                request.getRequestURI()
        );

        responseWriter.write(
                response, HttpStatus.UNAUTHORIZED, body
        );
    }
}

//todo: AuthenticationEntryPoint