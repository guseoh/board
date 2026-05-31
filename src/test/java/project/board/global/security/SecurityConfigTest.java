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
import project.board.global.security.user.CustomLoginSuccessHandler;
import project.board.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static project.board.testsupport.TestFixtures.principal;

@SpringBootTest(properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("anonymous users are redirected from my pages and post writes")
    void anonymousAccessIsRestricted() throws Exception {
        mockMvc.perform(get("/my"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));

        mockMvc.perform(post("/post/new").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));
    }

    @Test
    @DisplayName("admin URLs reject USER and allow ADMIN")
    void adminAccessControl() throws Exception {
        mockMvc.perform(get("/admin").with(authentication(authToken(Role.USER))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin").with(authentication(authToken(Role.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST requests protected by security require CSRF token")
    void csrfRequiredForPost() throws Exception {
        mockMvc.perform(post("/post/new")
                        .with(authentication(authToken(Role.USER)))
                        .param("title", "title")
                        .param("content", "content"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("login success redirects by role")
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
