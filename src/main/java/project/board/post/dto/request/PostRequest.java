package project.board.post.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.board.post.dto.response.PostDetailsResponse;

@Getter
@NoArgsConstructor
@Setter
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    public static PostRequest from(PostDetailsResponse post) {

        PostRequest request = new PostRequest();
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());

        return request;
    }

}
