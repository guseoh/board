package project.board.comment.dto;

import lombok.Builder;
import lombok.Getter;
import project.board.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponse {
    private Long id;
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;


    private Long memberId;
    private String nickname;
    private Long postId;

    private List<CommentResponse> replies;

    public static CommentResponse from(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .createdBy(c.getCreatedBy())
                .updatedBy(c.getUpdatedBy())
                .memberId(c.getMember().getId())
                .nickname(c.getMember().getNickname())
                .postId(c.getPost().getId())
                .replies(c.getChildren().stream()
                        .map(CommentResponse::fromReply)
                        .toList())
                .build();
    }

    public static CommentResponse fromReply(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .createdBy(c.getCreatedBy())
                .updatedBy(c.getUpdatedBy())
                .memberId(c.getMember().getId())
                .nickname(c.getMember().getNickname())
                .postId(c.getPost().getId())
                .replies(List.of())
                .build();
    }
}
