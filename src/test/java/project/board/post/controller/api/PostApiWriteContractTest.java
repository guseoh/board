package project.board.post.controller.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.global.security.principal.UnifiedPrincipal;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.request.PostCreateApiRequest;
import project.board.post.dto.request.PostUpdateApiRequest;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;
import project.board.post.service.PostService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static project.board.testsupport.TestFixtures.member;
import static project.board.testsupport.TestFixtures.post;
import static project.board.testsupport.TestFixtures.principal;
import static project.board.testsupport.TestFixtures.setId;

@SpringBootTest(properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostApiWriteControllerTest {

    private static final String CREATE_REQUEST = """
            {
              "title": "새 게시글",
              "content": "새 게시글 내용"
            }
            """;

    private static final String UPDATE_REQUEST = """
            {
              "title": "수정 제목",
              "content": "수정 내용"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("익명 사용자의 게시글 작성 수정 삭제 요청은 401 JSON을 반환한다")
    void anonymousWriteRequestsReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/posts"));

        mockMvc.perform(put("/api/posts/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/posts/10"));

        mockMvc.perform(delete("/api/posts/10")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/posts/10"));

        then(postService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인증 사용자의 게시글 쓰기 요청에도 CSRF 토큰이 필요하다")
    void writeRequestsRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .with(authentication(authToken(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISSING"))
                .andExpect(jsonPath("$.path").value("/api/posts"));

        mockMvc.perform(put("/api/posts/10")
                        .with(authentication(authToken(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISSING"));

        mockMvc.perform(delete("/api/posts/10")
                        .with(authentication(authToken(1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISSING"));

        then(postService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글 작성 성공 시 201과 생성 위치 및 게시글 ID를 반환한다")
    void createPostReturnsCreatedResponse() throws Exception {
        given(postService.createPost(any(PostCreateApiRequest.class), eq(1L)))
                .willReturn(101L);

        mockMvc.perform(post("/api/posts")
                        .with(authentication(authToken(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", "http://localhost/api/posts/101"))
                .andExpect(jsonPath("$.postId").value(101));

        ArgumentCaptor<PostCreateApiRequest> captor =
                ArgumentCaptor.forClass(PostCreateApiRequest.class);

        then(postService).should().createPost(captor.capture(), eq(1L));
        assertThat(captor.getValue().title()).isEqualTo("새 게시글");
        assertThat(captor.getValue().content()).isEqualTo("새 게시글 내용");
    }

    @Test
    @DisplayName("게시글 작성 요청의 제목이 비어 있으면 400 Validation 오류를 반환한다")
    void createPostRejectsBlankTitle() throws Exception {
        String request = """
                {
                  "title": " ",
                  "content": "내용"
                }
                """;

        mockMvc.perform(post("/api/posts")
                        .with(authentication(authToken(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/posts"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));

        then(postService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글 작성 요청의 내용이 500자를 넘으면 400 Validation 오류를 반환한다")
    void createPostRejectsOverlongContent() throws Exception {
        String request = """
                {
                  "title": "제목",
                  "content": "%s"
                }
                """.formatted("a".repeat(501));

        mockMvc.perform(post("/api/posts")
                        .with(authentication(authToken(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));

        then(postService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글 수정 성공 시 204를 반환하고 인증 회원 ID를 Service에 전달한다")
    void updatePostReturnsNoContent() throws Exception {
        mockMvc.perform(put("/api/posts/10")
                        .with(authentication(authToken(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ArgumentCaptor<PostUpdateApiRequest> captor =
                ArgumentCaptor.forClass(PostUpdateApiRequest.class);

        then(postService).should().update(captor.capture(), eq(10L), eq(1L));
        assertThat(captor.getValue().title()).isEqualTo("수정 제목");
        assertThat(captor.getValue().content()).isEqualTo("수정 내용");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자의 게시글 수정은 403 JSON 오류를 반환한다")
    void updatePostRejectsNonOwner() throws Exception {
        willThrow(new CustomException(ErrorCode.NOT_POST_OWNER))
                .given(postService)
                .update(any(PostUpdateApiRequest.class), eq(10L), eq(2L));

        mockMvc.perform(put("/api/posts/10")
                        .with(authentication(authToken(2L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_POST_OWNER"))
                .andExpect(jsonPath("$.path").value("/api/posts/10"));
    }

    @Test
    @DisplayName("게시글 삭제 성공 시 204를 반환한다")
    void deletePostReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/posts/10")
                        .with(authentication(authToken(1L)))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        then(postService).should().delete(10L, 1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제는 404 JSON 오류를 반환한다")
    void deletePostReturnsNotFound() throws Exception {
        willThrow(new CustomException(ErrorCode.POST_NOT_FOUND))
                .given(postService)
                .delete(999L, 1L);

        mockMvc.perform(delete("/api/posts/999")
                        .with(authentication(authToken(1L)))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/posts/999"));
    }

    private UsernamePasswordAuthenticationToken authToken(Long memberId) {
        UnifiedPrincipal principal = principal(memberId, Role.USER);
        return new UsernamePasswordAuthenticationToken(
                principal,
                "credentials",
                principal.getAuthorities()
        );
    }
}

@ExtendWith(MockitoExtension.class)
class PostServiceWriteApiTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("API 게시글 작성은 인증 회원을 작성자로 지정하고 생성된 ID를 반환한다")
    void createPostAssignsAuthenticatedWriter() {
        Member writer = member(1L);
        PostCreateApiRequest request =
                new PostCreateApiRequest("새 제목", "새 내용");

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(writer));
        given(postRepository.save(any(Post.class)))
                .willAnswer(invocation -> {
                    Post saved = invocation.getArgument(0);
                    setId(saved, 10L);
                    return saved;
                });

        Long postId = postService.createPost(request, 1L);

        assertThat(postId).isEqualTo(10L);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        then(postRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("새 제목");
        assertThat(captor.getValue().getContent()).isEqualTo("새 내용");
        assertThat(captor.getValue().getMember()).isSameAs(writer);
        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원 ID가 없거나 존재하지 않으면 게시글을 저장하지 않는다")
    void createPostRequiresAuthenticatedMember() {
        PostCreateApiRequest request =
                new PostCreateApiRequest("제목", "내용");

        assertThatThrownBy(() -> postService.createPost(request, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.LOGIN_REQUIRED.getMessage());

        given(memberRepository.findById(99L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(request, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.LOGIN_REQUIRED.getMessage());

        then(postRepository).shouldHaveNoInteractions();
        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("API 게시글 수정은 작성자만 제목과 내용을 변경한다")
    void updatePostChangesOwnerPost() {
        Member writer = member(1L);
        Post post = post(10L, "기존 제목", "기존 내용", writer);
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        postService.update(
                new PostUpdateApiRequest("수정 제목", "수정 내용"),
                10L,
                1L
        );

        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getContent()).isEqualTo("수정 내용");
        then(postRepository).should().findById(10L);
        then(postRepository).should(never()).save(any(Post.class));
        then(memberRepository).shouldHaveNoInteractions();
        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 API로 게시글을 수정할 수 없다")
    void updatePostRejectsNonOwner() {
        Post post = post(10L, "기존 제목", "기존 내용", member(1L));
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(
                new PostUpdateApiRequest("변경 제목", "변경 내용"),
                10L,
                2L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());

        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getContent()).isEqualTo("기존 내용");
        then(postRepository).should(never()).save(any(Post.class));
        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Controller 검증을 우회한 501자 API 요청도 도메인에서 차단한다")
    void writeApiKeepsDomainValidation() {
        Member writer = member(1L);
        Post post = post(10L, "기존 제목", "기존 내용", writer);
        String tooLong = "a".repeat(501);

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(writer));
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.createPost(
                new PostCreateApiRequest(tooLong, "내용"),
                1L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_TITLE_TOO_LONG.getMessage());

        assertThatThrownBy(() -> postService.update(
                new PostUpdateApiRequest("제목", tooLong),
                10L,
                1L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_CONTENT_TOO_LONG.getMessage());

        assertThat(post.getContent()).isEqualTo("기존 내용");
        then(postRepository).should(never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 삭제는 답글과 최상위 댓글을 제거한 뒤 게시글을 삭제한다")
    void deletePostRemovesCommentsInOrder() {
        Post post = post(10L, "삭제 제목", "삭제 내용", member(1L));
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        postService.delete(10L, 1L);

        InOrder order = inOrder(commentRepository, postRepository);
        order.verify(commentRepository).deleteRepliesByPostId(10L);
        order.verify(commentRepository).deleteRootCommentsByPostId(10L);
        order.verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 댓글이나 게시글을 삭제할 수 없다")
    void deletePostRejectsNonOwnerBeforeDeletingAnything() {
        Post post = post(10L, "삭제 제목", "삭제 내용", member(1L));
        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(10L, 2L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NOT_POST_OWNER.getMessage());

        then(commentRepository).shouldHaveNoInteractions();
        then(postRepository).should(never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 수정하거나 삭제할 수 없다")
    void missingPostCannotBeUpdatedOrDeleted() {
        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(
                new PostUpdateApiRequest("제목", "내용"),
                999L,
                1L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());

        assertThatThrownBy(() -> postService.delete(999L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());

        then(commentRepository).shouldHaveNoInteractions();
        then(postRepository).should(never()).delete(any(Post.class));
    }
}
