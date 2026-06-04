package project.board.comment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MyRecentCommentResponse {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
