package project.board.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.board.comment.dto.CommentDto;
import project.board.member.dto.MemberDto;
import project.board.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    @Getter
    @NoArgsConstructor //todo: 사용하는 이유?
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "제목은 필수입니다.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

//        public static Post toEntity(CreateRequest request) {
//            return Post.createPost(request.title, request.content);
//        }

    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        private String title;
        private String content;

//        public static Post toEntity(UpdateRequest request) {
//            return Post.createPost(request.title, request.content);
//        }

    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private int viewCount;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;

        private List<CommentDto.Response> comments;
        private MemberDto.Response member;

        public static Response from(Post post) {
            return Response.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .viewCount(post.getViewCount())
                    .createdAt(post.getCreatedAt())
                    .createdBy(post.getCreatedBy())
                    .updatedAt(post.getUpdatedAt())
                    .updatedBy(post.getUpdatedBy())
                    .member(MemberDto.Response.from(post.getMember()))
                    .comments(post.getComments().stream()
                            .map(CommentDto.Response::from)
                            .toList())
                    .build();
        }

    }
}
