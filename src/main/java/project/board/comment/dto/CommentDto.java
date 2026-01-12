package project.board.comment.dto;


import com.sun.source.doctree.CommentTree;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.board.comment.entity.Comment;
import project.board.member.dto.MemberDto;
import project.board.post.dto.PostDto;

import java.time.LocalDateTime;

public class CommentDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String content;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String content;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;

        private MemberDto.Response member;
        private PostDto.Response post;

        public static Response from(Comment comment) {
            return Response.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .member(MemberDto.Response.from(comment.getMember()))
                    .post(PostDto.Response.from(comment.getPost()))
                    .build();
        }


    }
}



//todo: 검증 추가