package project.board.global.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import project.board.comment.dto.request.CommentCreateRequest;
import project.board.global.pagination.PageRequestDto;
import project.board.member.dto.request.MemberCreateRequest;
import project.board.member.dto.request.MemberNicknameUpdateRequest;
import project.board.member.dto.request.MemberPasswordUpdateRequest;
import project.board.post.dto.request.PostRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void memberEmailAndNicknameAreRequiredAndFormatted() {
        MemberCreateRequest request = memberRequest();
        request.setEmail(null);
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).contains("email");
        request.setEmail("");
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).contains("email");
        request.setEmail("   ");
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).contains("email");
        request.setEmail("invalid-email");
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).contains("email");
        request.setEmail("user@example.com");
        assertThat(validator.validate(request)).isEmpty();

        MemberNicknameUpdateRequest nickname = new MemberNicknameUpdateRequest();
        for (String value : new String[]{null, "", " ", "a", "nickname12345", "name!"}) {
            nickname.setNickname(value);
            assertThat(validator.validate(nickname)).isNotEmpty();
        }
        for (String value : new String[]{"ab", "가나다라마바사아자차카타"}) {
            nickname.setNickname(value);
            assertThat(validator.validate(nickname)).isEmpty();
        }
    }

    @Test
    void passwordPostCommentAndPaginationBoundsAreValidated() {
        MemberPasswordUpdateRequest password = new MemberPasswordUpdateRequest();
        password.setCurrentPassword("short");
        password.setNewPassword("newpass1");
        password.setNewPasswordConfirm("");
        assertThat(validator.validate(password)).extracting(v -> v.getPropertyPath().toString())
                .contains("currentPassword", "newPasswordConfirm");

        String max = "a".repeat(500);
        assertThat(validator.validate(new PostRequest(max, max))).isEmpty();
        assertThat(validator.validate(new PostRequest(max + "a", max))).isNotEmpty();
        CommentCreateRequest comment = new CommentCreateRequest();
        comment.setContent(max);
        assertThat(validator.validate(comment)).isEmpty();
        comment.setContent(max + "a");
        assertThat(validator.validate(comment)).isNotEmpty();

        assertThat(validator.validate(PageRequestDto.builder().build())).isEmpty();
        for (PageRequestDto request : new PageRequestDto[]{
                PageRequestDto.builder().page(0).size(5).build(),
                PageRequestDto.builder().page(-1).size(5).build(),
                PageRequestDto.builder().page(1).size(0).build(),
                PageRequestDto.builder().page(1).size(101).build()}) {
            assertThat(validator.validate(request)).isNotEmpty();
        }
    }

    private MemberCreateRequest memberRequest() {
        MemberCreateRequest request = new MemberCreateRequest();
        request.setNickname("tester");
        request.setPassword("password1");
        request.setPasswordConfirm("password1");
        return request;
    }
}
