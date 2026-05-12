package project.board.post.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 최근 작성한 글
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostRecent {

    private Long id;
    private String title;
    private int viewCount;
    private LocalDateTime createdAt;

}
