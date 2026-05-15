package project.board.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.request.PostRequest;
import project.board.post.dto.response.PostListResponse;
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
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 작성은 로그인 회원이 존재하면 제목과 내용, 작성자를 저장하고 응답으로 반환한다")
    void save_success_savesPostWithWriter() {
        // given
        Long memberId = 1L;
        Member writer = member(memberId, "작성자");
        PostRequest request = new PostRequest("테스트 제목", "테스트 내용");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(writer));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        // when
        PostListResponse response = postService.save(request, memberId);

        // then
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("테스트 제목");
        assertThat(response.getMemberNickname()).isEqualTo("작성자");
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getContent()).isEqualTo("테스트 내용");
        assertThat(postCaptor.getValue().getMember()).isSameAs(writer);
    }

    @Test
    @DisplayName("게시글 작성은 로그인 회원을 찾을 수 없으면 로그인 필요 예외를 던진다")
    void save_memberNotFound_throwsLoginRequired() {
        // given
        Long memberId = 999L;
        PostRequest request = new PostRequest("테스트 제목", "테스트 내용");
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.save(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.LOGIN_REQUIRED.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정은 작성자 본인이 요청하면 제목과 내용을 변경한다")
    void update_owner_success_changesTitleAndContent() {
        // given
        Long writerId = 1L;
        Long postId = 10L;
        Post post = post(postId, "기존 제목", "기존 내용", member(writerId, "작성자"));
        PostRequest request = new PostRequest("수정 제목", "수정 내용");
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        postService.update(request, postId, writerId);

        // then
        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getContent()).isEqualTo("수정 내용");
    }

    @Test
    @DisplayName("게시글 수정은 작성자가 아니면 예외를 던지고 내용을 변경하지 않는다")
    void update_notOwner_throwsException() {
        // given
        Long writerId = 1L;
        Long otherMemberId = 2L;
        Long postId = 10L;
        Post post = post(postId, "기존 제목", "기존 내용", member(writerId, "작성자"));
        PostRequest request = new PostRequest("수정 제목", "수정 내용");
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.update(request, postId, otherMemberId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getContent()).isEqualTo("기존 내용");
    }

    @Test
    @DisplayName("게시글 삭제는 작성자 본인이 요청하면 댓글을 먼저 삭제하고 게시글을 삭제한다")
    void delete_owner_success_deletesCommentsAndPost() {
        // given
        Long writerId = 1L;
        Long postId = 10L;
        Post post = post(postId, "삭제 제목", "삭제 내용", member(writerId, "작성자"));
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        postService.delete(postId, writerId);

        // then
        verify(commentRepository).deleteById(postId);
        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("조회수 증가는 수정된 행이 없으면 게시글 없음 예외를 던진다")
    void viewCount_postNotFound_throwsException() {
        // given
        Long postId = 999L;
        given(postRepository.incrementViewCount(postId)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> postService.viewCount(postId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("검색은 제목에 키워드가 포함된 게시글 목록을 응답 DTO로 변환한다")
    void search_success_mapsPostListResponses() {
        // given
        Member writer = member(1L, "작성자");
        Post first = post(10L, "Spring Boot 테스트", "내용1", writer);
        Post second = post(11L, "Spring Security 테스트", "내용2", writer);
        given(postRepository.findByTitleContaining("Spring")).willReturn(List.of(first, second));

        // when
        List<PostListResponse> responses = postService.search("Spring");

        // then
        assertThat(responses).extracting(PostListResponse::getTitle)
                .containsExactly("Spring Boot 테스트", "Spring Security 테스트");
        assertThat(responses).extracting(PostListResponse::getMemberNickname)
                .containsExactly("작성자", "작성자");
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
}
