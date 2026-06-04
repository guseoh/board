package project.board.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateRequest {

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

}
