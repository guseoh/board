package project.board.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import project.board.member.entity.Member;
import project.board.post.entity.Post;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostListResponse {

    private Long id;
    private String title;
    private int viewCount;
    private String memberNickname;
    private LocalDateTime createdAt;

//    @Getter
//    @Builder
//    private static class MemberSummary {
//        private Long id;
//        private String nickname;
//
//        public static MemberSummary from(Member member) {
//            return MemberSummary.builder()
//                    .id(member.getId())
//                    .nickname(member.getNickname())
//                    .build();
//        }
//    }

    public static PostListResponse from(Post post) {
        return PostListResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .viewCount(post.getViewCount())
                .memberNickname(post.getMember().getNickname())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
