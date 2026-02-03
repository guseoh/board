package project.board.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

    @ExceptionHandler(CustomException.class)
    public String customException(CustomException e, RedirectAttributes ra) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Exception:  msg= {}", errorCode.getMessage());

        ra.addFlashAttribute("msg", e.getMessage());

        return "redirect:" + errorCode.getRedirectUrl();
    }
}
