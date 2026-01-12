package project.board.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.board.comment.dto.CommentDto;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.post.dto.PostDto;

import java.time.LocalDateTime;
import java.util.List;

public class MemberDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {

        private String nickname;
        private String password;
        private String email;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {

        private String nickname;
        private String password;
        private String email;
    }

    @Getter
    @Builder
    public static class Response {

        private Long id;
        private String nickname;
        private String email;
        private Role role;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;

        private List<CommentDto.Response> comments;
        private List<PostDto.Response> posts;

        public static Response from(Member member) {
            return Response.builder()
                    .id(member.getId())
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .role(member.getRole())
                    .comments(member.getComments().stream()
                            .map(CommentDto.Response::from)
                            .toList())
                    .posts(member.getPosts().stream()
                            .map(PostDto.Response::from)
                            .toList())
                    .build();
        }
    }


}
