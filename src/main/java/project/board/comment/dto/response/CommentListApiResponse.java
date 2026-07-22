package project.board.comment.dto.response;

import project.board.comment.entity.Comment;
import project.board.member.entity.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CommentListApiResponse(
        List<CommentResponse> comments
) {

    public CommentListApiResponse {
        comments = comments == null
                ? List.of()
                : List.copyOf(comments);
    }

    public static CommentListApiResponse from(List<Comment> comments) {
        /*
         * Post.comments에는 루트 댓글과 답글이 함께 들어 있다.
         * 엔티티 연관관계를 JSON으로 직접 노출하지 않고,
         * API 응답에서 사용할 한 단계 계층 구조로 변환한다.
         */
        Map<Long, List<ReplyResponse>> repliesByParentId = new LinkedHashMap<>();

        for (Comment comment : comments) {
            if (comment.rootComment()) {
                continue;
            }

            repliesByParentId
                    .computeIfAbsent(comment.getParent().getId(), ignored -> new ArrayList<>())
                    .add(ReplyResponse.from(comment));
        }

        List<CommentResponse> rootComments = comments.stream()
                .filter(Comment::rootComment)
                .map(comment -> CommentResponse.from(
                        comment,
                        repliesByParentId.getOrDefault(comment.getId(), List.of())
                ))
                .toList();

        return new CommentListApiResponse(rootComments);
    }

    public record CommentResponse(
            Long id,
            String content,
            WriterResponse writer,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ReplyResponse> replies
    ) {

        public CommentResponse {
            replies = replies == null
                    ? List.of()
                    : List.copyOf(replies);
        }

        private static CommentResponse from(
                Comment comment,
                List<ReplyResponse> replies
        ) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getContent(),
                    WriterResponse.from(comment.getMember()),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt(),
                    replies
            );
        }
    }

    public record ReplyResponse(
            Long id,
            String content,
            WriterResponse writer,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {

        private static ReplyResponse from(Comment reply) {
            return new ReplyResponse(
                    reply.getId(),
                    reply.getContent(),
                    WriterResponse.from(reply.getMember()),
                    reply.getCreatedAt(),
                    reply.getUpdatedAt()
            );
        }
    }

    public record WriterResponse(
            Long id,
            String nickname
    ) {

        private static WriterResponse from(Member member) {
            return new WriterResponse(
                    member.getId(),
                    member.getNickname()
            );
        }
    }
}
