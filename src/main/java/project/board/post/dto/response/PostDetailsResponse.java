package project.board.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import project.board.comment.dto.CommentDto;
import project.board.member.entity.Member;
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

    private LocalDateTime createdAt;
    private List<CommentDto.Response> comments;

    private MemberSummary member;

    @Getter
    @Builder
    private static class MemberSummary {
        private Long id;
        private String nickname;

        public static MemberSummary from(Member member) {
            return MemberSummary.builder()
                    .id(member.getId())
                    .nickname(member.getNickname())
                    .build();
        }
    }

    public static PostDetailsResponse from(Post post, List<CommentDto.Response> comments) {
        return PostDetailsResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .comments(comments)
                .member(MemberSummary.from(post.getMember()))
                .build();
    }
}
