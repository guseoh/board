package project.board.comment.controller.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import project.board.comment.dto.response.CommentListApiResponse;
import project.board.comment.entity.Comment;
import project.board.comment.repository.CommentRepository;
import project.board.comment.service.CommentService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static project.board.testsupport.TestFixtures.comment;
import static project.board.testsupport.TestFixtures.member;
import static project.board.testsupport.TestFixtures.post;
import static project.board.testsupport.TestFixtures.reply;

@SpringBootTest(properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentApiReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Test
    @DisplayName("게시글 댓글을 답글 계층과 작성자 정보로 반환한다")
    void getCommentsReturnsHierarchy() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 22, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 22, 11, 0);

        CommentListApiResponse response = new CommentListApiResponse(
                List.of(new CommentListApiResponse.CommentResponse(
                        100L,
                        "댓글",
                        new CommentListApiResponse.WriterResponse(1L, "user1"),
                        createdAt,
                        updatedAt,
                        List.of(new CommentListApiResponse.ReplyResponse(
                                101L,
                                "답글",
                                new CommentListApiResponse.WriterResponse(2L, "user2"),
                                createdAt.plusMinutes(10),
                                updatedAt.plusMinutes(10)
                        ))
                ))
        );

        given(commentService.getCommentsApi(10L)).willReturn(response);

        mockMvc.perform(get("/api/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.comments[0].id").value(100))
                .andExpect(jsonPath("$.comments[0].content").value("댓글"))
                .andExpect(jsonPath("$.comments[0].writer.id").value(1))
                .andExpect(jsonPath("$.comments[0].writer.nickname").value("user1"))
                .andExpect(jsonPath("$.comments[0].replies[0].id").value(101))
                .andExpect(jsonPath("$.comments[0].replies[0].writer.nickname").value("user2"));

        then(commentService).should().getCommentsApi(10L);
    }

    @Test
    @DisplayName("댓글이 없는 게시글은 빈 배열을 반환한다")
    void getCommentsReturnsEmptyList() throws Exception {
        given(commentService.getCommentsApi(10L))
                .willReturn(new CommentListApiResponse(List.of()));

        mockMvc.perform(get("/api/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 요청은 404 JSON을 반환한다")
    void missingPostReturnsNotFound() throws Exception {
        given(commentService.getCommentsApi(999L))
                .willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/posts/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/posts/999/comments"));
    }

    @Test
    @DisplayName("게시글 ID 타입이 올바르지 않으면 400 JSON을 반환한다")
    void invalidPostIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/posts/not-a-number/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT_TYPE"))
                .andExpect(jsonPath("$.path").value("/api/posts/not-a-number/comments"));

        then(commentService).shouldHaveNoInteractions();
    }
}

@ExtendWith(MockitoExtension.class)
class CommentServiceReadApiTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글과 답글을 한 단계 계층형 API 응답으로 변환한다")
    void getCommentsBuildsHierarchy() {
        Member rootWriter = member(1L);
        Member replyWriter = member(2L);
        Post post = post(10L, member(3L));
        Comment root = comment(100L, "댓글", rootWriter, post);
        reply(101L, "답글", replyWriter, post, root);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        CommentListApiResponse response = commentService.getCommentsApi(10L);

        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).id()).isEqualTo(100L);
        assertThat(response.comments().get(0).writer().id()).isEqualTo(1L);
        assertThat(response.comments().get(0).replies())
                .extracting(CommentListApiResponse.ReplyResponse::id)
                .containsExactly(101L);
        assertThat(response.comments().get(0).replies().get(0).writer().id())
                .isEqualTo(2L);

        then(postRepository).should().findById(10L);
        then(commentRepository).shouldHaveNoInteractions();
        then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("댓글이 없는 게시글은 빈 API 목록으로 변환한다")
    void getCommentsReturnsEmptyList() {
        Post post = post(10L, member(1L));
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        CommentListApiResponse response = commentService.getCommentsApi(10L);

        assertThat(response.comments()).isEmpty();
    }

    @Test
    @DisplayName("게시글이 없으면 댓글 변환을 시작하지 않는다")
    void getCommentsRejectsMissingPost() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentsApi(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());

        then(commentRepository).shouldHaveNoInteractions();
        then(memberRepository).shouldHaveNoInteractions();
    }
}
