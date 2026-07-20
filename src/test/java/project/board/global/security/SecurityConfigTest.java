package project.board.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import project.board.global.security.handler.CustomLoginSuccessHandler;
import project.board.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static project.board.testsupport.TestFixtures.principal;

@SpringBootTest(properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("익명 사용자는 마이페이지와 게시글 작성에서 리다이렉트된다")
    void anonymousAccessIsRestricted() throws Exception {
        mockMvc.perform(get("/my"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));

        mockMvc.perform(post("/post/new").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));

        mockMvc.perform(get("/post/10/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));

        mockMvc.perform(get("/post/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("인증 사용자의 게시글 수정 GET은 Security 단계에서 통과한다")
    void authenticatedEditGetPassesSecurity() throws Exception {
        mockMvc.perform(get("/post/10/edit").with(authentication(authToken(Role.USER))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("관리자 경로는 일반 사용자를 거부하고 관리자를 허용한다")
    void adminAccessControl() throws Exception {
        mockMvc.perform(get("/admin").with(authentication(authToken(Role.USER))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin").with(authentication(authToken(Role.ADMIN))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/users/1/role")
                        .with(authentication(authToken(Role.USER)))
                        .with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("보안이 적용된 쓰기 요청에는 위조 방지 토큰이 필요하다")
    void csrfRequiredForPost() throws Exception {
        mockMvc.perform(post("/post/new")
                        .with(authentication(authToken(Role.USER)))
                        .param("title", "title")
                        .param("content", "content"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Actuator는 익명 health만 노출하고 상세와 다른 endpoint를 차단한다")
    void actuatorAccessIsRestricted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));
        mockMvc.perform(get("/actuator/mappings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));

        mockMvc.perform(get("/actuator/metrics").with(authentication(authToken(Role.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인 성공 시 역할에 따라 리다이렉트한다")
    void loginSuccessRedirectsByRole() throws Exception {
        CustomLoginSuccessHandler handler = new CustomLoginSuccessHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        MockHttpServletResponse userResponse = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                request,
                adminResponse,
                new UsernamePasswordAuthenticationToken(principal(1L, Role.ADMIN), "credentials")
        );
        handler.onAuthenticationSuccess(
                request,
                userResponse,
                new UsernamePasswordAuthenticationToken(principal(2L, Role.USER), "credentials")
        );

        assertThat(adminResponse.getRedirectedUrl()).isEqualTo("/admin");
        assertThat(userResponse.getRedirectedUrl()).isEqualTo("/");
    }

    private UsernamePasswordAuthenticationToken authToken(Role role) {
        return new UsernamePasswordAuthenticationToken(principal(1L, role), "credentials", principal(1L, role).getAuthorities());
    }
}
