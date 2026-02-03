package project.board.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    LOGIN_REQUIRED("/loginForm", "로그인이 필요합니다"),
    POST_NOT_FOUND("/", "해당 게시글이 존재하지 않습니다."),
    NOT_POST_OWNER("/", "본인 게시글만 수정/삭제 가능합니다");

    private final String redirectUrl;
    private final String message;

}
