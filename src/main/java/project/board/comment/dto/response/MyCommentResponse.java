package project.board.comment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyCommentResponse {

    private Long id;
    private Long postId;
    private String postTitle;
    private String content;
    private LocalDateTime createdAt;

    public MyCommentResponse(Long id, Long postId, String postTitle, String content, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.postTitle = postTitle;
        this.content = content;
        this.createdAt = createdAt;
    }
}
