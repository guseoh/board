package project.board.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // post
    LOGIN_REQUIRED("/loginForm", "로그인이 필요합니다"),
    POST_NOT_FOUND("/", "해당 게시글이 존재하지 않습니다."),
    NOT_POST_OWNER("/", "본인 게시글만 수정/삭제 가능합니다"),

    // member
    MEMBER_NOT_FOUND("/", "해당 사용자가 존재하지 않습니다."),
    DUPLICATE_EMAIL("/signup", "이미 사용중인 이메일입니다."),
    DUPLICATE_NICKNAME("/signup", "이미 사용중인 닉네임입니다.");

    private final String redirectUrl;
    private final String message;

}
