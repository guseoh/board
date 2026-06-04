package project.board.global.exception.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.global.notification.discord.DiscordNotifier;
import project.board.member.dto.request.MemberCreateRequest;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalViewControllerAdvice {

    private final DiscordNotifier discordNotifier;

    @ExceptionHandler(CustomException.class)
    public String customException(CustomException e, RedirectAttributes ra, Model model) {
        ErrorCode errorCode = e.getErrorCode();

        // Discord 알림은 중요한 예외만 전송
        if (isShouldNotify(errorCode)) {
            discordNotifier.send("""
                    [Board 예외 발생]
                    
                    ErrorCode: %s
                    Message: %s
                    RedirectUrl: %s
                    """.formatted(
                    errorCode.name(),
                    errorCode.getMessage(),
                    e.getRedirectUrl()
            ));
        }

        log.warn("Exception:  msg= {}", errorCode.getMessage());

        ra.addFlashAttribute("msg", e.getMessage());

        if (errorCode == ErrorCode.DUPLICATE_EMAIL || errorCode == ErrorCode.DUPLICATE_NICKNAME || errorCode == ErrorCode.PASSWORD_MISMATCH) {

            model.addAttribute("form", new MemberCreateRequest());

            model.addAttribute("error", errorCode.getMessage());
            return "member/signup";
        }

        return "redirect:" + errorCode.getRedirectUrl();
    }

    private static boolean isShouldNotify(ErrorCode errorCode) {
        return errorCode != ErrorCode.DUPLICATE_EMAIL
                && errorCode != ErrorCode.DUPLICATE_NICKNAME
                && errorCode != ErrorCode.PASSWORD_MISMATCH;
    }
}