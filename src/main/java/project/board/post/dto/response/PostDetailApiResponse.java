package project.board.post.dto.response;

import project.board.member.entity.Member;
import project.board.post.entity.Post;

import java.time.LocalDateTime;

public record PostDetailApiResponse(
        Long id,
        String title,
        String content,
        int viewCount,
        WriterResponse writer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailApiResponse from(Post post) {
        return new PostDetailApiResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getViewCount(),
                WriterResponse.from(post.getMember()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public record WriterResponse(
            Long id, String nickname
    ) {
        public static WriterResponse from(Member member) {
            return new WriterResponse(
                    member.getId(), member.getNickname()
            );
        }
    }



}
