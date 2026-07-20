package project.board.post.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.board.post.dto.response.PostDetailResponse;

@Getter
@NoArgsConstructor
@Setter
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 500, message = "제목은 500자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 500, message = "내용은 500자 이하여야 합니다.")
    private String content;

    public static PostRequest from(PostDetailResponse post) {
        PostRequest request = new PostRequest();
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());

        return request;
    }

}
