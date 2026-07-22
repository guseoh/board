package project.board.global.api.error;

import org.springframework.http.HttpStatus;
import project.board.global.exception.ErrorCode;

public final class ApiErrorStatusMapper {

    private ApiErrorStatusMapper() {
    }

    public static HttpStatus from(ErrorCode errorCode) {
        return switch (errorCode) {
            case LOGIN_REQUIRED ->
                    HttpStatus.UNAUTHORIZED;

            case POST_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;

            case NOT_POST_OWNER ->
                    HttpStatus.FORBIDDEN;

            case POST_NOT_TITLE,
                 POST_NOT_CONTENT,
                 POST_TITLE_TOO_LONG,
                 POST_CONTENT_TOO_LONG ->
                    HttpStatus.BAD_REQUEST;

            case POST_WRITER_CANNOT_CHANGE ->
                    HttpStatus.CONFLICT;

            /*
             * 정상적인 게시글 API 흐름에서는 작성자를 요청으로 받지 않고
             * 인증 Principal에서 가져온다.
             *
             * 따라서 POST_NOT_MEMBER가 발생한다면 클라이언트 입력 오류보다는
             * 서버 내부 연결 오류나 잘못된 Service 호출일 가능성이 높다.
             */
            case POST_NOT_MEMBER ->
                    HttpStatus.INTERNAL_SERVER_ERROR;

            /*
             * 아직 API 계약을 설계하지 않은 회원·댓글 오류는
             * 임의로 status를 확정하지 않는다.
             */
            default ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}