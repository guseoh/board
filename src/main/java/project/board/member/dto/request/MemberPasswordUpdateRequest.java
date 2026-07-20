package project.board.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberPasswordUpdateRequest {

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

    public void clearPasswords() {
        currentPassword = null;
        newPassword = null;
        newPasswordConfirm = null;
    }
}
