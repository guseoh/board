package project.board.global.api.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import project.board.global.api.error.ApiErrorResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {

    private final JsonMapper jsonMapper;

    /*
        전달받은 ApiErrorResponse 객체를 JSON으로 변환하여 HTTP 응답에 기록한다.
     */
    public void write(
            HttpServletResponse response,
            HttpStatus status,
            ApiErrorResponse body
    ) throws IOException {

        // HTTP 응답이 이미 클라이언트에게 전송되기 시작했는지를 확인한다.
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        jsonMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}