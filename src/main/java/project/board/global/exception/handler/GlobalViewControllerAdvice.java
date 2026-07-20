package project.board.global.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Set;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalViewControllerAdvice {

    private static final Set<ErrorCode> DISCORD_NOTIFICATION_ERROR_CODES = Set.of();

    private final DiscordNotifier discordNotifier;

    @ExceptionHandler(CustomException.class)
    public String customException(CustomException e, RedirectAttributes ra, Model model, HttpServletRequest request) {
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
            MemberCreateRequest submitted = (MemberCreateRequest) model.asMap().get("form");
            MemberCreateRequest safeForm = new MemberCreateRequest();
            if (submitted != null) {
                safeForm.setNickname(submitted.getNickname());
                safeForm.setEmail(submitted.getEmail());
            } else {
                safeForm.setNickname(request.getParameter("nickname"));
                safeForm.setEmail(request.getParameter("email"));
            }
            model.addAttribute("form", safeForm);

            model.addAttribute("error", errorCode.getMessage());
            return "member/signup";
        }

        return "redirect:" + errorCode.getRedirectUrl();
    }

    private static boolean isShouldNotify(ErrorCode errorCode) {
        return DISCORD_NOTIFICATION_ERROR_CODES.contains(errorCode);
    }
}
