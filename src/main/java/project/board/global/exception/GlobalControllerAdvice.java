package project.board.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

    @ExceptionHandler(CustomException.class)
    public String customException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Exception:  msg= {} ", errorCode.getMessage());

        return "redirect:" + errorCode.getRedirectUrl();
    }
}
