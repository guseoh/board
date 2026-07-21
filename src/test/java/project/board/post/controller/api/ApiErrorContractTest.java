package project.board.post.controller.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Role;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static project.board.testsupport.TestFixtures.principal;

@SpringBootTest(properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiErrorContractTest.TestApiController.class)
class ApiErrorContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("익명 API 쓰기 요청은 로그인 화면이 아니라 401 JSON을 반환한다")
    void anonymousApiWriteReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(post("/api/test/validation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제목"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path").value("/api/test/validation"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("인증된 API 쓰기 요청도 CSRF 토큰이 없으면 403 JSON을 반환한다")
    void authenticatedApiWriteWithoutCsrfReturnsJsonForbidden() throws Exception {
        mockMvc.perform(post("/api/test/validation")
                        .with(authentication(authToken(Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제목"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISSING"))
                .andExpect(jsonPath("$.path").value("/api/test/validation"));
    }

    @Test
    @DisplayName("API 보안 추가 후에도 기존 SSR 게시글 쓰기와 수정 화면은 인증을 요구한다")
    void viewSecurityContractIsPreserved() throws Exception {
        mockMvc.perform(post("/post/new").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));

        mockMvc.perform(get("/post/10/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"));
    }

    @Test
    @DisplayName("게시글 없음 CustomException은 404 JSON으로 변환한다")
    void postNotFoundIsConvertedToJson() throws Exception {
        mockMvc.perform(get("/api/test/errors/POST_NOT_FOUND")
                        .with(authentication(authToken(Role.USER))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 게시글이 존재하지 않습니다."))
                .andExpect(jsonPath("$.path").value("/api/test/errors/POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("게시글 작성자 아님 CustomException은 403 JSON으로 변환한다")
    void notPostOwnerIsConvertedToJson() throws Exception {
        mockMvc.perform(get("/api/test/errors/NOT_POST_OWNER")
                        .with(authentication(authToken(Role.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("NOT_POST_OWNER"))
                .andExpect(jsonPath("$.message").value("본인 게시글만 수정/삭제 가능합니다"));
    }

    @Test
    @DisplayName("RequestBody Bean Validation 실패는 필드 오류를 포함한 400 JSON을 반환한다")
    void requestBodyValidationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/test/validation")
                        .with(authentication(authToken(Role.USER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("요청값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("제목은 필수입니다."));
    }

    @Test
    @DisplayName("깨진 JSON 요청은 MALFORMED_JSON 400 응답을 반환한다")
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/test/validation")
                        .with(authentication(authToken(Role.USER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.path").value("/api/test/validation"));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type은 415 JSON을 반환한다")
    void unsupportedMediaTypeReturnsJson() throws Exception {
        mockMvc.perform(post("/api/test/validation")
                        .with(authentication(authToken(Role.USER)))
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title=제목"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("잘못된 PathVariable 타입은 400 JSON을 반환한다")
    void invalidPathVariableTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/test/type/not-a-number")
                        .with(authentication(authToken(Role.USER))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT_TYPE"));
    }

    @Test
    @DisplayName("처리되지 않은 API 예외는 내부 상세를 숨긴 500 JSON을 반환한다")
    void unexpectedExceptionReturnsSafeJson() throws Exception {
        mockMvc.perform(get("/api/test/unexpected")
                        .with(authentication(authToken(Role.USER))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    private UsernamePasswordAuthenticationToken authToken(Role role) {
        return new UsernamePasswordAuthenticationToken(
                principal(1L, role),
                "credentials",
                principal(1L, role).getAuthorities()
        );
    }

    @RestController
    @RequestMapping("/api/test")
    static class TestApiController {

        @GetMapping("/errors/{errorCode}")
        void customException(@PathVariable ErrorCode errorCode) {
            throw new CustomException(errorCode);
        }

        @PostMapping(value = "/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validateRequest(@Valid @RequestBody TestCreateRequest request) {
        }

        @GetMapping("/type/{postId}")
        Long type(@PathVariable Long postId) {
            return postId;
        }

        @GetMapping("/unexpected")
        void unexpectedException() {
            throw new IllegalStateException("테스트용 내부 예외");
        }
    }

    record TestCreateRequest(
            @NotBlank(message = "제목은 필수입니다.")
            String title
    ) {
    }
}
