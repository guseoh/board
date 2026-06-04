package project.board.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import project.board.global.security.principal.UnifiedPrincipal;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 로그인한 사용자 정보 가져오기
        UnifiedPrincipal user = (UnifiedPrincipal) authentication.getPrincipal();

        if (user.getRole().equals("ROLE_ADMIN")) {
            response.sendRedirect("/admin");
        } else {
            response.sendRedirect("/");
        }

    }
}
