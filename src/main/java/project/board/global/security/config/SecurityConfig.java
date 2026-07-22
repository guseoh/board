package project.board.global.security.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import project.board.global.security.handler.ApiAccessDeniedHandler;
import project.board.global.security.handler.ApiAuthenticationEntryPoint;
import project.board.global.security.handler.CustomLoginSuccessHandler;
import project.board.global.security.oauth.CustomOauth2UserService;
import project.board.global.security.user.CustomUserDetailsService;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] WHITE_LIST = {
            "/",
            "/login",
            "/loginForm",
            "/signup",
            "/css/**",
            "/js/**",
            "/images/**",
            "/error"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            PasswordEncoder passwordEncoder,
            CustomUserDetailsService customUserDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider provider,
                                           CustomOauth2UserService customOauth2UserService,
                                           CustomLoginSuccessHandler successHandler,
                                           ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
                                           ApiAccessDeniedHandler apiAccessDeniedHandler) throws Exception {

        RequestMatcher apiRequestMatcher = pathPattern("/api/**");
        RequestMatcher viewRequestMatcher = new NegatedRequestMatcher(apiRequestMatcher);

        LoginUrlAuthenticationEntryPoint viewAuthenticationEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/loginForm");
        viewAuthenticationEntryPoint.setFavorRelativeUris(true);

        http
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/post/*/edit").authenticated()
                        .requestMatchers(HttpMethod.POST, "/post/**").authenticated()
                        .requestMatchers("/my/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint,
                                apiRequestMatcher
                        )
                        .defaultAuthenticationEntryPointFor(
                                viewAuthenticationEntryPoint,
                                viewRequestMatcher
                        )
                        .defaultAccessDeniedHandlerFor(
                                apiAccessDeniedHandler,
                                apiRequestMatcher
                        )
                )
                .formLogin(form -> form
                        .loginPage("/loginForm")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureUrl("/loginForm?error=true"
                        ).permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/loginForm")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(
                                        customOauth2UserService
                                )
                        )
                        .defaultSuccessUrl("/")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();


    }

}


//todo: CSRF