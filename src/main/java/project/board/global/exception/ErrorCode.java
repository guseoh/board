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
    POST_NOT_TITLE("/", "제목은 필수입니다."),
    POST_NOT_CONTENT("/", "내용은 필수입니다."),
    POST_NOT_MEMBER("/", "작성자는 필수입니다."),
    POST_WRITER_CANNOT_CHANGE("/", "게시글 작성자는 변경할 수 없습니다."),

    // member
    MEMBER_NOT_FOUND("/", "해당 사용자가 존재하지 않습니다."),
    DUPLICATE_EMAIL("/signup", "이미 사용중인 이메일입니다."),
    DUPLICATE_NICKNAME("/signup", "이미 사용중인 닉네임입니다."),
    PASSWORD_MISMATCH("/signup", "비밀번호가 일치해야합니다."),

    // comment
    COMMENT_NOT_FOUND("/", "해당 댓글은 존재하지 않습니다."),
    COMMENT_NOT_OWNER("/", "해당 댓글 작성자가 아닙니다.");

    private final String redirectUrl;
    private final String message;

}
