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
    POST_TITLE_TOO_LONG("/", "제목은 500자 이하여야 합니다."),
    POST_CONTENT_TOO_LONG("/", "내용은 500자 이하여야 합니다."),
    POST_NOT_MEMBER("/", "작성자는 필수입니다."),
    POST_WRITER_CANNOT_CHANGE("/", "게시글 작성자는 변경할 수 없습니다."),

    // member
    MEMBER_NOT_FOUND("/", "해당 사용자가 존재하지 않습니다."),
    DUPLICATE_EMAIL("/signup", "이미 사용중인 이메일입니다."),
    DUPLICATE_NICKNAME("/signup", "이미 사용중인 닉네임입니다."),
    PASSWORD_MISMATCH("/signup", "비밀번호가 일치해야합니다."),
    MEMBER_NOT_AUTHENTICATION("/signup", "인증 정보가 일치하지 않습니다."),
    PASSWORD_CONFIRM("/my/edit", "새로운 비밀번호가 일치하지 않습니다."),
    PASSWORD_CURRENT_REQUIRED("/my/edit", "비밀번호를 입력해야 합니다."),
    PASSWORD_INVALID("/my/edit", "비밀번호가 일치하지 않습니다."),
    SOCIAL_PASSWORD_CHANGE_NOT_ALLOWED("/my/edit", "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),
    WITHDRAW_CONFIRMATION_MISMATCH("/my/withdraw", "회원 탈퇴 확인 문구가 일치하지 않습니다."),

    // comment
    COMMENT_NOT_FOUND("/", "해당 댓글은 존재하지 않습니다."),
    COMMENT_NOT_OWNER("/", "해당 댓글 작성자가 아닙니다."),
    COMMENT_NOT_CONTENT("/", "댓글 내용은 필수입니다."),
    COMMENT_CONTENT_TOO_LONG("/", "댓글 내용은 500자 이하여야 합니다."),
    COMMENT_INVALID_PARENT("/", "부모 댓글이 현재 게시글의 댓글이 아닙니다."),
    REPLY_DEPTH_NOT_ALLOWED("/", "대댓글의 대댓글은 안됩니다.");



    private final String redirectUrl;
    private final String message;

}
