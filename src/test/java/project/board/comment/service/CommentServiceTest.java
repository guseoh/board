package project.board.comment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import project.board.comment.dto.CommentRequestDto;
import project.board.comment.dto.CommentResponse;
import project.board.comment.dto.MyCommentPageResponse;
import project.board.comment.dto.MyCommentResponse;
import project.board.comment.dto.MyRecentComment;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.global.dto.PageRequestDto;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static project.board.testsupport.TestFixtures.comment;
import static project.board.testsupport.TestFixtures.member;
import static project.board.testsupport.TestFixtures.post;
import static project.board.testsupport.TestFixtures.reply;
import static project.board.testsupport.TestFixtures.setId;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("회원과 게시글이 존재하면 최상위 댓글을 생성한다")
    void createSuccess() {
        Member member = member(1L);
        Post post = post(10L, member(2L));
        CommentRequestDto request = request("comment content");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            setId(saved, 100L);
            return saved;
        });

        CommentResponse response = commentService.create(request, 1L, 10L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("comment content");
        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getPostId()).isEqualTo(10L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 작성자가 없으면 게시글을 조회하지 않는다")
    void createMemberNotFound() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(request("comment"), 1L, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        verify(postRepository, never()).findById(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 게시글의 최상위 댓글에만 답글을 생성한다")
    void createReplySuccessAndInvalidParent() {
        Member member = member(1L);
        Post post = post(10L, member(2L));
        Post otherPost = post(20L, member(3L));
        Comment parent = comment(100L, "parent", member(2L), post);
        Comment child = reply(101L, "child", member, post, parent);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.findById(100L)).willReturn(Optional.of(parent));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            setId(saved, 200L);
            return saved;
        });

        CommentResponse response = commentService.createReply(request("reply"), 1L, 10L, 100L);

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getContent()).isEqualTo("reply");

        given(postRepository.findById(20L)).willReturn(Optional.of(otherPost));
        assertThatThrownBy(() -> commentService.createReply(request("reply"), 1L, 20L, 100L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_INVALID_PARENT.getMessage());

        given(commentRepository.findById(101L)).willReturn(Optional.of(child));
        assertThatThrownBy(() -> commentService.createReply(request("reply"), 1L, 10L, 101L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_INVALID_PARENT.getMessage());
    }

    @Test
    @DisplayName("댓글 작성자만 댓글을 수정하고 삭제할 수 있다")
    void updateAndDeleteOwnerOnly() {
        Member owner = member(1L);
        Post post = post(10L, member(2L));
        Comment comment = comment(100L, "old", owner, post);
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        CommentResponse response = commentService.update(100L, 1L, 10L, request("changed"));
        commentService.delete(1L, 100L, 10L);

        assertThat(response.getContent()).isEqualTo("changed");
        verify(commentRepository).delete(comment);
        assertThatThrownBy(() -> commentService.update(100L, 2L, 10L, request("fail")))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_OWNER.getMessage());
    }

    @Test
    @DisplayName("수정할 댓글이 없으면 댓글 없음 예외를 던진다")
    void updateMissingComment() {
        given(commentRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(404L, 1L, 10L, request("changed")))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("내 댓글 통계, 페이지, 최근 댓글을 반환한다")
    void myCommentQueries() {
        MyCommentResponse myComment = new MyCommentResponse(1L, 10L, "post title", "comment", LocalDateTime.now());
        MyRecentComment recent = MyRecentComment.builder()
                .id(1L)
                .title("post title")
                .content("comment")
                .createdAt(LocalDateTime.now())
                .build();
        given(commentRepository.countByMemberId(1L)).willReturn(3L);
        given(commentRepository.countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any(), any()))
                .willReturn(1L);
        given(commentRepository.countByMemberIdAndCreatedAtGreaterThanEqual(any(), any()))
                .willReturn(2L);
        given(commentRepository.findMyComments(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(myComment), PageRequest.of(0, 5), 1));
        given(commentRepository.findRecentComments(any(), any())).willReturn(List.of(recent));

        MyCommentPageResponse page = commentService.myCommentPage(
                1L,
                PageRequestDto.builder().page(1).size(5).build(),
                "comment"
        );

        assertThat(commentService.myCommentCount(1L)).isEqualTo(3L);
        assertThat(page.getMyCommentCount()).isEqualTo(3L);
        assertThat(page.getTodayMyCommentCount()).isEqualTo(1L);
        assertThat(page.getRecentCommentCount()).isEqualTo(2L);
        assertThat(page.getComments().getDtoList()).extracting(MyCommentResponse::getContent)
                .containsExactly("comment");
        assertThat(commentService.recentComments(1L)).extracting(MyRecentComment::getTitle)
                .containsExactly("post title");
        assertThatThrownBy(() -> commentService.myCommentPage(null, PageRequestDto.builder().build(), null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    private CommentRequestDto request(String content) {
        CommentRequestDto request = new CommentRequestDto();
        request.setContent(content);
        return request;
    }
}
