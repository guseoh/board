package project.board.comment.repository;

import org.springframework.data.jpa.repository.Query;
import project.board.comment.dto.MyCommentResponse;
import project.board.comment.entity.Comment;
import project.board.global.dto.PageRequestDto;
import project.board.global.dto.PageResultDto;

public interface CommentQueryRepository {

    PageResultDto<MyCommentResponse, Comment> MyComments(
            Long memberId,
            PageRequestDto request,
            String key
    )


}
