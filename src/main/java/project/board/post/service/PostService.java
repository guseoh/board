package project.board.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.board.global.exception.CustomException;
import project.board.global.exception.ErrorCode;
import project.board.member.entity.Member;
import project.board.member.repository.MemberRepository;
import project.board.post.dto.PostDto;
import project.board.post.entity.Post;
import project.board.post.repository.PostRepository;

import static project.board.global.exception.ErrorCode.POST_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;    // nullable = false

    @Transactional
    public PostDto.Response save(PostDto.CreateRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() ->
                new CustomException(POST_NOT_FOUND));

        Post saved = postRepository.save(Post.create(request.getTitle(), request.getContent(), member));

        return PostDto.Response.from(saved);
    }

    public PostDto.Response findOne(Long id){
        Post post = getPost(id);
        return PostDto.Response.from(post);
    }

    public Page<PostDto.Response> findAll(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(PostDto.Response::from);
    }

    @Transactional
    public void update(PostDto.UpdateRequest request, Long postId, Long memberId) {

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
        postRepository.delete(find);
    }

    @Transactional
    public void viewCount(Long id) {
        getPost(id).increaseViewCount();
    }

    private Post getPost(Long id) {
        Post find = postRepository.findById(id).orElseThrow(() ->
                new CustomException(POST_NOT_FOUND));
        return find;
    }

    private void validateWriter(Post post, Long memberId) {
        Long writerId = post.getMember().getId();
        if (writerId == null || memberId == null || !writerId.equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_POST_OWNER);
        }
    }

}
