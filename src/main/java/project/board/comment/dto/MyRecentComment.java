package project.board.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MyRecentComment {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
