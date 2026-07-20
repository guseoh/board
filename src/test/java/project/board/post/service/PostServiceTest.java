package project.board.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.global.pagination.PageRequestDto;
import project.board.global.pagination.PageResultDto;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.request.PostRecent;
import project.board.post.dto.request.PostRequest;
import project.board.post.dto.response.PostDetailResponse;
import project.board.post.dto.response.PostListResponse;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static project.board.testsupport.TestFixtures.comment;
import static project.board.testsupport.TestFixtures.member;
import static project.board.testsupport.TestFixtures.post;
import static project.board.testsupport.TestFixtures.reply;
import static project.board.testsupport.TestFixtures.setId;

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
    @DisplayName("존재하는 회원의 게시글을 저장한다")
    void saveSuccess() {
        Long memberId = 1L;
        Member writer = member(memberId);
        PostRequest request = new PostRequest("title", "content");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(writer));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            setId(saved, 10L);
            return saved;
        });

        PostListResponse response = postService.createPost(request, memberId);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("title");
        assertThat(response.getMemberNickname()).isEqualTo(writer.getNickname());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("알 수 없는 회원으로 게시글 저장 시 로그인이 필요하다는 예외를 던진다")
    void saveMemberNotFound() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(new PostRequest("title", "content"), 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.LOGIN_REQUIRED.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("Controller 검증을 우회한 501자 게시글은 저장하거나 기존 내용을 변경하지 않는다")
    void rejectsOverlongPostAtDomainBoundary() {
        Member writer = member(1L);
        Post post = post(10L, "title", "content", writer);
        String tooLong = "a".repeat(501);
        given(memberRepository.findById(1L)).willReturn(Optional.of(writer));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.createPost(new PostRequest(tooLong, "content"), 1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_TITLE_TOO_LONG.getMessage());
        assertThatThrownBy(() -> postService.update(new PostRequest("title", tooLong), 10L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_CONTENT_TOO_LONG.getMessage());

        assertThat(post.getContent()).isEqualTo("content");
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("최상위 댓글과 답글을 포함한 상세 정보를 조회한다")
    void getPostDetailSuccess() {
        Member writer = member(1L);
        Member commenter = member(2L);
        Post post = post(10L, "detail title", "detail content", writer);
        Comment root = comment(100L, "comment", commenter, post);
        reply(101L, "reply", writer, post, root);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        PostDetailResponse response = postService.getPostDetail(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().get(0).getReplies()).hasSize(1);
    }

    @Test
    @DisplayName("작성자만 수정 화면용 게시글을 조회한다")
    void getPostForEditOwner() {
        Post post = post(10L, "edit title", "edit content", member(1L));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        PostDetailResponse response = postService.getPostForEdit(10L, 1L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("edit title");
        verify(postRepository).findById(10L);
    }

    @Test
    @DisplayName("수정 화면 조회도 비작성자와 ADMIN 비작성자를 허용하지 않는다")
    void getPostForEditRejectsNonOwnerAndAdmin() {
        Post post = post(10L, "title", "content", member(1L));
        Member admin = member(2L, "admin", "admin@example.com", project.board.member.entity.Role.ADMIN);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPostForEdit(10L, 2L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());
        assertThatThrownBy(() -> postService.getPostForEdit(10L, admin.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());
        verify(postRepository, org.mockito.Mockito.times(2)).findById(10L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 수정 화면 조회는 게시글 없음 오류를 반환한다")
    void getPostForEditNotFound() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostForEdit(99L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
        verify(postRepository).findById(99L);
    }

    @Test
    @DisplayName("키워드가 없으면 전체 게시글을 페이지 결과로 변환한다")
    void findAllSuccess() {
        Member writer = member(1L);
        Post post = post(10L, "list title", "content", writer);
        PageRequestDto request = PageRequestDto.builder().page(1).size(5).build();
        given(postRepository.findPosts(isNull(), any()))
                .willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 5), 1));

        PageResultDto<PostListResponse, Post> result = postService.getPosts(request);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getDtoList()).extracting(PostListResponse::getTitle)
                .containsExactly("list title");
        verify(postRepository).findPosts(isNull(), any());
    }

    @Test
    @DisplayName("키워드가 있으면 같은 페이지 계약으로 제목을 검색한다")
    void findByKeywordSuccess() {
        Member writer = member(1L);
        Post post = post(10L, "Spring board", "content", writer);
        PageRequestDto request = PageRequestDto.builder()
                .page(1)
                .size(5)
                .keyword("Spring")
                .build();
        given(postRepository.findPosts(eq("Spring"), any()))
                .willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 5), 1));

        PageResultDto<PostListResponse, Post> result = postService.getPosts(request);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getDtoList()).extracting(PostListResponse::getTitle)
                .containsExactly("Spring board");
        verify(postRepository).findPosts(eq("Spring"), any());
    }

    @Test
    @DisplayName("요청자가 작성자인 경우에만 게시글을 수정한다")
    void updateOwnerOnly() {
        Member writer = member(1L);
        Post post = post(10L, "old title", "old content", writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.update(new PostRequest("new title", "new content"), 10L, 1L);

        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");

        assertThatThrownBy(() -> postService.update(new PostRequest("other title", "content"), 10L, 2L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());
        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
    }

    @Test
    @DisplayName("댓글 삭제 후 작성자만 게시글을 삭제할 수 있다")
    void deleteOwner() {
        Member writer = member(1L);
        Post post = post(10L, "delete title", "content", writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.delete(10L, 1L);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(commentRepository, postRepository);
        order.verify(commentRepository).deleteRepliesByPostId(10L);
        order.verify(commentRepository).deleteRootCommentsByPostId(10L);
        order.verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("작성자가 아닌 ADMIN도 일반 게시글을 수정하거나 삭제할 수 없다")
    void adminCannotBypassOwnership() {
        Member writer = member(1L);
        Member admin = member(2L, "admin", "admin@example.com", project.board.member.entity.Role.ADMIN);
        Post post = post(10L, "title", "content", writer);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(new PostRequest("changed", "changed"), 10L, admin.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());
        assertThatThrownBy(() -> postService.delete(10L, admin.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());

        assertThat(post.getTitle()).isEqualTo("title");
        assertThat(post.getContent()).isEqualTo("content");
        verify(commentRepository, never()).deleteRepliesByPostId(10L);
        verify(commentRepository, never()).deleteRootCommentsByPostId(10L);
        verify(postRepository, never()).delete(post);
    }

    @Test
    @DisplayName("관리자 삭제는 댓글을 제거하고 식별자로 게시글을 삭제한다")
    void deleteForAdminSuccess() {
        Post post = post(10L, member(1L));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.deleteForAdmin(10L);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(commentRepository, postRepository);
        order.verify(commentRepository).deleteRepliesByPostId(10L);
        order.verify(commentRepository).deleteRootCommentsByPostId(10L);
        order.verify(postRepository).deleteById(10L);
    }

    @Test
    @DisplayName("조회수 증가 시 수정된 행이 없으면 예외를 던진다")
    void viewCount() {
        given(postRepository.incrementViewCount(10L)).willReturn(1);
        given(postRepository.incrementViewCount(99L)).willReturn(0);

        postService.viewCount(10L);

        assertThatThrownBy(() -> postService.viewCount(99L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("검색 결과, 통계, 내 게시글, 최근 게시글을 반환한다")
    void searchAndMyQueries() {
        Member writer = member(1L);
        Post first = post(10L, "Spring", "content", writer);
        Post second = post(11L, "JPA", "content", writer);
        PostRecent recent = PostRecent.builder()
                .id(11L)
                .title("JPA")
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        given(postRepository.findByTitleContaining("S")).willReturn(List.of(first));
        given(postRepository.countMyPosts(1L)).willReturn(2L);
        given(postRepository.sumViewCountByMemberId(1L)).willReturn(7L);
        given(postRepository.countTodayPosts(any(), any())).willReturn(3L);
        given(postRepository.countByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any(), any()))
                .willReturn(1L);
        given(postRepository.findMyPosts(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 5), 2));
        given(postRepository.findMyRecentPosts(any(), any())).willReturn(List.of(recent));

        assertThat(postService.search("S")).extracting(PostListResponse::getTitle).containsExactly("Spring");
        assertThat(postService.countTodayPosts()).isEqualTo(3L);
        assertThat(postService.countMyPosts(1L)).isEqualTo(2L);
        assertThat(postService.countMyPostViews(1L)).isEqualTo(7L);
        assertThat(postService.myTodayPostsCount(1L)).isEqualTo(1L);
        assertThat(postService.getMyPosts(1L, PageRequestDto.builder().page(1).size(5).keyword("post").build())
                .getDtoList()).extracting(PostListResponse::getTitle)
                .containsExactly("Spring", "JPA");
        assertThat(postService.getRecentPosts(1L)).extracting(PostRecent::getTitle).containsExactly("JPA");
        assertThatThrownBy(() -> postService.countMyPosts(null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        assertThatThrownBy(() -> postService.countMyPostViews(null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}
