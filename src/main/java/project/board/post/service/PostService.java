package project.board.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.board.comment.dto.CommentDto;
import project.board.comment.repository.CommentRepository;
import project.board.global.dto.PageRequestDto;
import project.board.global.dto.PageResultDto;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.request.PostRequest;
import project.board.post.dto.response.PostDetailsResponse;
import project.board.post.dto.response.PostListResponse;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static project.board.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;    // nullable = false
    private final CommentRepository commentRepository;

    @Transactional
    public PostListResponse save(PostRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new CustomException(LOGIN_REQUIRED));

        Post saved = postRepository.save(Post.create(request.getTitle(), request.getContent(), member));

        return PostListResponse.from(saved);
    }

    public PostDetailsResponse findOne(Long id){
        Post post = getPost(id);

        List<CommentDto.Response> comments = post.getComments().stream()
                .map(CommentDto.Response::from)
                .toList();

        return PostDetailsResponse.from(post, comments);
    }

    public PageResultDto<PostListResponse, Post> findAll(PageRequestDto pageRequestDto) {
        Pageable pageable = pageRequestDto.getPageable(Sort.by("id").descending());

        Page<Post> result = postRepository.findAllWithMember(pageable);

        Function<Post, PostListResponse> fn = PostListResponse::from;

        return new PageResultDto<>(result, fn);
    }

    public List<Post> findAllAdmin() {
        return postRepository.findAllWithMemberForAdmin();
    }

    @Transactional
    public void update(PostRequest request, Long postId, Long memberId) {

        Post post = getPost(postId);
        validateWriter(post, memberId);

        post.change(
                request.getTitle(),
                request.getContent()
        );
    }

    @Transactional
    public void delete(Long id, Long memberId)  {
        Post find = getPost(id);

        validateWriter(find, memberId);

        commentRepository.deleteById(id);

        postRepository.delete(find);
    }

    @Transactional
    public void deleteForAdmin(Long postId) {
        Post find = getPost(postId);

        commentRepository.deleteById(postId);

        postRepository.deleteById(postId);

    }
    @Transactional
    public void viewCount(Long id) {
        int updated = postRepository.incrementViewCount(id);
        if (updated == 0) throw new CustomException(POST_NOT_FOUND);
    }

    public List<PostListResponse> search(String keyword) {
        return postRepository.findByTitleContaining(keyword)
                .stream()
                .map(PostListResponse::from)
                .toList();
    }

    public Long todayWrite() {

        LocalDate today = LocalDate.now();
        LocalDateTime startDay = today.atStartOfDay();
        LocalDateTime nextDay = today.plusDays(1).atStartOfDay();

        return postRepository.countTodayPosts(startDay, nextDay);
    }

    private Post getPost(Long id) {
        return postRepository.findById(id).orElseThrow(() ->
                new CustomException(POST_NOT_FOUND));
    }

    private void validateWriter(Post post, Long memberId) {
        Long writerId = post.getMember().getId();
        if (writerId == null || memberId == null || !writerId.equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_POST_OWNER);
        }
    }

    public long count() {
        return postRepository.count();
    }
}
