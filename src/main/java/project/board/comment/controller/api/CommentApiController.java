package project.board.comment.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.board.comment.dto.response.CommentListApiResponse;
import project.board.comment.service.CommentService;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentApiController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<CommentListApiResponse> getComments(
            @PathVariable Long postId
    ) {
        /*
         * 게시글 상세와 댓글 목록을 별도 리소스로 분리한다.
         * 게시글 본문 조회가 댓글 개수와 구조에 종속되지 않도록 하기 위한 API 경계다.
         */
        CommentListApiResponse response = commentService.getCommentsApi(postId);

        return ResponseEntity.ok(response);
    }
}
