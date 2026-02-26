package project.board.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.board.comment.dto.CommentDto;
import project.board.comment.dto.CommentRequestDto;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

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

    private void validateOwner(Comment comment, Long memberId) {
        if (!comment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_OWNER);
        }
    }

}
