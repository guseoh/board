package project.board.comment.dto;

import lombok.Builder;
import lombok.Getter;
import project.board.comment.entity.Comment;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyCommentResponse {

    private Long id;
    private Long postId;
    private String postTitle;
    private String content;
    private LocalDateTime createdAt;


//    public static MyCommentResponse from(Comment comment) {
//        return
//    }

}
