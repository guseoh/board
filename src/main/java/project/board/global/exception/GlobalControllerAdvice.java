package project.board.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.member.dto.request.MemberCreateRequest;

@ControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

    @ExceptionHandler(CustomException.class)
    public String customException(CustomException e, RedirectAttributes ra, Model model) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("Exception:  msg= {}", errorCode.getMessage());

        ra.addFlashAttribute("msg", e.getMessage());

        if (errorCode == ErrorCode.DUPLICATE_EMAIL || errorCode == ErrorCode.DUPLICATE_NICKNAME || errorCode == ErrorCode.PASSWORD_MISMATCH) {

            model.addAttribute("form", new MemberCreateRequest());

            model.addAttribute("error", errorCode.getMessage());
            return "member/signup";
        }

        return "redirect:" + errorCode.getRedirectUrl();
    }
}
