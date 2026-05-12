package project.board.comment.dto;

import lombok.Builder;
import lombok.Getter;
import project.board.comment.entity.Comment;
import project.board.global.dto.PageResultDto;

import java.util.List;

@Getter
@Builder
public class MyCommentPageResponse {

    private long myCommentCount;
    private long todayMyCommentCount;
    private long recentCommentCount;

    private PageResultDto<MyCommentResponse, Comment> comments;
}
