package project.board.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.board.comment.dto.request.CommentCreateRequest;
import project.board.comment.dto.response.CommentListApiResponse;
import project.board.comment.dto.response.CommentResponse;
import project.board.comment.dto.response.MyCommentPageResponse;
import project.board.comment.dto.response.MyCommentResponse;
import project.board.comment.dto.response.MyRecentCommentResponse;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.global.pagination.PageRequestDto;
import project.board.global.pagination.PageResultDto;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public CommentListApiResponse getCommentsApi(Long postId) {
        /*
         * 댓글이 없는 게시글은 빈 배열을 반환하지만,
         * 게시글 자체가 없으면 POST_NOT_FOUND로 구분해야 한다.
         * 기존 Post.comments 연관관계를 사용해 현재 도메인 구조를 그대로 재사용한다.
         */
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND)
        );

        return CommentListApiResponse.from(post.getComments());
    }

    public CommentResponse createComment(CommentCreateRequest commentDto, Long memberId, Long postId) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment comment = Comment.create(commentDto.getContent(), member, post, null);

        Comment saved = commentRepository.save(comment);

        return CommentResponse.from(saved);
    }


    public CommentResponse createReply(CommentCreateRequest commentDto, Long memberId, Long postId, Long parentId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment parent = commentRepository.findById(parentId).orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        validateReply(parent, post);

        Comment reply = Comment.create(commentDto.getContent(), member, post, parent);

        Comment saved = commentRepository.save(reply);

        return CommentResponse.from(saved);
    }

    public CommentResponse update(Long commentId, Long memberId, Long postId, CommentCreateRequest dto) {

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_NOT_FOUND, "/post/" + postId)
        );

        validateCommentPost(comment, postId);
        validateOwner(comment, memberId);

        comment.changeContent(dto.getContent());

        return CommentResponse.from(comment);
    }

    public void delete(Long memberId, Long commentId, Long postId) {

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_NOT_FOUND, "/post/" + postId)
        );

        validateCommentPost(comment, postId);
        validateOwner(comment, memberId);

        commentRepository.deleteRepliesByParentId(commentId);
        commentRepository.delete(comment);
    }

    // 내가 작성한 댓글 조회
    public Long countMyComment(Long memberId) {
        return commentRepository.countByMemberId(memberId);
    }


    public MyCommentPageResponse getMyCommentPage(Long memberId, PageRequestDto request, String keyword) {

        if (memberId == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startDay = today.atStartOfDay();
        LocalDateTime nextDay = today.plusDays(1).atStartOfDay();

        long todayComment = commentRepository.countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                memberId, startDay, nextDay
        );

        long recentComment = commentRepository.countByMemberIdAndCreatedAtGreaterThanEqual(
                memberId, today.minusDays(7).atStartOfDay()
        );

        Pageable pageable = request.getPageable(Sort.by("id").descending());

        Page<MyCommentResponse> comments = commentRepository.findMyComments(
                memberId, keyword, pageable
        );

        PageResultDto<MyCommentResponse, Comment> commentsPage =
                new PageResultDto<>(comments);


        return MyCommentPageResponse.builder()
                .myCommentCount(countMyComment(memberId))
                .todayMyCommentCount(todayComment)
                .recentCommentCount(recentComment)
                .comments(commentsPage)
                .build();
    }

    public List<MyRecentCommentResponse> recentComments(Long memberId) {
        return commentRepository.findRecentComments(
                memberId, PageRequest.of(0, 5));
    }

    private void validateOwner(Comment comment, Long memberId) {
        if (!comment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_OWNER);
        }
    }

    private static void validateCommentPost(Comment comment, Long postId) {
        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND, "/post/" + postId);
        }
    }


    private static void validateReply(Comment parent, Post post) {
        if (!parent.getPost().getId().equals(post.getId())) {
            throw new CustomException(ErrorCode.COMMENT_INVALID_PARENT);
        }

        if (parent.isReply()) {
            throw new CustomException(ErrorCode.COMMENT_INVALID_PARENT);
        }
    }
}
