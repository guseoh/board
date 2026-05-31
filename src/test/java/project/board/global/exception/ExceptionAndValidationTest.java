package project.board.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import project.board.comment.dto.CommentRequestDto;
import project.board.global.notification.discord.DiscordNotifier;
import project.board.member.dto.request.MemberCreateRequest;
import project.board.post.dto.request.PostRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ExceptionAndValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("CustomException exposes ErrorCode message and redirect")
    void customExceptionFields() {
        CustomException exception = new CustomException(ErrorCode.POST_NOT_FOUND, "/post/1");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.POST_NOT_FOUND.getMessage());
        assertThat(exception.getRedirectUrl()).isEqualTo("/post/1");
    }

    @Test
    @DisplayName("Global advice returns signup view for signup business errors")
    void globalAdviceSignupError() throws Exception {
        DiscordNotifier discordNotifier = mock(DiscordNotifier.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalControllerAdvice(discordNotifier))
                .build();

        mockMvc.perform(get("/duplicate-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/signup"))
                .andExpect(model().attributeExists("form", "error"));

        verify(discordNotifier, never()).send(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Global advice redirects and flashes message for other custom errors")
    void globalAdviceRedirects() throws Exception {
        DiscordNotifier discordNotifier = mock(DiscordNotifier.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalControllerAdvice(discordNotifier))
                .build();

        mockMvc.perform(get("/missing-post"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("msg"));

        verify(discordNotifier).send(org.mockito.ArgumentMatchers.contains("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("Bean validation keeps view and model errors")
    void beanValidationFailureKeepsView() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController()).build();

        mockMvc.perform(post("/post-form")
                        .param("title", "")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("post/form"))
                .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    @DisplayName("request DTO validation catches invalid inputs")
    void dtoValidation() {
        PostRequest postRequest = new PostRequest("", "");
        CommentRequestDto commentRequest = new CommentRequestDto();
        commentRequest.setContent("");
        MemberCreateRequest memberRequest = new MemberCreateRequest();
        memberRequest.setNickname("u");
        memberRequest.setEmail("not-email");
        memberRequest.setPassword("short");
        memberRequest.setPasswordConfirm("");

        assertThat(validator.validate(postRequest)).hasSizeGreaterThanOrEqualTo(2);
        assertThat(validator.validate(commentRequest)).hasSize(1);
        assertThat(validator.validate(memberRequest)).hasSizeGreaterThanOrEqualTo(3);
    }

    @Controller
    private static class ThrowingController {

        @GetMapping("/duplicate-email")
        String duplicateEmail() {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        @GetMapping("/missing-post")
        String missingPost() {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        @PostMapping("/post-form")
        String validatePost(@Valid @ModelAttribute("form") PostRequest request,
                            BindingResult bindingResult,
                            Model model) {
            if (bindingResult.hasErrors()) {
                model.addAttribute("mode", "create");
                return "post/form";
            }
            return "redirect:/";
        }
    }
}
