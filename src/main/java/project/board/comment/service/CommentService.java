package project.board.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.board.comment.dto.*;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.global.dto.PageRequestDto;
import project.board.global.dto.PageResultDto;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.global.security.user.UnifiedPrincipal;
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


    public CommentDto.Response create(CommentRequestDto commentDto, Long memberId, Long postId) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment comment = Comment.create(commentDto.getContent(), member, post);

        Comment saved = commentRepository.save(comment);

        return CommentDto.Response.from(saved);
    }

    public CommentDto.Response update(Long commentId, Long memberId, Long postId, CommentRequestDto dto) {

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_NOT_FOUND, "/post/" + postId)
        );

        validateOwner(comment, memberId);

        comment.changeContent(dto.getContent());

        return CommentDto.Response.from(comment);
    }

    public void delete(Long memberId, Long commentId, Long postId) {

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new CustomException(ErrorCode.COMMENT_NOT_FOUND, "/post/" + postId)
        );

        validateOwner(comment, memberId);

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto.Response> findAll(Long postId) {

        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND, "/post/" + postId);
        }

        List<Comment> comments = commentRepository.findAllByPostIdOrderByIdAsc(postId);

        return comments.stream()
                .map(CommentDto.Response::from)
                .toList();
    }


    // 내가 작성한 댓글 조회
    public Long myCommentCount(Long memberId) {
        return commentRepository.countByMemberId(memberId);
    }

    public MyCommentPageResponse myCommentPage(PageRequestDto request, String keyword) {

        Long memberId = getLoginMemberId();

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
                .myCommentCount(myCommentCount(memberId))
                .todayMyCommentCount(todayComment)
                .recentCommentCount(recentComment)
                .comments(commentsPage)
                .build();
    }

    public List<MyRecentComment> recentComments(Long memberId) {
        return commentRepository.findRecentComments(
                memberId, PageRequest.of(0, 5));
    }

    private void validateOwner(Comment comment, Long memberId) {
        if (!comment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_OWNER);
        }
    }

    private Long getLoginMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UnifiedPrincipal unifiedPrincipal)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_AUTHENTICATION);
        }

        return unifiedPrincipal.getMemberId();
    }

}
