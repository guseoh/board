package project.board.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Builder
public class MemberUpdateRequest {

    @Pattern(
            regexp = "^[a-zA-Z0-9가-힣]{2,12}$",
            message = "닉네임은 2~12자, 영문/숫자/한글만 가능합니다."
    )
    private String nickname;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
            message = "비밀번호는 8~20자, 영문과 숫자를 각각 1개 이상 포함해야 합니다."
    )
    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    private String currentPassword;


    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
            message = "비밀번호는 8~20자, 영문과 숫자를 각각 1개 이상 포함해야 합니다."
    )
    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    private String newPassword;

    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
            message = "비밀번호는 8~20자, 영문과 숫자를 각각 1개 이상 포함해야 합니다."
    )
    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    private String newPasswordConfirm;

}
