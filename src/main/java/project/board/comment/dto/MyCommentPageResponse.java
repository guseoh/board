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

//    private List<MyCommentResponse> comments;
    private PageResultDto<MyCommentResponse, Comment> page;
//    private String keyword;
}
