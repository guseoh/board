package project.board.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import project.board.global.pagination.PageResultDto;
import project.board.post.entity.Post;

@Getter
@Builder
public class MyPostResponse {

    private long myPostCount;
    private long todayMyPostCount;
    private long myPostViewCount;

    private PageResultDto<PostListResponse, Post> posts;

}
