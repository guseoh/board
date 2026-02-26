package project.board.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String redirectUrl;

    public CustomException(ErrorCode errorCode, String redirectUrl) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.redirectUrl = redirectUrl;
    }

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.redirectUrl = errorCode.getRedirectUrl();
    }

}
