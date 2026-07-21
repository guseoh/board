package project.board.post.controller.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import project.board.comment.repository.CommentRepository;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.global.pagination.PageRequestDto;
import project.board.global.pagination.PageResultDto;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.response.PostDetailApiResponse;
import project.board.post.dto.response.PostListResponse;
import project.board.post.dto.response.PostPageApiResponse;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;
import project.board.post.service.PostService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("게시글 목록을 페이지 메타데이터와 함께 반환한다")
    void getPostsReturnsPageResponse() throws Exception {
        PostListResponse firstPost = PostListResponse.builder()
                .id(10L)
                .title("첫 번째 게시글")
                .viewCount(15)
                .memberNickname("user")
                .createdAt(LocalDateTime.of(2026, 7, 21, 10, 0))
                .build();

        Page<PostListResponse> page = new PageImpl<>(
                List.of(firstPost),
                PageRequest.of(0, 2, Sort.by("id").descending()),
                3
        );

        PageResultDto<PostListResponse, Post> pageResult =
                new PageResultDto<>(page);

        given(postService.getPosts(any(PageRequestDto.class)))
                .willReturn(pageResult);

        mockMvc.perform(get("/api/posts")
                        .param("page", "1")
                        .param("size", "2")
                        .param("keyword", "첫 번째"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].title").value("첫 번째 게시글"))
                .andExpect(jsonPath("$.content[0].memberNickname").value("user"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        ArgumentCaptor<PageRequestDto> captor =
                ArgumentCaptor.forClass(PageRequestDto.class);

        then(postService).should().getPosts(captor.capture());

        PageRequestDto captured = captor.getValue();
        assertThat(captured.getPage()).isEqualTo(1);
        assertThat(captured.getSize()).isEqualTo(2);
        assertThat(captured.getKeyword()).isEqualTo("첫 번째");
    }

    @Test
    @DisplayName("목록 요청값이 없으면 기본 page와 size를 사용한다")
    void getPostsUsesDefaultPagination() throws Exception {
        Page<PostListResponse> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 5, Sort.by("id").descending()),
                0
        );

        PageResultDto<PostListResponse, Post> pageResult =
                new PageResultDto<>(page);

        given(postService.getPosts(any(PageRequestDto.class)))
                .willReturn(pageResult);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<PageRequestDto> captor =
                ArgumentCaptor.forClass(PageRequestDto.class);

        then(postService).should().getPosts(captor.capture());

        PageRequestDto captured = captor.getValue();
        assertThat(captured.getPage()).isEqualTo(1);
        assertThat(captured.getSize()).isEqualTo(5);
        assertThat(captured.getKeyword()).isNull();
    }

    @Test
    @DisplayName("page가 1보다 작으면 400 Validation 오류를 반환한다")
    void invalidPageReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/posts"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"));

        then(postService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("size가 100보다 크면 400 Validation 오류를 반환한다")
    void oversizedPageReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));

        then(postService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글 상세를 작성자 정보와 함께 반환하고 댓글은 포함하지 않는다")
    void getPostReturnsDetailWithoutComments() throws Exception {
        PostDetailApiResponse response = new PostDetailApiResponse(
                10L,
                "상세 제목",
                "상세 내용",
                15,
                new PostDetailApiResponse.WriterResponse(1L, "user"),
                LocalDateTime.of(2026, 7, 21, 10, 0),
                LocalDateTime.of(2026, 7, 21, 11, 0)
        );

        given(postService.getPostApiDetail(10L))
                .willReturn(response);

        mockMvc.perform(get("/api/posts/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("상세 제목"))
                .andExpect(jsonPath("$.content").value("상세 내용"))
                .andExpect(jsonPath("$.viewCount").value(15))
                .andExpect(jsonPath("$.writer.id").value(1))
                .andExpect(jsonPath("$.writer.nickname").value("user"))
                .andExpect(jsonPath("$.comments").doesNotExist());

        then(postService).should().getPostApiDetail(10L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 404 JSON 오류를 반환한다")
    void missingPostReturnsNotFound() throws Exception {
        given(postService.getPostApiDetail(999L))
                .willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 게시글이 존재하지 않습니다."))
                .andExpect(jsonPath("$.path").value("/api/posts/999"));
    }

    @Test
    @DisplayName("게시글 ID 타입이 올바르지 않으면 400 JSON 오류를 반환한다")
    void invalidPostIdTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/posts/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT_TYPE"))
                .andExpect(jsonPath("$.path").value("/api/posts/not-a-number"));

        then(postService).shouldHaveNoInteractions();
    }
}

@ExtendWith(MockitoExtension.class)
class PostServiceApiTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("API 상세 조회는 게시글과 작성자를 응답 DTO로 변환한다")
    void getPostApiDetailMapsPostAndWriter() {
        Post post = mock(Post.class);
        Member member = mock(Member.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 21, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 21, 11, 0);

        given(postRepository.findById(10L))
                .willReturn(Optional.of(post));
        given(post.getId()).willReturn(10L);
        given(post.getTitle()).willReturn("상세 제목");
        given(post.getContent()).willReturn("상세 내용");
        given(post.getViewCount()).willReturn(15);
        given(post.getMember()).willReturn(member);
        given(post.getCreatedAt()).willReturn(createdAt);
        given(post.getUpdatedAt()).willReturn(updatedAt);
        given(member.getId()).willReturn(1L);
        given(member.getNickname()).willReturn("user");

        PostDetailApiResponse response =
                postService.getPostApiDetail(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("상세 제목");
        assertThat(response.content()).isEqualTo("상세 내용");
        assertThat(response.viewCount()).isEqualTo(15);
        assertThat(response.writer().id()).isEqualTo(1L);
        assertThat(response.writer().nickname()).isEqualTo("user");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);

        then(postRepository).should().findById(10L);
        then(memberRepository).shouldHaveNoInteractions();
        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("API 상세 조회 대상이 없으면 POST_NOT_FOUND 예외를 발생시킨다")
    void getPostApiDetailThrowsWhenPostDoesNotExist() {
        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        CustomException exception = catchThrowableOfType(
                () -> postService.getPostApiDetail(999L),
                CustomException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        then(memberRepository).shouldHaveNoInteractions();
        then(commentRepository).shouldHaveNoInteractions();
    }
}

class PostPageApiResponseTest {

    @Test
    @DisplayName("빈 페이지는 첫 페이지이면서 마지막 페이지로 변환한다")
    void emptyPageIsFirstAndLast() {
        Page<PostListResponse> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 5),
                0
        );

        PageResultDto<PostListResponse, Post> pageResult =
                new PageResultDto<>(page);

        PostPageApiResponse response =
                PostPageApiResponse.from(pageResult);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalPages()).isZero();
        assertThat(response.totalElements()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지를 올바르게 판정한다")
    void lastPageIsDetected() {
        PostListResponse post = PostListResponse.builder()
                .id(3L)
                .title("마지막 게시글")
                .viewCount(0)
                .memberNickname("user")
                .createdAt(LocalDateTime.of(2026, 7, 21, 10, 0))
                .build();

        Page<PostListResponse> page = new PageImpl<>(
                List.of(post),
                PageRequest.of(1, 2),
                3
        );

        PageResultDto<PostListResponse, Post> pageResult =
                new PageResultDto<>(page);

        PostPageApiResponse response =
                PostPageApiResponse.from(pageResult);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
    }

    @Test
    @DisplayName("응답 content는 전달받은 목록을 방어적으로 복사한다")
    void contentIsDefensivelyCopied() {
        List<PostListResponse> source = new ArrayList<>();

        PostPageApiResponse response = new PostPageApiResponse(
                source,
                1,
                5,
                0,
                0,
                true,
                true
        );

        source.add(PostListResponse.builder()
                .id(1L)
                .title("추가 게시글")
                .viewCount(0)
                .memberNickname("user")
                .createdAt(LocalDateTime.of(2026, 7, 21, 10, 0))
                .build());

        assertThat(response.content()).isEmpty();
        assertThatThrownBy(() -> response.content().add(
                PostListResponse.builder()
                        .id(2L)
                        .title("변경 시도")
                        .viewCount(0)
                        .memberNickname("user")
                        .createdAt(LocalDateTime.of(2026, 7, 21, 10, 0))
                        .build()
        )).isInstanceOf(UnsupportedOperationException.class);
    }
}
