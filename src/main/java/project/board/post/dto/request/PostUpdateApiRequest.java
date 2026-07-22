package project.board.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostUpdateApiRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(
                max = 500,
                message = "제목은 500자 이하여야 합니다."
        )
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(
                max = 500,
                message = "내용은 500자 이하여야 합니다."
        )
        String content
) {
}
