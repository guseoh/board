package project.board.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import project.board.comment.dto.CommentDto;
import project.board.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PostDetailsResponse {

    private Long id;
    private String title;
    private String content;
    private int viewCount;

    private LocalDateTime CreatedAt;
    private List<CommentDto.Response> comments;

    public static PostDetailsResponse from(Post post, List<CommentDto.Response> comments) {
        return PostDetailsResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .CreatedAt(post.getCreatedAt())
                .comments(comments)
                .build();
    }

}
