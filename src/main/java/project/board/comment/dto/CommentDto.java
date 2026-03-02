package project.board.comment.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.board.comment.entity.Comment;
import project.board.member.dto.MemberDto;
import project.board.post.dto.PostDto;

import java.time.LocalDateTime;

public class CommentDto {

//    @Getter
//    @NoArgsConstructor
//    public static class CreateRequest {
//        private String content;
//    }
//
//    @Getter
//    @NoArgsConstructor
//    public static class UpdateRequest {
//        private String content;
//    }



    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String content;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;

//        private MemberDto.Response member;
//        private PostDto.Response post;

        private Long memberId;
        private String nickname;
        private Long postId;

        public static Response from(Comment c) {
            return Response.builder()
                    .id(c.getId())
                    .content(c.getContent())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .createdBy(c.getCreatedBy())
                    .updatedBy(c.getUpdatedBy())
                    .memberId(c.getMember().getId())
                    .nickname(c.getMember().getNickname())
                    .postId(c.getPost().getId())
                    .build();
        }


    }
}



