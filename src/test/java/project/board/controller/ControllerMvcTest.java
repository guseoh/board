package project.board.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import project.board.comment.dto.MyCommentPageResponse;
import project.board.comment.dto.MyCommentResponse;
import project.board.comment.repository.CommentRepository;
import project.board.comment.service.CommentService;
import project.board.global.pagination.PageRequestDto;
import project.board.global.pagination.PageResultDto;
import project.board.global.notification.discord.DiscordNotifier;
import project.board.global.security.principal.UnifiedPrincipal;
import project.board.member.dto.response.MemberUpdateResponse;
import project.board.member.entity.Member;
import project.board.member.entity.Role;
import project.board.member.service.MemberService;
import project.board.post.dto.request.PostRecent;
import project.board.post.dto.response.PostDetailsResponse;
import project.board.post.dto.response.PostListResponse;
import project.board.post.entity.Post;
import project.board.post.service.PostService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static project.board.testsupport.TestFixtures.member;
import static project.board.testsupport.TestFixtures.post;
import static project.board.testsupport.TestFixtures.principal;

@WebMvcTest(properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class ControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CommentRepository commentRepository;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회원 컨트롤러는 회원가입 화면 조회, 가입 처리, 검증 실패 처리를 수행한다")
    void memberController() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/signup"))
                .andExpect(model().attributeExists("form"));

        mockMvc.perform(post("/signup")
                        .param("nickname", "tester")
                        .param("email", "tester@example.com")
                        .param("password", "password1")
                        .param("passwordConfirm", "password1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loginForm"))
                .andExpect(flash().attributeExists("msg"));
        verify(memberService).signUp(any());

        mockMvc.perform(post("/signup")
                        .param("nickname", "")
                        .param("email", "invalid-email")
                        .param("password", "")
                        .param("passwordConfirm", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("member/signup"))
                .andExpect(model().attributeHasErrors("form"));

        mockMvc.perform(get("/loginForm"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/loginForm"));
    }

    @Test
    @DisplayName("게시글 컨트롤러는 목록, 상세, 작성, 수정, 삭제, 검색 요청을 처리한다")
    void postController() throws Exception {
        UnifiedPrincipal user = principal(1L, Role.USER);
        Member writer = member(1L);
        Post post = post(10L, "title", "content", writer);
        PageResultDto<PostListResponse, Post> page = new PageResultDto<>(
                new PageImpl<>(List.of(post), PageRequest.of(0, 5), 1),
                PostListResponse::from
        );
        PostDetailsResponse detail = PostDetailsResponse.from(post, List.of());

        given(postService.findAll(any(PageRequestDto.class))).willReturn(page);
        given(postService.todayWrite()).willReturn(1L);
        given(memberService.countMember()).willReturn(2L);
        given(postService.myPostCount(1L)).willReturn(3L);
        given(commentService.myCommentCount(1L)).willReturn(4L);
        given(postService.findOne(10L)).willReturn(detail);
        given(postService.save(any(), eq(1L))).willReturn(PostListResponse.from(post));
        given(postService.search("title")).willReturn(List.of(PostListResponse.from(post)));

        mockMvc.perform(get("/").with(authenticated(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("post/list"))
                .andExpect(model().attributeExists("page", "posts", "myPostCount", "myCommentCount"));

        mockMvc.perform(get("/post/10").with(authenticated(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("post/detail"))
                .andExpect(model().attributeExists("post", "comments", "commentForm", "memberId"));
        verify(postService).viewCount(10L);

        mockMvc.perform(get("/posts/search").param("keyword", "title"))
                .andExpect(status().isOk())
                .andExpect(view().name("post/list"))
                .andExpect(model().attributeExists("posts", "keyword"));

        mockMvc.perform(get("/post/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("post/form"))
                .andExpect(model().attribute("mode", "create"));

        mockMvc.perform(post("/post/new")
                        .with(authenticated(user))
                        .param("title", "title")
                        .param("content", "content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/10"));
        verify(postService).save(any(), eq(1L));

        mockMvc.perform(post("/post/new")
                        .with(authenticated(user))
                        .param("title", "")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("post/form"))
                .andExpect(model().attributeHasErrors("form"));

        mockMvc.perform(get("/post/10/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("post/form"))
                .andExpect(model().attribute("mode", "edit"));

        mockMvc.perform(post("/post/10/edit")
                        .with(authenticated(user))
                        .param("title", "updated")
                        .param("content", "updated content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/10"));
        verify(postService).update(any(), eq(10L), eq(1L));

        mockMvc.perform(post("/post/10/delete").with(authenticated(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        verify(postService).delete(10L, 1L);
    }

    @Test
    @DisplayName("댓글 컨트롤러는 작성, 답글, 수정, 삭제를 처리하고 익명 사용자를 리다이렉트한다")
    void commentController() throws Exception {
        UnifiedPrincipal user = principal(1L, Role.USER);

        mockMvc.perform(post("/post/10/comment")
                        .with(authenticated(user))
                        .param("content", "comment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/10"));
        verify(commentService).create(any(), eq(1L), eq(10L));

        mockMvc.perform(post("/post/10/comment")
                        .with(anonymousPrincipal())
                        .param("content", "comment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/loginForm?redirect=*"));

        mockMvc.perform(post("/post/10/comment/100/replies")
                        .with(authenticated(user))
                        .param("content", "reply"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/10"));
        verify(commentService).createReply(any(), eq(1L), eq(10L), eq(100L));

        mockMvc.perform(post("/post/10/comment/100/edit")
                        .with(authenticated(user))
                        .param("content", "changed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/10"));
        verify(commentService).update(eq(100L), eq(1L), eq(10L), any());

        mockMvc.perform(post("/post/10/comment/100/delete").with(authenticated(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/10"));
        verify(commentService).delete(1L, 100L, 10L);

        mockMvc.perform(post("/post/10/comment")
                        .with(authenticated(user))
                        .param("content", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/post/10"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("마이페이지 컨트롤러는 대시보드, 게시글, 댓글, 정보 수정, 비밀번호, 탈퇴 흐름을 반환한다")
    void myController() throws Exception {
        UnifiedPrincipal user = principal(1L, Role.USER);
        Member writer = member(1L);
        Post post = post(10L, "title", "content", writer);
        PageResultDto<PostListResponse, Post> page = new PageResultDto<>(
                new PageImpl<>(List.of(post), PageRequest.of(0, 5), 1),
                PostListResponse::from
        );
        MyCommentPageResponse commentPage = MyCommentPageResponse.builder()
                .myCommentCount(1)
                .todayMyCommentCount(1)
                .recentCommentCount(1)
                .comments(new PageResultDto<>(
                        new PageImpl<MyCommentResponse>(List.of(), PageRequest.of(0, 5), 0)
                ))
                .build();
        given(postService.myPostCount(1L)).willReturn(1L);
        given(commentService.myCommentCount(1L)).willReturn(2L);
        given(postService.recentPosts(1L)).willReturn(List.of(new PostRecent(10L, "title", 0, LocalDateTime.now())));
        given(commentService.recentComments(1L)).willReturn(List.of());
        given(postService.findAll(any(PageRequestDto.class))).willReturn(page);
        given(postService.todayWrite()).willReturn(3L);
        given(postService.myTodayPostsCount(1L)).willReturn(4L);
        given(postService.myPosts(1L)).willReturn(List.of(PostListResponse.from(post)));
        given(commentService.myCommentPage(eq(1L), any(PageRequestDto.class), any())).willReturn(commentPage);
        given(memberService.getMyProfile(1L)).willReturn(MemberUpdateResponse.builder()
                .nickname("user1")
                .email("user1@example.com")
                .passwordChangeable(true)
                .build());

        mockMvc.perform(get("/my").with(authenticated(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("my/my"))
                .andExpect(model().attributeExists("myPostCount", "myCommentCount", "recentPosts", "recentComments"));

        mockMvc.perform(get("/my/posts").with(authenticated(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("my/myPost"))
                .andExpect(model().attributeExists("posts", "page"));

        mockMvc.perform(get("/my/comments").with(authenticated(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("my/myComment"))
                .andExpect(model().attributeExists("pageResponse"));

        mockMvc.perform(get("/my/edit").with(authenticated(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("my/myEdit"))
                .andExpect(model().attributeExists("form", "nicknameRequest", "passwordRequest"));

        mockMvc.perform(post("/my/edit/password")
                        .with(authenticated(user))
                        .param("currentPassword", "password1")
                        .param("newPassword", "password2")
                        .param("newPasswordConfirm", "password2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        verify(memberService).updatePassword(eq(1L), any());

        mockMvc.perform(get("/my/withdraw").with(authenticated(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("my/withdraw"));

        mockMvc.perform(post("/my/withdraw").with(authenticated(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        verify(memberService).withdraw(1L);
    }

    @Test
    @DisplayName("관리자 컨트롤러는 대시보드, 관리 화면, 명령 리다이렉트를 반환한다")
    void adminController() throws Exception {
        Member admin = member(1L, "admin", "admin@example.com", Role.ADMIN);
        Post post = post(10L, "title", "content", admin);
        given(postService.count()).willReturn(10L);
        given(memberService.countMember()).willReturn(2L);
        given(postService.findAllAdmin()).willReturn(List.of(post));
        given(memberService.findAllForAdmin()).willReturn(List.of(admin));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andExpect(model().attributeExists("totalPosts", "totalUsers"));

        mockMvc.perform(get("/admin/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/posts"))
                .andExpect(model().attributeExists("posts"));

        mockMvc.perform(post("/admin/posts/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
        verify(postService).deleteForAdmin(10L);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("members"));

        mockMvc.perform(post("/admin/users/1/role").param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
        verify(memberService).roleChange("ADMIN", 1L);

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
        verify(memberService).deleteForAdmin(1L);
    }

    @Test
    @DisplayName("익명 댓글 요청은 서비스를 호출하지 않는다")
    void anonymousCommentDoesNotCallService() throws Exception {
        mockMvc.perform(post("/post/10/comment").with(anonymousPrincipal()).param("content", "comment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/loginForm?redirect=*"));

        verify(commentService, never()).create(any(), any(), any());
    }

    private UsernamePasswordAuthenticationToken authToken(UnifiedPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
    }

    private RequestPostProcessor authenticated(UnifiedPrincipal principal) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(authToken(principal));
            return request;
        };
    }

    private RequestPostProcessor anonymousPrincipal() {
        return request -> {
            SecurityContextHolder.clearContext();
            return request;
        };
    }
}
