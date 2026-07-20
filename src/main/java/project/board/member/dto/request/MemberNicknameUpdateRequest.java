package project.board.member.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberNicknameUpdateRequest {

    @Pattern(
            regexp = "^[a-zA-Z0-9가-힣]{2,12}$",
            message = "닉네임은 2~12자, 영문/숫자/한글만 가능합니다."
    )
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

}
