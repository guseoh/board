package project.board.global.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import project.board.global.security.user.CustomUserDetails;

import java.util.Optional;

@Configuration
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(p -> p instanceof CustomUserDetails)
                .map(p -> (CustomUserDetails) p)
                .map(CustomUserDetails::getNickname)
                .filter(nickname -> nickname != null && !nickname.isBlank())
                .or(() -> Optional.of("system"));
    }
}
