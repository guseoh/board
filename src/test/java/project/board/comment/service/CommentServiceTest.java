package project.board.comment.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import project.board.comment.dto.CommentDto;
import project.board.comment.dto.CommentRequestDto;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.global.security.user.UnifiedPrincipal;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("댓글 작성은 회원과 게시글이 존재하면 댓글을 저장하고 응답 DTO를 반환한다")
    void create_success_savesCommentAndReturnsResponse() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        Member member = member(memberId, "댓글작성자");
        Post post = post(postId, "게시글 제목", "게시글 내용", member(2L, "게시글작성자"));
        CommentRequestDto request = commentRequest("댓글 내용");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        // when
        CommentDto.Response response = commentService.create(request, memberId, postId);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("댓글 내용");
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getNickname()).isEqualTo("댓글작성자");
        assertThat(response.getPostId()).isEqualTo(postId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 작성은 회원이 존재하지 않으면 회원 없음 예외를 던지고 저장하지 않는다")
    void create_memberNotFound_throwsException() {
        // given
        Long memberId = 999L;
        Long postId = 10L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.create(commentRequest("댓글 내용"), memberId, postId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        verify(postRepository, never()).findById(postId);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 수정은 작성자 본인이 요청하면 댓글 내용을 변경하고 응답 DTO를 반환한다")
    void update_owner_success_changesContent() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        Long commentId = 100L;
        Comment comment = comment(commentId, "기존 댓글", member(memberId, "댓글작성자"), post(postId, "제목", "내용", member(2L, "게시글작성자")));
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        CommentDto.Response response = commentService.update(commentId, memberId, postId, commentRequest("수정 댓글"));

        // then
        assertThat(comment.getContent()).isEqualTo("수정 댓글");
        assertThat(response.getContent()).isEqualTo("수정 댓글");
    }

    @Test
    @DisplayName("댓글 수정은 작성자가 아니면 예외를 던지고 내용을 변경하지 않는다")
    void update_notOwner_throwsException() {
        // given
        Long ownerId = 1L;
        Long otherMemberId = 2L;
        Long postId = 10L;
        Long commentId = 100L;
        Comment comment = comment(commentId, "기존 댓글", member(ownerId, "댓글작성자"), post(postId, "제목", "내용", member(3L, "게시글작성자")));
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.update(commentId, otherMemberId, postId, commentRequest("수정 댓글")))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_OWNER.getMessage());
        assertThat(comment.getContent()).isEqualTo("기존 댓글");
    }

    @Test
    @DisplayName("댓글 삭제는 작성자 본인이 요청하면 댓글을 삭제한다")
    void delete_owner_success_deletesComment() {
        // given
        Long memberId = 1L;
        Long postId = 10L;
        Long commentId = 100L;
        Comment comment = comment(commentId, "삭제 댓글", member(memberId, "댓글작성자"), post(postId, "제목", "내용", member(2L, "게시글작성자")));
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        commentService.delete(memberId, commentId, postId);

        // then
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("댓글 목록 조회는 게시글이 존재하면 게시글 댓글을 id 오름차순 응답 DTO 목록으로 반환한다")
    void findAll_postExists_returnsComments() {
        // given
        Long postId = 10L;
        Member writer = member(1L, "댓글작성자");
        Post post = post(postId, "제목", "내용", member(2L, "게시글작성자"));
        Comment first = comment(100L, "첫 댓글", writer, post);
        Comment second = comment(101L, "두 번째 댓글", writer, post);
        given(postRepository.existsById(postId)).willReturn(true);
        given(commentRepository.findAllByPostIdOrderByIdAsc(postId)).willReturn(List.of(first, second));

        // when
        List<CommentDto.Response> responses = commentService.findAll(postId);

        // then
        assertThat(responses).extracting(CommentDto.Response::getContent)
                .containsExactly("첫 댓글", "두 번째 댓글");
    }

    @Test
    @DisplayName("내 댓글 수 조회는 인증된 UnifiedPrincipal의 회원 id로 댓글 수를 조회한다")
    void myCommentCount_authenticatedPrincipal_countsByLoginMemberId() {
        // given
        Long loginMemberId = 1L;
        loginAs(loginMemberId);
        given(commentRepository.countByMemberId(loginMemberId)).willReturn(3L);

        // when
        Long count = commentService.myCommentCount(999L);

        // then
        assertThat(count).isEqualTo(3L);
        verify(commentRepository).countByMemberId(loginMemberId);
    }

    @Test
    @DisplayName("내 댓글 수 조회는 UnifiedPrincipal 인증 정보가 아니면 인증 정보 불일치 예외를 던진다")
    void myCommentCount_invalidPrincipal_throwsException() {
        // given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("plain-user", "password")
        );

        // when & then
        assertThatThrownBy(() -> commentService.myCommentCount(1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_AUTHENTICATION.getMessage());
    }

    private CommentRequestDto commentRequest(String content) {
        CommentRequestDto request = new CommentRequestDto();
        request.setContent(content);
        return request;
    }

    private Member member(Long id, String nickname) {
        Member member = Member.create(nickname, nickname + "@example.com", "encoded", Role.USER);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Post post(Long id, String title, String content, Member member) {
        Post post = Post.create(title, content, member);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Comment comment(Long id, String content, Member member, Post post) {
        Comment comment = Comment.create(content, member, post);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private void loginAs(Long memberId) {
        UnifiedPrincipal principal = new UnifiedPrincipal("사용자", memberId, "user@example.com", "ROLE_USER", "password", null, null, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities())
        );
    }
}
